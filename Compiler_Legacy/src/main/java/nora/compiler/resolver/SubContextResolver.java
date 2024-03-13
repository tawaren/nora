package nora.compiler.resolver;

import nora.compiler.entries.Instance;
import nora.compiler.entries.resolved.Generic;
import nora.compiler.entries.resolved.Variance;
import nora.compiler.resolver.bindings.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

public class SubContextResolver extends AbstractContextResolver implements GenericContextResolver {
    private final ImportResolver imports;
    private final List<Generic> generics = new LinkedList<>();

    SubContextResolver(List<String> argNames, ImportResolver imports) {
        super();
        int argCount = 0;
        for (String arg : argNames) {
            var binds = bindings.computeIfAbsent(arg, n -> new Stack<>());
            binds.push(new ArgBinding(argCount++));
        }

        this.imports = imports;
    }

    public void addGeneric(Generic g){
        var binds = bindings.computeIfAbsent(g.name(), n -> new Stack<>());
        binds.push(new TypeBinding(generics.size()));
        generics.add(g);
    }

    @Override
    public Binding resolve(String name) {
        var bind = super.resolve(name);
        if (bind != null) return bind;
        return new DefBinding(imports.resolveReference(name));
    }

    @Override
    public SubContextResolver methodContextResolver(List<String> argNames) {
        return new SubContextResolver(argNames, imports);
    }

    @Override
    public GenericContextResolver dataContextResolver(List<Generic> rootGenerics) {
        var res = new SubContextResolver(List.of(),imports);
        if(rootGenerics != null)rootGenerics.forEach(res::addGeneric);
        return res;
    }

    @Override
    public Instance getGenericBound(int arg) {
        return generics.get(arg).bound();
    }

    @Override
    public Variance getGenericVariance(int arg) {
        return generics.get(arg).variance();
    }
}