package nora.compiler.entries.exprs;

import nora.compiler.resolver.ContextResolver;

public class LetExpr extends UnresolvedExpr {
    private final String name;
    private final Expr bind;
    private final Expr res;

    public LetExpr(String name, Expr bind, Expr res) {
        this.name = name;
        this.bind = bind;
        this.res = res;
    }

    @Override
    public nora.compiler.entries.exprs.resolved.LetExpr resolve(ContextResolver resolver) {
        var nBind = bind.resolve(resolver);
        return resolver.withBinding(name, (ctx, bind) -> {
            var nRes =  res.resolve(ctx);
            return new nora.compiler.entries.exprs.resolved.LetExpr (bind, nBind, nRes);
        });
    }
}
