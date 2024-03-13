package nora.parsed

import nora.*


typealias ParsedState = ParametricReference

//Todo: we need a Module representation -- Should go into parsed
data class Module(val defs:Map<String,Definition<ParsedState>>)

fun parseModule(name:String): Maybe<Module> {
    return when(val path = modulePath(name)){
        None -> None
        is Some -> {
            TODO("open file @ Path and parse it using antlr")
        }
    }
}


fun parseDefinition(path:String):Maybe<Definition<ParsedState>> {
    val res = path.split("::");
    if(res.size != 2) return None
    return when(val mod = parsedModule(res[0])){
        None -> None
        is Some -> mod.elem.defs[res[1]].toMaybe()
    }
}
