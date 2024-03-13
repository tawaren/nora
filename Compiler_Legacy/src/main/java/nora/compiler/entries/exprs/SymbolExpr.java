package nora.compiler.entries.exprs;

import nora.compiler.entries.DefInstance;
import nora.compiler.entries.exprs.resolved.BindingExpr;
import nora.compiler.entries.exprs.resolved.InstanceExpr;
import nora.compiler.resolver.ContextResolver;
import nora.compiler.resolver.bindings.DefBinding;

import java.util.List;

public class SymbolExpr extends UnresolvedExpr {
    public final String ref;

    public SymbolExpr(String ref) {
        this.ref = ref;
    }

    @Override
    public Expr resolve(ContextResolver resolver) {
        var bind = resolver.resolve(ref);
        if(bind instanceof DefBinding db) {
            var inst = new DefInstance(db.getDef(), null);
            return new InstanceExpr(inst);
        }
        return new BindingExpr(bind);
    }
}
