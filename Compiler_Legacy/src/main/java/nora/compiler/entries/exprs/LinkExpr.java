package nora.compiler.entries.exprs;

import nora.compiler.entries.exprs.resolved.InstanceExpr;
import nora.compiler.entries.ref.ParametricReference;
import nora.compiler.entries.unresolved.RawGeneric;
import nora.compiler.resolver.ContextResolver;

import java.util.List;

public class LinkExpr extends UnresolvedExpr {
    public final ParametricReference ref;

    public LinkExpr(ParametricReference ref) {
        this.ref = ref;
    }

    @Override
    public Expr resolve(ContextResolver resolver) {
        return new InstanceExpr(ref.resolve(resolver));
    }

    public boolean isSelf(String curDef, List<ParametricReference> gens) {
        //Todo: in theory we could do it with generics, as long as they are the same
        if(!ref.getName().equals(curDef)) return false;
        if(ref.getApplies() == null) return false;
        var appls = ref.getApplies();
        if(gens.size() != appls.size()) return false;
        for(int i = 0; i < gens.size(); i++){
            if(appls.get(i) != gens.get(i)) return false;
        }
        return true;
    }
}
