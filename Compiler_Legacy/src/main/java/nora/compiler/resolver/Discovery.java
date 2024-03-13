package nora.compiler.resolver;

import nora.compiler.entries.Definition;
import nora.compiler.parser.ModuleParser;
import nora.compiler.resolver.PathTreeNode;
import nora.compiler.resolver.PathScanner;

import java.nio.file.Path;
import java.util.List;

public class Discovery {
    public static PathTreeNode scanAll(List<Path> roots){
        var rootNode = new PathTreeNode("", Definition.ROOT);
        for(Path p: roots) PathScanner.discoverPaths(p, null, rootNode,  ModuleParser::parseModule);
        return rootNode;
    }

}
