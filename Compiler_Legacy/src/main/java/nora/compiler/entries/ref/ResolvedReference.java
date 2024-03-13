package nora.compiler.entries.ref;

import nora.compiler.entries.Definition;
import nora.compiler.resolver.ContextResolver;

public class ResolvedReference extends Reference{
    private final Definition def;

    public ResolvedReference(Definition def) {
        super(def.getFullyQualifiedName());
        this.def = def;
    }

    @Override
    public Definition resolve(ContextResolver resolver) {
       return def;
    }
}
