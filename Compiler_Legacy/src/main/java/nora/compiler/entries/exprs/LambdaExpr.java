package nora.compiler.entries.exprs;

import nora.compiler.entries.unresolved.RawArgument;
import nora.compiler.resolver.ContextResolver;

import java.util.List;

public class LambdaExpr extends UnresolvedExpr {
    private final List<RawArgument> args;
    private final Expr body;

    public LambdaExpr(List<RawArgument> args, Expr body) {
        this.args = args;
        this.body = body;
    }

    @Override
    public Expr resolve(ContextResolver resolver) {
        var nArgs = args.stream().map(a -> a.resolve(resolver)).toList();
        var argNames = args.stream().map(RawArgument::name).toList();
        return resolver.withCapturingContext(argNames, ctx -> {
            var nExpr = body.resolve(ctx);
            var capts = ctx.getAllCaptures();
            return new nora.compiler.entries.exprs.resolved.LambdaExpr(capts,nArgs,nExpr);
        });
    }
}
