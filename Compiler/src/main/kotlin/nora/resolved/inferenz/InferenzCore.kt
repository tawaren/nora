package nora.resolved.inferenz

import nora.*
import nora.parsed.ParsedState
import nora.resolved.ResolvedState
import nora.typesys.*
/*
 This is a modified HM
 This does not support subtyping
  However, the unification keeps the first valid type instead of going to an error type
           if unification happens in flow order (which it does)
            an assignments to an already inferred/fixed type will not lead to an error
            ensuring that in many cases we can assign to a superType
  Further, the unification algorithm is in flow order
           and only uses HM when not already a concrete type is known
           Its basically flow typing with HM support for special cases
  After this another flow typing pass is made using the HM results as hints
    where flow typing is not enough
   Without HM hints Backtracking would be necessary

*/

sealed interface TypeState
data class Forward(val tVar: TypeVar):TypeState
data class Delayed(val c:(Type) -> Type, val u: (InferenceContext, Type) -> InferenceContext):TypeState {
    fun apply(ctx:InferenceContext, t:Type):InferenceContext = u(ctx,c(t))
}
data class Result(val type: Type):TypeState

//Todo: Allow to register lastResorts
data class InferenceContext(val mapping:Map<TypeVar,TypeState>, val fallback:Map<TypeVar,Type>)

fun InferenceContext.resolve(t:Type):Pair<Type,InferenceContext>{
    return when(t){
        is TypeVar -> when(val entry = mapping[t]) {
            is Forward -> {
                val (res, nCtx) = resolve(entry.tVar)
                Pair(res, nCtx.copy(mapping = nCtx.mapping + Pair(t, Forward(res as TypeVar))))
            }
            else -> Pair(t, this)
        }
        else -> Pair(t,this)
    }
}

//Can be used for early breakout to loosen coupling
fun InferenceContext.getType(t:Type):Type? {
    return when(t){
        is TypeVar -> when(val entry = mapping[t]){
            is Forward -> getType(entry.tVar)
            is Result -> entry.type
            else -> null
        }
        else -> t
    }
}

fun InferenceContext.addFallback(tv:TypeVar, t:Type):InferenceContext = copy(fallback = fallback + Pair(tv,t))

fun InferenceContext.expectReturnType(typ:Type, f:(InferenceContext) -> Pair<Type,InferenceContext>):InferenceContext {
    val (t,c) = f(this)
    return c.unify(t,typ)
}

fun InferenceContext.unify(cur:Type, upper:Type):InferenceContext {
    val (curRes, nCtx) = resolve(cur)
    val (upperRes, nCtx2) = nCtx.resolve(upper)
    if(curRes == upperRes) return nCtx2
    return nCtx2.unifyDispatch(curRes, upperRes)
}


private fun InferenceContext.forward(source:TypeVar, target:TypeVar):InferenceContext {
    return copy(mapping = mapping + Pair(source, Forward(target)))
}

private fun InferenceContext.handleDelays(entry:Delayed?, t2:Type):InferenceContext{
    assert(t2 !is TypeVar)
    if(entry == null) return this
    return entry.apply(this, t2)
}

private fun InferenceContext.unifyDispatch(t1:Type, t2:Type):InferenceContext {
    //Just dispatch to simulate multimethod
    return when(t1){
        is TypeVar -> when (t2) {
            is TypeVar -> unifyCase(t1, t2)
            else -> unifyCase(t1, t2)
        }
        else -> when(t2){
            is TypeVar -> unifyCase(t1, t2)
            else -> unifyCase(t1, t2)
        }
    }
}

private fun InferenceContext.unifyCase(t1:TypeVar, t2:TypeVar):InferenceContext {
    val t1Entry = mapping[t1]
    val t2Entry = mapping[t2]
    return if(t2Entry == null) {
        forward(t2, t1)
    } else if(t1Entry == null){
        forward(t1, t2)
    } else {
        if(t1Entry is Delayed && t2Entry is Delayed){
            val joinedDelays = Delayed(
                { t2Entry.c(t1Entry.c(it)) },
                { c, t -> t2Entry.u(t1Entry.u(c,t),t)}
            )
            copy(mapping = mapping + Pair(t2, joinedDelays)).forward(t1, t2)
        } else if(t1Entry is Delayed){
            handleDelays(t1Entry, (t2Entry as Result).type).forward(t1, t2)
        } else if(t2Entry is Delayed){
            handleDelays(t2Entry, (t1Entry as Result).type).forward(t2, t1)
        } else {
            //We do not forward because:
            // if both are same forwarding does not change anything
            // if both are different, we would destroy the individual type information for no benefit
            unifyCase((t1Entry as Result).type, (t2Entry as Result).type)
        }
    }
}

private fun InferenceContext.unifyCase(t1:TypeVar, t2:Type):InferenceContext {
    val t1Entry = mapping[t1]
    return if(t1Entry == null || t1Entry is Delayed){
        handleDelays(t1Entry as Delayed?, t2).copy(mapping = mapping + Pair(t1, Result(t2)))
    } else {
        unifyCase((t1Entry as Result).type, t2)
    }
}

private fun InferenceContext.unifyCase(t1:Type, t2:TypeVar):InferenceContext {
    val t2Entry = mapping[t2]
    return if(t2Entry == null || t2Entry is Delayed){
        handleDelays(t2Entry as Delayed?, t1).copy(mapping = mapping + Pair(t2, Result(t1)))
    } else {
        unifyCase((t2Entry as Result).type, t1)
    }
}

private fun InferenceContext.unifyCase(t1:Type, t2:Type):InferenceContext {
    return if(t1 is DataType && t2 is DataType){
        if(areRelated(t1.base,t2.base)){
            t1.parameters.zip(t2.parameters).fold(this){ c, (sub,sup) ->
                assert(sub.variance == sup.variance)
                c.unify(sub.param.data,sup.param.data)
            }
        } else {
            this
        }
    } else {
        //Todo: Treat generics special - use upper bounds for merging.
        this
    }
}

private fun areRelated(b1:Elem<Ref>,b2:Elem<Ref>):Boolean{
    if(b1.data == b2.data) return true
    val def1 = parsedDefinition(b1.data.name).toNullable() as Data<ParsedState>
    return if(def1.parent == null) {
        val def2 = parsedDefinition(b2.data.name).toNullable() as Data<ParsedState>
        if(def2.parent == null) {
            false
        } else {
            areRelated(def2.parent, b1)
        }
    } else {
        areRelated(def1.parent, b2)
    }
}

//Helper to delay unifications that require a concrete type
fun InferenceContext.whenTyped(t:Type, c:(Type) -> Type, u:(InferenceContext, Type) -> InferenceContext):InferenceContext {
    val (res, nCtx) = resolve(t)
    return when(res) {
        is TypeVar -> when(val entry = nCtx.mapping[res]){
            is Delayed -> {
                val joinedDelays = Delayed(
                    { entry.c(c(it))},
                    { conv, rt -> entry.u(u(conv,rt),rt)}
                )
                nCtx.copy(mapping = mapping + Pair(res, joinedDelays))
            }
            is Result -> {
                val nt = c(entry.type)
                val nCtx2 = u(nCtx,nt)
                nCtx2.copy(mapping = mapping + Pair(res, Result(nt)))
            }
            else -> nCtx.copy(mapping = nCtx.mapping + Pair(res, Delayed(c,u)))
        }
        else -> u(nCtx,res)
    }
}



fun collectTraitNames(t:TypeRelated):Set<String> {
    fun collectTraitList(traits:List<Elem<ResolvedState>>):Set<String> {
        return traits.flatMap { collectTraitNames(it.data) }.toSet()
    }

    return when(t){
        is DataType -> when(val def = resolvedDefinition(t.base.data.name).toNullable()){
            //Todo: have common super
            is Data -> collectTraitList(def.traits)
            is Tuple -> collectTraitList(def.traits)
            is nora.Array -> collectTraitList(def.traits)
            is Trait -> collectTraitList(def.traits)
            else -> setOf()
        }
        is Constraint -> when(val def = resolvedDefinition(t.base.data.name).toNullable()){
            is Data -> collectTraitList(def.traits)
            is Tuple -> collectTraitList(def.traits)
            is nora.Array -> collectTraitList(def.traits)
            is Trait -> collectTraitList(def.traits)
            else -> setOf()
        } + t.base.data.name
        else -> setOf()
    }
}

private fun coerceToSatisfy(t:Type, traitNames:Set<String>):Type? {
    val presentTraits = collectTraitNames(t)
    if(presentTraits.containsAll(traitNames)) return t
    val parent = resolvedParent(t) ?: return null
    return coerceToSatisfy(parent, traitNames)
}

fun coerceToSatisfy(t:Type, g:Generic<ResolvedState>):Type {
    if(g.bounds.isEmpty()) return t
    val traitNames = g.bounds.map { it.data }.filterIsInstance<TraitConstraint>().map { it.base.data.name }.toSet()
    if(traitNames.isEmpty()) return t
    return coerceToSatisfy(t,traitNames)?:t
}

private fun collectAllConstraints(t:TypeRelated):List<Constraint> {
    fun collectRec(traits:List<Elem<ResolvedState>>, subst:Array<Type>):List<Constraint>{
        return traits.flatMap { collectAllConstraints((it.data as Constraint).substitute(subst)) }
    }

    fun collectDef(name:String, subst:Array<Type>):List<Constraint> {
        return when(val def = resolvedDefinition(name).toNullable()){
            is Data -> collectRec(def.traits, subst)
            is Tuple -> collectRec(def.traits, subst)
            is nora.Array -> collectRec(def.traits, subst)
            else -> listOf()
        }
    }

    return when(t){
        is TraitConstraint -> collectDef(t.base.data.name, t.parameters.map { it.param.data }.toTypedArray()) + t
        is DataType -> collectDef(t.base.data.name, t.parameters.map { it.param.data }.toTypedArray())
        is GenericType -> t.bounds.flatMap { collectAllConstraints(it.data) }
        else -> listOf()
    }
}

fun collectMatchingConstraints(t:Type, g:Generic<ResolvedState>):List<Constraint> {
    val targets = g.bounds.map { (it.data as Constraint).base.data.name }.toSet()
    if(targets.isEmpty()) return listOf()
    val all = collectAllConstraints(t)
    return all.filter { targets.contains(it.base.data.name) }
}

fun InferenceContext.unifyConstraints(subst:Array<Type>, g:Generic<ResolvedState>, t:Type):InferenceContext{
    return whenTyped(t, {coerceToSatisfy(it, g)}){ctx, coerced ->
        val matches = collectMatchingConstraints(coerced, g)
        if(matches.isEmpty()) {
            ctx
        } else {
            val gBounds = g.bounds.associate { Pair((it.data as Constraint).base.data.name, it.data.parameters) }
            matches.fold(ctx){c, b ->
                val gBound = gBounds[b.base.data.name]
                gBound?.zip(b.parameters)?.fold(c) { c2, (g,b2) ->
                    c2.unify(g.param.data.substitute(subst), b2.param.data)
                }?: c
            }
        }
    }
}

data class TypeResolutionContext(val mapping: Map<TypeVar,TypeState>)

fun InferenceContext.resolve():TypeResolutionContext {
    val res = fallback.entries.fold(this) { c, (e,fb) ->
        val (res, nc) = c.resolve(e)
        when (val entry = nc.mapping[res as TypeVar]) {
            is Delayed -> {
                val nc2 = entry.apply(nc, fb)
                nc2.copy(mapping = nc2.mapping + Pair(res, Result(fb)))
            }
            null -> nc.copy(mapping = nc.mapping + Pair(res, Result(fb)))
            else -> nc
        }
    }
    //Todo: Call last resorts
    return TypeResolutionContext(res.mapping)
}


//Todo: add recursion protection: Fallback to unknown <-- not sure that it can happen


private fun TypeResolutionContext.resolveParams(ps:List<TypeParam>):List<TypeParam> {
    return ps.map { TypeParam(it.variance, it.param.map { p -> resolveType(p) }) }
}

private fun TypeResolutionContext.resolveConstraint(t:Constraint):Constraint {
    return when(t){
        is TraitConstraint -> TraitConstraint(t.base, resolveParams(t.parameters))
        is DataType -> DataType(t.base, resolveParams(t.parameters))
    }
}

fun TypeResolutionContext.resolveType(t:Type):Type {
    return when(t){
        is DataType -> DataType(t.base, resolveParams(t.parameters))
        is GenericType -> GenericType(t.name, t.bounds.map { it.map { c -> resolveConstraint(c) }} )
        is TypeVar -> when(val entry = mapping[t]){
            is Forward -> resolveType(entry.tVar)
            is Result -> resolveType(entry.type)
            else -> UnknownType
        }
        UnknownType -> UnknownType
    }
}

fun TypeResolutionContext.isKnownType(t:Type):Boolean {
    return when(t){
        is DataType -> true
        is GenericType -> true
        is TypeVar -> when(val entry = mapping[t]){
            is Forward -> isKnownType(entry.tVar)
            is Result -> true
            else -> false
        }
        UnknownType -> false
    }
}