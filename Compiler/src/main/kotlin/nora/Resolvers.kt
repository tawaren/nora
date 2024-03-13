package nora

import nora.codegen.TypeId
import nora.codegen.rebindNames
import nora.codegen.typeIds
import nora.files.discoverModules
import nora.parsed.ParsedState
import nora.parsed.Module
import nora.parsed.collecting.collectAllRefs
import nora.parser.ModuleDefinitionParser
import nora.resolved.ResolvedState
import nora.typesys.*
import java.nio.file.Path

//Note: These simulate nora's environment and argument oracles
//      Will be set once by main
private var moduleRootDirs: List<Path> = listOf()
private var rootDir: Path = Path.of("")

fun init(rootDirInit: Path, moduleRootDirsInit:List<Path>) {
    rootDir = rootDirInit
    moduleRootDirs = moduleRootDirsInit
}


//Note: These are memoized/cached versions of transformation results or often used derivations
private val modulePaths = mutableMapOf<String, Path>()
private val moduleList = mutableListOf<String>()
private val moduleDefsCache = mutableMapOf<String, Maybe<Set<String>>>()
private val parsedModuleCache = mutableMapOf<String,Maybe<Module>>()
private val parsedDefinitionCache = mutableMapOf<String,Maybe<Definition<ParsedState>>>()
private var definitionMemberCache: Maybe<Map<String, List<Ref>>> = None
private val resolvedDefinitionCache = mutableMapOf<String,Maybe<Definition<ResolvedState>>>()
private val typedDefinitionCache = mutableMapOf<String,Maybe<Definition<ResolvedState>>>()
private val boundedDefinitionCache = mutableMapOf<String,Maybe<Definition<ResolvedState>>>()
private val resolvedTraitCtrCache = mutableMapOf<Pair<String, Int>,Maybe<Constraint>>()
private val resolvedDataCtrCache = mutableMapOf<Pair<String, Int>,Maybe<Type>>()
private var resolvedTypeIds: Maybe<Map<String, TypeId>> = None


//Major transformation Results
// List of all available Modules
fun moduleList():List<String>{
    if(moduleList.isEmpty()){
        val (dir, list) = discoverModules(moduleRootDirs, rootDir)
        modulePaths.putAll(dir)
        moduleList.addAll(list)
    }
    return moduleList
}

//Module Source File Path
fun modulePath(name:String):Maybe<Path>{
    if(modulePaths.isEmpty()){
        val (dir, list) = discoverModules(moduleRootDirs, rootDir)
        modulePaths.putAll(dir)
        moduleList.addAll(list)
    }
    return modulePaths[name].toMaybe()
}

//Module to contained defs (for import resolution)
fun moduleDefs(name:String):Maybe<Set<String>>{
    return moduleDefsCache.computeIfAbsent(name){ modulePath(name).map { ModuleDefinitionParser.extractBindings(it) }}
}

//Raw Parsed Modules
fun parsedModule(name:String):Maybe<Module> {
    return parsedModuleCache.computeIfAbsent(name){ nora.parsed.parseModule(it) }
}

//Raw Parsed Definitions
fun parsedDefinition(path:String):Maybe<Definition<ParsedState>>{
    return parsedDefinitionCache.computeIfAbsent(path){ nora.parsed.parseDefinition(it) }
}

//Raw Member Definitions
fun definitionMembers(path:String):List<Ref>{
    if(definitionMemberCache == None){
        definitionMemberCache = Some(collectAllRefs())
    }
    return (definitionMemberCache as Some).elem[path] ?: listOf()
}

//Definition were Defined Types are resolved
fun resolvedDefinition(path:String):Maybe<Definition<ResolvedState>>{
    return resolvedDefinitionCache.computeIfAbsent(path){ parsedDefinition(it).map{d -> nora.parsed.resolveDefinition(d)} }
}

//Definition were all types are resolved (Defined and Inferred)
fun typedDefinition(path:String):Maybe<Definition<ResolvedState>>{
    return typedDefinitionCache.computeIfAbsent(path){ resolvedDefinition(it).map{d -> nora.resolved.typeDefinition(d)} }
}

//Definition were all types are resolved (Defined and Inferred)
fun boundedDefinition(path:String):Maybe<Definition<ResolvedState>>{
    return boundedDefinitionCache.computeIfAbsent(path){ typedDefinition(it).map{d -> d.rebindNames() } }
}

//Derivations -- Used in Type Inference and Checking
fun resolvedTraitCtr(name:Elem<Ref>, expectedArgs:Int):Maybe<Constraint>{
    return resolvedTraitCtrCache.computeIfAbsent(Pair(name.data.name, expectedArgs)){ nora.resolved.resolveConstraintCtr(name, expectedArgs) }
}

fun resolvedDataCtr(name:Elem<Ref>, expectedArgs:Int):Maybe<Type>{
    return resolvedDataCtrCache.computeIfAbsent(Pair(name.data.name, expectedArgs)){ nora.resolved.resolveTypeCtr(name, expectedArgs) }
}

fun resolveTypeId(path:String):Maybe<TypeId>{
    if(resolvedTypeIds == None){
        resolvedTypeIds = Some(typeIds())
    }
    return (resolvedTypeIds as Some).elem[path].toMaybe()
}

//Todo: can we move those?
fun resolvedParent(type:Type):Type?{
    return when(type){
        is DataType -> {
            val def = parsedDefinition(type.base.data.name).toNullable() as Data<ParsedState>
            if(def.parent == null) return null
            val substMap = type.parameters.map { it.param.data }.toTypedArray()
            resolvedDataCtr(def.parent, substMap.size).toNullable()?.substitute(substMap)
        }
        else -> null
    }
}

fun resolvedConstraints(type:Type):List<Constraint>{
    return when(type){
        is DataType -> {
            val def = parsedDefinition(type.base.data.name).toNullable() as Data<ParsedState>
            val subst = type.parameters.map { it.param.data }.toTypedArray()
            def.traits.mapNotNull { t -> resolvedTraitCtr(t.data.main.map { it as Ref }, subst.size).toNullable()?.substitute(subst) }
        }
        else -> listOf()
    }
}

fun resolvedConstraints(type:Constraint):List<Constraint>{
    val def = parsedDefinition(type.base.data.name).toNullable() as Data<ParsedState>
    val subst = type.parameters.map { it.param.data }.toTypedArray()
    return def.traits.mapNotNull { t -> resolvedTraitCtr(t.data.main.map { it as Ref }, subst.size).toNullable()?.substitute(subst) }
}

//For Tests
fun setParsedModuleEntries(entries:Map<String,Maybe<Module>>){
    parsedModuleCache.clear()
    parsedModuleCache.putAll(entries)
}

fun setParsedDefinitionEntries(entries:Map<String,Maybe<Definition<ParsedState>>>){
    parsedDefinitionCache.clear()
    parsedDefinitionCache.putAll(entries)
}

fun setResolvedDefinitionEntries(entries:Map<String,Maybe<Definition<ResolvedState>>>){
    resolvedDefinitionCache.clear()
    resolvedDefinitionCache.putAll(entries)
}

fun setResolvedTraitCtrEntries(entries:Map<String,Constraint>){
    resolvedTraitCtrCache.clear()
    resolvedTraitCtrCache.putAll(entries.map { Pair(Pair(it.key, 0), Some(it.value)) })
}

fun setResolvedDataCtrEntries(entries:Map<String,Type>){
    resolvedDataCtrCache.clear()
    resolvedDataCtrCache.putAll(entries.map { Pair(Pair(it.key, 0), Some(it.value)) })
}

fun setResolvedVarArgDataCtrEntries(entries:Map<Pair<String,Int>,Type>){
    resolvedDataCtrCache.clear()
    resolvedDataCtrCache.putAll(entries.mapValues { Some(it.value) })
}