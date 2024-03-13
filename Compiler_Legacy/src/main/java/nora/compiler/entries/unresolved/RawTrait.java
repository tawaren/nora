package nora.compiler.entries.unresolved;

import nora.compiler.entries.proxies.TraitResolverProxy;
import nora.compiler.entries.ref.ParametricReference;
import nora.compiler.entries.interfaces.Trait;
import nora.compiler.entries.resolved.Generic;
import nora.compiler.entries.resolved.ResolvedTrait;
import nora.compiler.resolver.ContextResolver;

import java.util.List;
import java.util.stream.Collectors;

public class RawTrait extends Unresolved<Trait> {
    private final List<ParametricReference> traits;
    private final List<RawGeneric> generics;

    public RawTrait(String fullyQualifiedName, List<RawGeneric> generics, List<ParametricReference> traits) {
        super(fullyQualifiedName);
        this.generics = generics;
        this.traits = traits;
    }

    @Override
    public Trait resolve(ContextResolver resolver) {
        var nTraits = traits.stream().map(m -> m.resolve(resolver)).collect(Collectors.toSet());
        List<Generic> nGeneric = generics.stream().map(g -> g.resolve(resolver)).toList();
        return new ResolvedTrait(getFullyQualifiedName(), nGeneric, nTraits);
    }

    public int numGenerics() {
        return generics.size();
    }

    @Override
    public Trait asResolverProxy() {
        return new TraitResolverProxy(this);
    }

    @Override
    public Trait asTrait() {
        return asResolverProxy();
    }
}
