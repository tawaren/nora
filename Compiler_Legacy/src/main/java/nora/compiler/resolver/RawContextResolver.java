package nora.compiler.resolver;

import nora.compiler.entries.Instance;
import nora.compiler.entries.resolved.Generic;
import nora.compiler.entries.resolved.Variance;
import nora.compiler.resolver.bindings.Binding;
import nora.compiler.resolver.bindings.DefBinding;

import java.util.List;

public class RawContextResolver extends AbstractContextResolver {
    private final ImportResolver imports;

    public RawContextResolver(ImportResolver imports) {
        this.imports = imports;
    }

    @Override
    public Binding resolve(String name) {
        return new DefBinding(imports.resolveReference(name));
    }

    @Override
    public GenericContextResolver methodContextResolver(List<String> argNames) {
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
        throw new RuntimeException("No generics bound in Raw context");
    }

    @Override
    public Variance getGenericVariance(int arg) {
        throw new RuntimeException("No generics bound in Raw context");
    }
}