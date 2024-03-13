package nora.compiler.resolver;

import nora.compiler.entries.Instance;
import nora.compiler.entries.resolved.Generic;
import nora.compiler.entries.resolved.Variance;
import nora.compiler.resolver.bindings.*;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface ContextResolver {
    <T> T withBinding(String name, BiFunction<ContextResolver, Binding, T> body);
    Binding resolve(String name);
    <T> T withCapturingContext(List<String> argNames, Function<ContextResolver, T> body);
    List<Binding> getAllCaptures();
    GenericContextResolver methodContextResolver(List<String> argNames);
    GenericContextResolver dataContextResolver(List<Generic> rootGenerics);

    Instance getGenericBound(int arg);
    Variance getGenericVariance(int arg);

}
