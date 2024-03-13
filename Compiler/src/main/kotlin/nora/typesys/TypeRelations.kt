package nora.typesys

import nora.Elem
import nora.Ref
import nora.resolvedConstraints
import nora.resolvedParent

sealed interface TypeCheckingResult
data object Success : TypeCheckingResult
data class Failure(val term1: TypeRelated, val failure:String, val term2:TypeRelated) : TypeCheckingResult

//Todo: Has Error Enough Src Info??
//Subtype algorithm
fun Type.satisfies(other: Constraint):TypeCheckingResult {
    return when(other){
        is DataType -> subTypeOf(other)
        is TraitConstraint -> when(this){
            is DataType -> {
                val traits = resolvedConstraints(this as Type);
                if(traits.isEmpty()) Success
                else traits.fold<_,TypeCheckingResult>(Failure(this, "satisfies", other)){ s,e ->
                    if(s == Success) Success
                    else e.satisfies(other)
                }
            }
            is GenericType -> if(bounds.isEmpty()) Success
            else bounds.fold<_,TypeCheckingResult>(Failure(this, "satisfies", other)){ s,e ->
                if(s == Success) Success
                else e.data.satisfies(other)
            }
            //This could be anything so we treat them as such
            //     they should only be around during type inferenz and not type checking
            is TypeVar -> Failure(this, "satisfies", other)
            UnknownType -> Failure(this, "satisfies", other)
        }
    }
}



//Can be used for Co & ContraVariance
fun Type.subTypeOf(other: Type):TypeCheckingResult  {
    if(this === other) return Success
    return when(this){
        is DataType -> when(other){
            is DataType -> {
                if(base.data == other.base.data) {
                    if(parameters.subTypeOf(other.parameters) == Success) return Success
                }
                val parent = resolvedParent(this) ?: return Failure(this, "suptypeOf", other)
                //We have a same argument policy for type hierarchies
                parent.subTypeOf(other)
            }
            is GenericType -> Failure(this, "suptypeOf", other)
            //This could be anything so we treat them as such
            //     they should only be around during type inferenz and not type checking
            is TypeVar -> Failure(this, "suptypeOf", other)
            UnknownType -> Failure(this, "suptypeOf", other)
        }
        is GenericType -> when(other){
            is DataType -> {
                val dataBounds = bounds.filterIsInstance<DataType>()
                if(dataBounds.isEmpty()) Success
                else dataBounds.fold<_,TypeCheckingResult>(Failure(this, "suptypeOf", other)){ s,e ->
                    if(s == Success) Success
                    else e.subTypeOf(other)
                }
            }
            is GenericType -> if(name.data == other.name.data) Success
                else Failure(this, "suptypeOf", other)
            //This could be anything so we treat them as such
            //     they should only be around during type inferenz and not type checking
            is TypeVar -> Failure(this, "suptypeOf", other)
            UnknownType -> Failure(this, "suptypeOf", other)
        }
        //This could be anything so we treat them as such
        //     they should only be around during type inferenz and not type checking
        is TypeVar -> if(this == other){
            Success
        } else{
            Failure(this, "suptypeOf", other)
        }
        UnknownType -> Failure(this, "suptypeOf", other)
    }
}

private fun Constraint.satisfies(other: TraitConstraint):TypeCheckingResult {
    return when(this){
        //Only happens in the GenericType Case of Type.satisfies
        is DataType -> (this as Type).satisfies(other)
        is TraitConstraint -> {
            if(base.data == other.base.data){
                if(parameters.subTypeOf(other.parameters) == Success) return Success
            }
            val traits = resolvedConstraints(this);
            if(traits.isEmpty()) Success
            else traits.fold<_,TypeCheckingResult>(Failure(this, "suptypeOf", other)){ s,e ->
                if(s == Success) Success
                else e.satisfies(other)
            }
        }
    }
}

private fun List<TypeParam>.subTypeOf(other:List<TypeParam>):TypeCheckingResult {
    assert(size == other.size)
    return this.zip(other).fold<_,TypeCheckingResult>(Success) { s, e ->
        if (s is Failure) s
        else {
            assert(e.first.variance == e.second.variance)
            when (e.first.variance) {
                Variance.CO -> e.first.param.data.subTypeOf(e.second.param.data)
                Variance.CONTRA -> e.second.param.data.subTypeOf(e.first.param.data)
                Variance.IN -> e.first.param.data.sameTypeAs(e.second.param.data)
            }
        }
    }
}

private fun List<TypeParam>.sameTypeAs(other:List<TypeParam>):TypeCheckingResult {
    assert(size == other.size)
    return this.zip(other).fold<_,TypeCheckingResult>(Success) { s, e ->
        if (s is Failure) s
        else e.first.param.data.sameTypeAs(e.second.param.data)
    }
}

//Used for invariance
fun Type.sameTypeAs(other: Type):TypeCheckingResult  {
    if(this === other) return Success
    return when(this){
        is DataType -> {
            if(other !is DataType) return Failure(this, "is", other)
            if(base.data != other.base.data) return Failure(this, "is", other)
            return parameters.sameTypeAs(other.parameters)
        }
        is GenericType -> {
            if(other !is GenericType) return Failure(this, "is", other)
            if(name.data == other.name.data) Success
            else Failure(this, "is", other)
        }
        //This could be anything so we treat them as such
        //     they should only be around during type inferenz and not type checking
        is TypeVar -> if(this == other){
            Success
        } else{
            Failure(this, "is", other)
        }
        UnknownType -> Failure(this, "is", other)
    }
}

fun commonSuperType(t1:Type, t2:Type):Type? {
    if(t1.subTypeOf(t2) == Success) return t2
    if(t2.subTypeOf(t1) == Success) return t1
    val parent = resolvedParent(t1) ?: return null
    return commonSuperType(parent,t2)
}

