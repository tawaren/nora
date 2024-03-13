package nora.codegen

import nora.Ref
import nora.definitionMembers
import nora.parsed.collecting.DataRoots
import nora.resolveTypeId
import nora.toNullable
import nora.typesys.*

data class TypeId(val cat:Int, val start:Int, val end:Int){
    override fun toString(): String = "{$cat,$start,$end}"
}

const val primitiveAnnotation = "nora.lang.Primitives::Primitive"

private fun findPrimitive(root: Ref, prims:Set<Ref>): List<String>? {
    return if(prims.contains(root)) {
        listOf()
    } else {
        val members = definitionMembers(root.name)
        members.map {
            val path = findPrimitive(it, prims)
            if(path == null) null
            else listOf(it.name) + path
        }.first()
    }
}

private fun typeFamily(cat:Int, root: Ref):Map<String, TypeId> {
    val allPrimitives = definitionMembers(primitiveAnnotation).toSet()
    val primitivePath = findPrimitive(root, allPrimitives)
    fun process(cur:Ref, start:Int, primPath:List<String>?):Pair<Int,Map<String, TypeId>> {
        val sub = definitionMembers(cur.name)
        return if(sub.isEmpty()){
            Pair(start+1, mapOf(Pair(cur.name, TypeId(cat, start, start))))
        } else {
            val primP = primPath?.first()
            val (prim, rest) = sub.partition { it.name == primP }
            val primRem = primPath?.drop(1)
            val (nextStart, primKids) = prim.fold(Pair(start, mapOf<String, TypeId>())) { (s,m), k ->
               val (nStart, nMap) = process(k, s, primRem)
               Pair(nStart, m + nMap)
            }
            val (finalStart, kids) = rest.fold(Pair(nextStart, primKids)) { (s,m), k ->
                val (nStart, nMap) = process(k, s, null)
                Pair(nStart, m + nMap)
            }
            return Pair(finalStart+1, kids + Pair(cur.name, TypeId(cat, start, finalStart)))
        }
    }
    return process(root, 0, primitivePath).second
}

fun typeIds():Map<String, TypeId> {
    return definitionMembers(DataRoots.name).fold(Pair(mapOf<String, TypeId>(),0)){
        (res,cat), def -> Pair(res + typeFamily(cat, def), cat+1)
    }.first
}

fun Variance.shortString():String {
    return  when (this) {
        Variance.CO -> "+"
        Variance.CONTRA -> "-"
        Variance.IN -> ""
    }
}

fun Type.genTypePattern():String {
    return when(this){
        is DataType -> {
            val idString = resolveTypeId(base.data.name).toNullable()?.toString()?:"ERROR" //Should not happen shall we gen an error type
            if(parameters.isEmpty()){
                idString
            } else {
                val params = parameters.joinToString { it.variance.shortString() + it.param.data.genTypePattern() }
                "$idString[$params]"
            }
        }
        is GenericType -> "?${name.data.pos}"
        else -> "ERROR" //Should not happen shall we gen an error type
    }
}

//Cal forAll definitionMembers(DataRoots.name)
fun genTypeFamily(root:String, builder:StringBuilder):Pair<Int, StringBuilder> {
    //Todo: resolve all child ids

    val tId = resolveTypeId(root).toNullable()!!


    return Pair(tId.cat, builder)
}