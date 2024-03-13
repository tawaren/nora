package nora.resolved


import nora.*
import nora.Parameterized
import nora.typesys.*


typealias ResolvedState = TypeRelated


fun resolveConstraintCtr(name:Elem<Ref>, expectedArgs:Int): Maybe<Constraint> {
    return when (val def = resolvedDefinition(name.data.name).toNullable()) {
        is Trait<ResolvedState> -> {
            Some(TraitConstraint(name, def.generics.map {
                TypeParam(
                    it.data.variance.data,
                    Elem(GenericType(
                        it.data.name,
                        it.data.bounds.map { d -> d.map { c -> c as Constraint } }
                    ), it.src)
                )
            }))
        }
        is VarArgParameterized<ResolvedState> -> {
            Some(DataType(name, def.genGenerics(expectedArgs).map {
                TypeParam(
                    it.data.variance.data,
                    Elem(GenericType(
                        it.data.name,
                        it.data.bounds.map { d -> d.map { c -> c as Constraint } }
                    ), it.src)
                )
            }))
        }
        is Parameterized<ResolvedState> -> {
            Some(DataType(name, def.generics.map {
                TypeParam(
                    it.data.variance.data,
                    Elem(GenericType(
                        it.data.name,
                        it.data.bounds.map { d -> d.map { c -> c as Constraint } }
                    ), it.src)
                )
            }))
        }
        else -> None
    }
}

fun resolveTypeCtr(name:Elem<Ref>, expectedArgs:Int): Maybe<Type> {
    return when (val def = resolvedDefinition(name.data.name).toNullable()) {
        is VarArgParameterized<ResolvedState> -> {
            Some(DataType(name, def.genGenerics(expectedArgs).map {
                TypeParam(
                    it.data.variance.data,
                    Elem(GenericType(
                        it.data.name,
                        it.data.bounds.map { d -> d.map { c -> c as Constraint } }
                    ), it.src)
                )
            }))
        }
        is Parameterized<ResolvedState> -> {
            Some(DataType(name, def.generics.map {
                TypeParam(
                    it.data.variance.data,
                    Elem(GenericType(
                        it.data.name,
                        it.data.bounds.map { d -> d.map { c -> c as Constraint } }
                    ), it.src)
                )
            }))
        }
        else -> None
    }

}
