package nora.meta.core;

import meta.MetaLanguageObject;
import meta.MetaLanguageProtocol;
import nora.vm.runtime.NoraVmContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class NoraCoreMetaLanguageProtocol implements MetaLanguageProtocol {
    private final Map<String, Function<Object[], MetaLanguageObject>> registry = new HashMap<>();

    @Override
    public MetaLanguageObject createObject(String name, Object... args) {
       var builder =  registry.get(name);
       if(builder != null) return builder.apply(args);
       return null;
    }

    @Override
    public void initialize(NoraVmContext context) throws Exception {
        MetaLanguageObject tupleHandler = new TupleLayoutHandler();
        registry.put("tuple_handler", args -> {
            assert args.length == 0;
            return tupleHandler;
        });
        registry.put("object_handler", args -> {
            assert args.length == 2;
            return new ObjectLayoutHandler((Integer)args[0],(Integer)args[1]);
        });

        var base = context.getBase();
        var lines = Files.readAllLines(base.resolve(Path.of(".meta", "NoraMetaLanguageProtocol.builtins")));
        var reg = context.getSpecialMethodRegistry();
        lines.forEach(l -> {
            var res = l.split("=");
            reg.bindSpecialMethod(res[0].trim(), res[1].trim());
        });
    }

    @Override
    public String getName() {
        return "nora_core";
    }
}
