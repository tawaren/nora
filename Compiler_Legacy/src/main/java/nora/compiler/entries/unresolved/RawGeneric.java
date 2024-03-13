package nora.compiler.entries.unresolved;

import nora.compiler.entries.Instance;
import nora.compiler.entries.resolved.Variance;
import nora.compiler.entries.ref.ParametricReference;
import nora.compiler.entries.ref.Resolvable;
import nora.compiler.entries.resolved.Generic;
import nora.compiler.resolver.ContextResolver;

public record RawGeneric(String name, Variance variance, ParametricReference typ, ParametricReference hint) implements Resolvable<Generic> {
    @Override
    public Generic resolve(ContextResolver resolver) {
        Instance bound = null;
        if(typ != null) bound = typ.resolve(resolver);
        Instance nHint = null;
        if(hint != null) nHint = hint.resolve(resolver);
        return new Generic(name, variance, bound, nHint);
    }
}
