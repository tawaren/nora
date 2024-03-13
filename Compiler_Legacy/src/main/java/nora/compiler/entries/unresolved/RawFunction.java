package nora.compiler.entries.unresolved;

import nora.compiler.entries.Instance;
import nora.compiler.entries.exprs.Expr;
import nora.compiler.entries.interfaces.Callable;
import nora.compiler.entries.proxies.FunctionResolverProxy;
import nora.compiler.entries.ref.ParametricReference;
import nora.compiler.entries.interfaces.Function;
import nora.compiler.entries.resolved.ResolvedFunction;
import nora.compiler.resolver.ContextResolver;

import java.util.List;

public class RawFunction extends Unresolved<Function> {
    private final Function.RecursionMode mode;
    private final ParametricReference returnType;
    private final List<RawGeneric> generics;
    private final List<RawArgument> args;
    private final Expr body;
    public RawFunction(String fullyQualifiedName, Function.RecursionMode mode, List<RawGeneric> generics, List<RawArgument> args, ParametricReference returnType, Expr body) {
        super(fullyQualifiedName);
        this.mode = mode;
        this.returnType = returnType;
        this.generics = generics;
        this.args = args;
        this.body = body;
    }

    @Override
    public Function resolve(ContextResolver resolver) {
        var argNames = args.stream().map(RawArgument::name).toList();
        var methodResolver = resolver.methodContextResolver(argNames);
        var newGenerics = generics.stream().map(g -> {
            var res = g.resolve(methodResolver);
            methodResolver.addGeneric(res);
            return res;
        }).toList();

        Instance inst = null;
        if(returnType != null)inst = returnType.resolve(methodResolver);
        var newArgs = args.stream().map(s -> s.resolve(methodResolver)).toList();
        var nBody = body.resolve(methodResolver);
        return new ResolvedFunction(getFullyQualifiedName(), mode, inst, nBody, newGenerics, newArgs);
    }

    public int numGenerics() {
        return generics.size();
    }

    @Override
    public Function asResolverProxy() {
        return new FunctionResolverProxy(this);
    }

    @Override
    public Function asFunction() {
        return asResolverProxy();
    }

    @Override
    public Callable asCallable() {
        return asResolverProxy();
    }
}
