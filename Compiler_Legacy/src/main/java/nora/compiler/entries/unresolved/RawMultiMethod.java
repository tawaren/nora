package nora.compiler.entries.unresolved;

import nora.compiler.entries.interfaces.Callable;
import nora.compiler.entries.proxies.MultiMethodResolverProxy;
import nora.compiler.entries.ref.ParametricReference;
import nora.compiler.entries.resolved.Argument;
import nora.compiler.entries.resolved.Generic;
import nora.compiler.entries.resolved.ResolvedCaseMethod;
import nora.compiler.entries.interfaces.MultiMethod;
import nora.compiler.entries.resolved.ResolvedMultiMethod;
import nora.compiler.resolver.ContextResolver;

import java.util.LinkedList;
import java.util.List;

public class RawMultiMethod extends Unresolved<MultiMethod> {

    private final boolean sealed;
    private final boolean partial;
    private final ParametricReference returnType;
    private final List<RawGeneric> generics;
    private final List<RawArgument> dynamicArgs;
    private final List<RawArgument> staticArgs;
    private final List<ResolvedCaseMethod> caseMethods = new LinkedList<>();

    public RawMultiMethod(String fullyQualifiedName, boolean sealed, boolean partial, List<RawGeneric> generics, List<RawArgument> dynamicArgs,List<RawArgument> staticArgs, ParametricReference returnType) {
        super(fullyQualifiedName);
        this.sealed = sealed;
        this.partial = partial;
        this.dynamicArgs = dynamicArgs;
        this.generics = generics;
        this.staticArgs = staticArgs;
        this.returnType = returnType;
    }

    public boolean isSealed() {
        return sealed;
    }

    public boolean isPartial() {
        return partial;
    }

    public void addCaseMethod(ResolvedCaseMethod caseM){
        caseMethods.add(caseM);
    }


    @Override
    public MultiMethod resolve(ContextResolver resolver) {
        var argNames = new LinkedList<String>();
        argNames.addAll(dynamicArgs.stream().map(RawArgument::name).toList());
        argNames.addAll(staticArgs.stream().map(RawArgument::name).toList());
        var methodContextResolver = resolver.methodContextResolver(argNames);
        var newGenerics = generics.stream().map(g -> {
            var res = g.resolve(methodContextResolver);
            methodContextResolver.addGeneric(res);
            return res;
        }).toList();
        var inst = returnType.resolve(methodContextResolver);
        var newDynamicArgs = dynamicArgs.stream().map(d -> d.resolve(methodContextResolver)).toList();
        var newStaticArgs = staticArgs.stream().map(s -> s.resolve(methodContextResolver)).toList();
        return new ResolvedMultiMethod(getFullyQualifiedName(), sealed, partial, inst, newGenerics, newDynamicArgs, newStaticArgs, caseMethods);
    }

    public int numGenerics() {
        return generics.size();
    }

    @Override
    public MultiMethod asResolverProxy() {
        return new MultiMethodResolverProxy(this);
    }

    @Override
    public MultiMethod asMultiMethod() {
        return asResolverProxy();
    }

    @Override
    public Callable asCallable() {
        return asResolverProxy();
    }
}
