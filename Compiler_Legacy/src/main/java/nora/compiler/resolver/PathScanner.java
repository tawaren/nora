package nora.compiler.resolver;

import nora.compiler.entries.Definition;
import nora.compiler.entries.Package;
import nora.compiler.resolver.PathTreeNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PathScanner {

    @FunctionalInterface
    public interface DefinitionProcessor {
        void processAndRecordModule(Path moduleFile, String name, String fullyQualifiedName, PathTreeNode packageNode);
    }

    private static final String NORA_ENDING = ".nora";

    public static void discoverPaths(Path directory, String qualifiedName,  PathTreeNode packageNode, DefinitionProcessor proc) {
        try {
            Files.list(directory).forEach((Path entry) -> {
                var name = entry.getFileName().toString();
                if (Files.isDirectory(entry)) {
                    var newQualifiedName = name;
                    if(qualifiedName != null) newQualifiedName = qualifiedName+"."+name;
                    var descended = packageNode.addAndDescend(name, new Package(newQualifiedName));
                    discoverPaths(entry, newQualifiedName, descended, proc);
                } else if (name.endsWith(NORA_ENDING)) {
                    var moduleName = name.substring(0, name.length() - NORA_ENDING.length());
                    var newQualifiedName = name;
                    if(qualifiedName != null) newQualifiedName = qualifiedName+"."+moduleName;
                    proc.processAndRecordModule(entry, moduleName, newQualifiedName, packageNode);
                }
            });
        } catch (IOException e) {
            System.out.println("P:"+directory.toAbsolutePath().toString());
            //Should not happpen
            e.printStackTrace();
        }
    }
}
