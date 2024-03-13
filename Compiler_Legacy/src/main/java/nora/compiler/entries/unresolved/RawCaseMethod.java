package nora.compiler.entries.unresolved;

import nora.compiler.entries.Definition;
import nora.compiler.entries.Instance;
import nora.compiler.entries.exprs.Expr;
import nora.compiler.entries.interfaces.Callable;
import nora.compiler.entries.interfaces.Function;
import nora.compiler.entries.proxies.CaseMethodResolverProxy;
import nora.compiler.entries.ref.ParametricReference;
import nora.compiler.entries.ref.Reference;
import nora.compiler.entries.resolved.ResolvedCaseMethod;
import nora.compiler.resolver.ContextResolver;

import java.util.LinkedList;
import java.util.List;

public class RawCaseMethod extends Unresolved<ResolvedCaseMethod> {

    private final Function.RecursionMode  mode;
    private final Reference before;
    private final Reference after;

    private final ParametricReference multiMethod;

    private final ParametricReference returnType;
    private final Expr body;
    private final List<RawGeneric> generics;
    private final List<RawArgument> dynamicArgs;
    private final List<RawArgument> staticArgs;

    public RawCaseMethod(String fullyQualifiedName, Function.RecursionMode mode, Reference before, Reference after, List<RawGeneric> generics, ParametricReference multiMethod, List<RawArgument> dyn, List<RawArgument> stat, ParametricReference returnType, Expr body) {
        super(fullyQualifiedName);
        this.mode = mode;
        this.before = before;
        this.after = after;
        this.multiMethod = multiMethod;
        this.returnType = returnType;
        this.body = body;
        this.generics = generics;
        this.dynamicArgs = dyn;
        this.staticArgs = stat;
     }

    @Override
    public ResolvedCaseMethod resolve(ContextResolver resolver) {
        Definition nBefore = null;
        if(before != null) nBefore = before.resolve(resolver);
        Definition nAfter = null;
        if(after != null) nAfter = after.resolve(resolver);

        var argNames = new LinkedList<String>();
        argNames.addAll(dynamicArgs.stream().map(RawArgument::name).toList());
        argNames.addAll(staticArgs.stream().map(RawArgument::name).toList());
        var methodResolver = resolver.methodContextResolver(argNames);
        var nGenerics = generics.stream().map(g -> {
            var res = g.resolve(methodResolver);
            methodResolver.addGeneric(res);
            return res;
        }).toList();

        Instance nMulti = null;
        if(multiMethod != null) nMulti = multiMethod.resolve(methodResolver);
        Instance nRet = null;
        if(returnType != null) nRet = returnType.resolve(methodResolver);
        var nDynArgs = dynamicArgs.stream().map(d -> d.resolve(methodResolver)).toList();
        var nStaticArgs = staticArgs.stream().map(s -> s.resolve(methodResolver)).toList();
        var nBody = body.resolve(methodResolver);

        return new ResolvedCaseMethod(getFullyQualifiedName(), mode, nBefore, nAfter, nMulti, nDynArgs, nStaticArgs, nRet, nBody, nGenerics);
    }

    public int numGenerics() {
        return generics.size();
    }

    @Override
    public CaseMethodResolverProxy asResolverProxy() {
        return new CaseMethodResolverProxy(this);
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
