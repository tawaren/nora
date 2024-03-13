package nora.compiler.entries.unresolved;

import nora.compiler.entries.Instance;
import nora.compiler.entries.ref.ParametricReference;
import nora.compiler.entries.ref.Resolvable;
import nora.compiler.entries.resolved.Argument;
import nora.compiler.resolver.ContextResolver;

public record RawArgument(String name, ParametricReference typ) implements Resolvable<Argument> {
    @Override
    public Argument resolve(ContextResolver resolver) {
        Instance nTyp = null;
        if(typ != null) nTyp = typ.resolve(resolver);
        return new Argument(name, nTyp);
    }
}
