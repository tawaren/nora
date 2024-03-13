package nora.compiler.entries.exprs;

import nora.compiler.entries.ref.ParametricReference;
import nora.compiler.resolver.ContextResolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateExpr extends UnresolvedExpr {
    private final ParametricReference target;
    private final Map<String,Expr> fields;
    private final boolean isTailCtx;

    public CreateExpr(ParametricReference target, Map<String,Expr> fields, boolean isTailCtx) {
        this.target = target;
        this.fields = fields;
        this.isTailCtx = isTailCtx;
    }

    @Override
    public Expr resolve(ContextResolver resolver) {
        var result = target.resolve(resolver);
        Map<String, Expr> newArgs = new HashMap<>();
        fields.forEach((key, value) -> newArgs.put(key, value.resolve(resolver)));
        return new nora.compiler.entries.exprs.resolved.CreateExpr(result, newArgs, isTailCtx);
    }


}
