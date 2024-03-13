package nora.typesys

import nora.Gen
import nora.Ref
import nora.resolvedConstraints

sealed interface TypeConstraint {val typ:Type}
private data class SubTypeOf(override val typ:Type) : TypeConstraint
private data class SuperTypeOf(override val typ:Type) : TypeConstraint
private data class TypeOf(override val typ:Type) : TypeConstraint

private fun Map<Gen, Set<TypeConstraint>>.add(name:Gen, typ:TypeConstraint):Map<Gen, Set<TypeConstraint>> {
    if(!this.containsKey(name)) return this;
    return this + Pair(name, this[name]!! + typ)
}

private fun Map<Gen, Set<TypeConstraint>>.addBound(typ:GenericType):Map<Gen, Set<TypeConstraint>> {
    return typ.bounds.filterIsInstance<DataType>().fold(this){m,b -> m.add(typ.name.data,SubTypeOf(b))}
}

//Note only inferences entries that exist (init with empty sets)
fun Type.match(other: Type, generics:Map<Gen, Set<TypeConstraint>>):Map<Gen, Set<TypeConstraint>> {
    return when(this){
        is DataType -> when(other){
            is DataType -> {
                parameters.zip(other.parameters).fold(generics) { state, (left, right) ->
                    left.match(right, state)
                }
            }

            is GenericType -> resolvedConstraints(this as Type).matchStrict(other.bounds.map{ it.data }, generics).addBound(other)
                .add(other.name.data,SuperTypeOf(this))
            else -> generics
        }
        is GenericType -> when(other){
            is DataType -> bounds.map{ it.data }.matchStrict(resolvedConstraints(other as Type), generics).addBound(this)
                .add(name.data,SubTypeOf(other))
            is GenericType -> bounds.map{ it.data }.matchStrict(other.bounds.map{ it.data }, generics).addBound(this).addBound(other)
                .add(other.name.data,/*Super*/TypeOf(this))
                .add(name.data,/*Sub*/TypeOf(other))
            else -> generics
        }
        else -> generics
    }
}

private fun TypeParam.match(other:TypeParam, generics:Map<Gen, Set<TypeConstraint>>):Map<Gen, Set<TypeConstraint>> {
    assert(variance == other.variance)
    return when(variance){
        Variance.CO -> param.data.match(other.param.data, generics)
        Variance.CONTRA -> other.param.data.match(param.data,generics)
        Variance.IN -> param.data.matchStrict(other.param.data, generics)
    }
}

private fun Constraint.matchStrict(other:Constraint, generics:Map<Gen, Set<TypeConstraint>>):Map<Gen, Set<TypeConstraint>>{
    return when(this){
        is DataType -> when(other){
            is DataType -> parameters.zip(other.parameters).fold(generics) { s,(l,r) -> l.matchStrict(r,s) }
            is TraitConstraint -> return generics; //Should not happen
        }
        is TraitConstraint -> when(other){
            is DataType -> return generics; //Should not happen
            is TraitConstraint -> parameters.zip(other.parameters).fold(generics) { s,(l,r) -> l.matchStrict(r,s) }
        }
    }
}

private fun List<Constraint>.matchStrict(other:List<Constraint>, generics:Map<Gen, Set<TypeConstraint>>):Map<Gen, Set<TypeConstraint>>{
    val selfConstraints = this.fold(mapOf<Ref,Constraint>()){s,e -> s + e.collect()}
    val otherConstraints = other.fold(mapOf<Ref,Constraint>()){s,e -> s + e.collect()}
    val names = selfConstraints.keys.intersect(otherConstraints.keys)
    return names.fold(generics){g,n -> selfConstraints[n]!!.matchStrict(otherConstraints[n]!!,g) }
}

private fun Constraint.collect():Map<Ref,Constraint>{
    return when(this){
        is DataType -> resolvedConstraints(this as Constraint).fold(mapOf(Pair(base.data, this as Constraint))){ s, e -> s + e.collect()}
        is TraitConstraint -> resolvedConstraints(this).fold(mapOf(Pair(base.data, this as Constraint))){s,e -> s + e.collect()}
    }
}


//Can be used for Co & ContraVariance
private fun Type.matchStrict(other: Type, generics:Map<Gen, Set<TypeConstraint>>):Map<Gen, Set<TypeConstraint>> {
    return when(this){
        is DataType -> when(other){
            is DataType -> {
                parameters.zip(other.parameters).fold(generics) { state, (left, right) ->
                    left.matchStrict(right, state)
                }
            }
            is GenericType -> generics.add(other.name.data,TypeOf(this))
            else -> generics
        }
        is GenericType -> when(other){
            is DataType -> generics.add(name.data,TypeOf(other))
            is GenericType -> generics.add(other.name.data,TypeOf(this)).add(name.data,TypeOf(other))
            else -> generics
        }
        else -> generics
    }
}

private fun TypeParam.matchStrict(other:TypeParam, generics:Map<Gen, Set<TypeConstraint>>):Map<Gen, Set<TypeConstraint>> {
    return param.data.matchStrict(other.param.data, generics)
}

//The constraint resolver Algorithm
// Should be filled by match and manual WeakHints
fun solveConstraints(constraints: Map<Gen, Set<TypeConstraint>>, hints:Map<Gen, Type>):Map<Gen, Type> {
    val withoutGenerics = constraints.mapValues { it.value.filter { v -> (v.typ !is GenericType) } }.filterValues { it.isNotEmpty() }
    //Todo: Only works because currently Generics are only referred over TypeOf Constraints
    val genericForwarders = constraints.mapValues { it.value.filter { v -> v.typ is GenericType }.map { v -> (v.typ as GenericType).name.data } }.filterValues { it.isNotEmpty() }
    val subTypeBounds = withoutGenerics.mapValues { it.value.filter { v -> v is SubTypeOf || v is TypeOf}.map { v -> v.typ }.toSet() }.filterValues { it.isNotEmpty() }
    val superTypeBounds = withoutGenerics.mapValues { it.value.filter { v -> v is SuperTypeOf || v is TypeOf}.map { v -> v.typ }.toSet() }.filterValues { it.isNotEmpty() }
    val highestSubBound = subTypeBounds.mapValues { it.value.reduce { l,r -> if(l.subTypeOf(r) == Success) r else {if(r.subTypeOf(l) == Success) l else UnknownType}}}
    val lowestSuperBound = superTypeBounds.mapValues { it.value.reduce { l,r -> if(l.subTypeOf(r) == Success) l else {if(r.subTypeOf(l) == Success) r else UnknownType}}}
    val fixed = constraints.keys.flatMap {
        val lowest = highestSubBound[it]
        val highest = lowestSuperBound[it]
        if(highest == null && lowest != null) listOf(Pair(it,lowest))
        else if(lowest == null && highest != null) listOf(Pair(it,highest))
        else if(lowest != null && highest != null) {
            if(lowest.sameTypeAs(highest) == Success) listOf(Pair(it,lowest))
            //Note: we leve illegal cases in, so the errors get nicer
            else if(!genericForwarders.containsKey(it)) listOf(Pair(it,lowest))//we prefer the more specific one
            else listOf()//we will see if generics will help
        }
        else if(hints.containsKey(it)) listOf(Pair(it,hints[it]!!))
        else listOf()
    }.toMap()

    //Todo: Only works because currently Generics are only referred over TypeOf Constraints
    fun resolve(key:Gen, visited:Set<Gen>):Type? {
        if(visited.contains(key)) return null
        if(fixed.containsKey(key)) return fixed[key]!!
        assert(genericForwarders.containsKey(key))
        return genericForwarders[key]!!.firstNotNullOf { resolve(it, visited + key) }
    }

    return constraints.keys.associateWith { resolve(it, setOf()) ?: UnknownType }
    //Normalize it by substituting
    // We delay in case the caller has further option to resolve missing generics
    //return result.mapValues { it.value.substitute(result) }
}