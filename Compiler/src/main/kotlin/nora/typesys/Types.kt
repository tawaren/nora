package nora.typesys

import nora.Elem
import nora.Gen
import nora.Ref
import nora.Src

enum class Variance {
    CO, CONTRA, IN
}

//Types: Extended Form of Refs (capturing target variance & providing algos)
sealed interface TypeRelated
sealed interface Constraint :TypeRelated {val base: Elem<Ref>; val parameters:List<TypeParam>}
sealed interface Type : TypeRelated

data class TypeParam(val variance: Variance, val param: Elem<Type>)
data class TraitConstraint(override val base: Elem<Ref>, override val parameters:List<TypeParam>): Constraint
//includes tuples/array/function etc...
data class DataType(override val base: Elem<Ref>, override val parameters:List<TypeParam>): Type, Constraint
data class GenericType(val name: Elem<Gen>, val bounds:List<Elem<Constraint>>): Type
//We could decide Early that we have no clue
data object UnknownType: Type
//Must be inferred
data class TypeVar(val id:Int, val src: Src): Type

