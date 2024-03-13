package nora.compiler.processing;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

public class BuiltinMapper {

    private final static Map<String,String> builtinMethods = new HashMap<>();

    public static void addMapping(String method, String builtin){
        builtinMethods.put(method, builtin);
    }

    public static void generateConfig(Path base) throws IOException {
        var metaDir = base.resolve(Path.of(".meta"));
        Files.createDirectory(metaDir);
        var file = metaDir.resolve("NoraMetaLanguageProtocol.builtins");
        try(OutputStreamWriter out = new OutputStreamWriter(Files.newOutputStream(file, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
           for(var entry:builtinMethods.entrySet()){
               out.append(entry.getKey()).append("=")
                       .append(entry.getValue()).append("\n");
           }
        }
    }
}
