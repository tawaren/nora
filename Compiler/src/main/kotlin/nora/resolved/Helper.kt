package nora.resolved

import nora.*
import nora.resolved.inferenz.type

fun Data<ResolvedState>.getField(name:String, includeHidden:Boolean = false):Elem<Argument<ResolvedState>>? {
    val f = fields.find { it.data.name.data.name == name }
    if(f != null) return f
    if(includeHidden){
        val h = hiddenFields.find { it.data.name.data.name == name }
        if(h != null) return h
    }
    if(parent != null) {
        return when(val def = resolvedDefinition(parent.data.name).toNullable()){
            is Data<ResolvedState> -> def.getField(name, includeHidden)
            else -> null
        }
    }
    return null
}


fun Data<ResolvedState>.getAllFields(includeHidden:Boolean = false):List<Elem<Argument<ResolvedState>>> {
    val parFields = if(parent != null) {
        when(val def = resolvedDefinition(parent.data.name).toNullable()){
            is Data<ResolvedState> -> def.getAllFields(includeHidden)
            else -> listOf()
        }
    } else {
        listOf()
    }
    return if(includeHidden){
        parFields + fields + hiddenFields
    } else {
        parFields + fields
    }
}

/*
fun IfExpr<ResolvedState>.extractFlowBinding():Pair<Loc, Elem<Expr<ResolvedState>>>? {
    return if(cond.data is InstanceOfExpr && cond.data.source.data is ValExpr){
        //We can flow type
        val bind = cond.data.source.data.source.data
        return Pair(bind, cond.data.type)
    } else {
        //We can not fow type
        null
    }
}
*/


