package nora.typesys

import nora.Gen
import nora.map


//Type Substitution Operations
fun Type.substitute(generics:Array<Type>): Type {
    return when(this) {
        is DataType -> this.substitute(generics)
        is GenericType -> {
            val pos = name.data.pos
            if(pos < generics.size) generics[pos]
            else UnknownType
        }
        //Todo: we later need a substitution that can substitute TypeVars
        is TypeVar -> this
        UnknownType -> UnknownType
    }
}

fun Constraint.substitute(generics:Array<Type>): Constraint {
    return when(this) {
        is DataType -> this.substitute(generics)
        is TraitConstraint -> this.substitute(generics)
    }
}

private fun DataType.substitute(generics:Array<Type>): DataType {
    return DataType(base, parameters.map {
        TypeParam(it.variance, it.param.map{ p ->
            p.substitute(generics)
        })
    })
}

private fun TraitConstraint.substitute(generics:Array<Type>): TraitConstraint {
    return TraitConstraint(base, parameters.map {
        TypeParam(it.variance, it.param.map{ p ->
            p.substitute(generics)
        })
    })
}