package nora.compiler.resolver;

import nora.compiler.entries.resolved.Generic;

public interface GenericContextResolver extends ContextResolver{
    void addGeneric(Generic g);
}
