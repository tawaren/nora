package nora.files

import java.nio.file.Path
import kotlin.io.path.*

fun discoverModules(modulePaths: List<Path>, rootDir: Path):Pair<Map<String,Path>,List<String>> {
    return modulePaths.fold(Pair(mapOf(), listOf())){ (r,l),p ->
        val (nr, nl) = traverseFiles(rootDir.resolve(p), "")
        Pair(r+nr,l+nl)
    }
}

fun extendName(name:String,path:Path):String {
    if(name.isEmpty()) return path.nameWithoutExtension
    return name+"."+path.nameWithoutExtension
}

fun traverseFiles(path:Path, name:String):Pair<Map<String,Path>,List<String>>{
    if(path.isRegularFile()){
        if(path.extension == "nora"){
            val moduleName = extendName(name, path)
            return Pair(mapOf(Pair(moduleName, path)), listOf(moduleName))
        }
        return Pair(mapOf(), listOf())
    } else if(path.isDirectory()) {
        val newName = extendName(name, path)
        return path.listDirectoryEntries().fold(Pair(mapOf(), listOf())){ (r,l),p ->
            val (nr, nl) = traverseFiles(p, newName)
            Pair(r+nr,l+nl)
        }
    }
    return Pair(mapOf(), listOf())
}