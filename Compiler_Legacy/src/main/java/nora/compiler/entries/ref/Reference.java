package nora.compiler.entries.ref;

import nora.compiler.entries.*;
import nora.compiler.entries.DefInstance;
import nora.compiler.entries.Instance;
import nora.compiler.processing.TypeCheckContext;
import nora.compiler.resolver.ContextResolver;
import nora.compiler.resolver.bindings.DefBinding;
import nora.compiler.resolver.bindings.TypeBinding;

import java.util.List;

public class Reference implements Resolvable<Definition>{
    private final String reference;

    public Reference(String reference) {
        this.reference = reference;
    }

    @Override
    public Definition resolve(ContextResolver resolver) {
        var def = resolver.resolve(reference);
        if(def instanceof DefBinding db) return db.getDef();
        throw new RuntimeException("Could not find reference");
    }

    public Instance resolveInstance(ContextResolver resolver, List<Instance> applies) {
        var def = resolver.resolve(reference);
        if(def instanceof DefBinding db) {
            var dbDef = db.getDef();
            var par = dbDef.asParametric();
            var fApplies = applies;
            if(fApplies == null && par != null && par.numGenerics() == 0 && !TypeCheckContext.isSpecialVarargDef(db.getDef())){
                fApplies = List.of();
            }
            return new DefInstance(dbDef, fApplies);
        }
        if(applies == null && def instanceof TypeBinding tb){
            var arg = tb.getArg();
            return new GenericInstance(resolver.getGenericVariance(arg), arg, resolver.getGenericBound(arg));
        }
        throw new RuntimeException("Could not find reference");
    }

    public String getName(){
        var res = reference.split("\\.");
        return res[res.length-1].replace("::", "__");
    }

    @Override
    public String toString() {
        return getName();
    }
}
