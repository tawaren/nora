package nora.compiler.entries.exprs.resolved;

import nora.compiler.entries.exprs.Expr;
import nora.compiler.resolver.ContextResolver;

public abstract class ResolvedExpr extends Expr {
    @Override
    public Expr resolve(ContextResolver resolver) {
        throw new RuntimeException("An expression can ony be resolved once");
    }
}
