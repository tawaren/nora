package nora.compiler.entries.proxies;

import nora.compiler.entries.Instance;
import nora.compiler.entries.interfaces.Parametric;
import nora.compiler.entries.interfaces.Trait;
import nora.compiler.entries.interfaces.WithTraits;
import nora.compiler.entries.resolved.Generic;
import nora.compiler.entries.resolved.Variance;
import nora.compiler.entries.unresolved.RawTrait;

import java.util.List;
import java.util.Set;

public class TraitResolverProxy extends BaseProxy<Trait, RawTrait> implements nora.compiler.entries.interfaces.Trait {

    public TraitResolverProxy(RawTrait rawTrait) {
        super(rawTrait);
    }

    @Override
    public Set<Instance> getTraits() {
        return onResolved(Trait::getTraits);
    }

    @Override
    public List<Generic> getGenerics(){
        return onResolved(Trait::getGenerics);
    }

    @Override
    public int numGenerics() {
        return onAny(Trait::numGenerics, RawTrait::numGenerics);
    }

    @Override
    public boolean hasTrait(Trait trait) {
        return onResolved(t -> t.hasTrait(trait));
    }

    @Override
    public boolean validateApplication(List<Instance> arguments, Variance position) {
        return onResolved(r -> r.validateApplication(arguments, position));
    }

    @Override
    public Trait asTrait() {
        var res = super.asTrait();
        if(res == null) return this;
        return res;
    }

    @Override
    public Parametric asParametric() {
        var res = super.asParametric();
        if(res == null) return this;
        return res;
    }

    @Override
    public WithTraits asWithTraits() {
        var res = super.asWithTraits();
        if(res == null) return this;
        return res;
    }
}
