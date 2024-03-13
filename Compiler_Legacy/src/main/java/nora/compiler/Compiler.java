package nora.compiler;

import nora.compiler.entries.BuildUnitRoot;
import nora.compiler.processing.BuiltinMapper;
import nora.compiler.processing.TypeCollector;
import nora.compiler.resolver.Discovery;
import nora.compiler.processing.TypeCheckContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Compiler {
    public static void main(String[] args) throws IOException {
        //Todo: needs to support 1 out, multi in.
        var source = Path.of(args[0]);
        var buildDir = source.getParent().resolve("build");

        var paths = Discovery.scanAll(List.of(source));
        paths.forEach(d -> {
            if(d instanceof BuildUnitRoot rr){
                rr.resolve(paths);
                return false;
            }
            return true;
        });
        TypeCheckContext.init(paths);
        paths.forEach(d -> {
            if(d instanceof BuildUnitRoot rr){
                rr.validateAndInfer();
                return false;
            }
            return true;
        });
        if(Files.exists(buildDir)){
            final List<Path> pathsToDelete = Files.walk(buildDir).sorted(Comparator.reverseOrder()).collect(Collectors.toList());
            for(Path path : pathsToDelete) {
                try { Files.deleteIfExists(path); } catch (Exception ignore){}
            }
        }
        paths.forEach(d -> {
            switch (d.getKind()){
                case Root:
                case Package:
                case Module:
                case Trait:
                    return true;
                case Data:
                case Function:
                case MultiMethod:
                    d.generateCode(buildDir);
                    return false;
            }
            return false;
        });
        TypeCollector.get().generateCode(buildDir);
        BuiltinMapper.generateConfig(buildDir);
        Files.write(buildDir.resolve(".meta.nora"),List.of("nora.meta.core.NoraCoreMetaLanguageProtocol"));
    }
}
