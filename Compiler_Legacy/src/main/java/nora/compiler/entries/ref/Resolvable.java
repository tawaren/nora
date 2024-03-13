package nora.compiler.entries.ref;

import nora.compiler.resolver.ContextResolver;

public interface Resolvable<T> {
    T resolve(ContextResolver resolver);
}
