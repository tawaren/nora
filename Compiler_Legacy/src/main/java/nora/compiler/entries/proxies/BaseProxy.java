package nora.compiler.entries.proxies;

import nora.compiler.entries.Definition;
import nora.compiler.entries.interfaces.Module;
import nora.compiler.entries.ref.Resolvable;
import nora.compiler.entries.interfaces.*;
import nora.compiler.entries.unresolved.Unresolved;
import nora.compiler.resolver.ContextResolver;

import java.nio.file.Path;

public class BaseProxy<T extends Definition, R extends Unresolved<T>> implements Definition, Resolvable<Definition> {
    private R raw;
    private T res;

    public BaseProxy(R raw) {
        this.raw = raw;
    }

    protected <V> V onResolved(java.util.function.Function<T,V> f) {
        if(res == null) throw new RuntimeException("Function only avaiable on resolved");
        return f.apply(res);
    }

    protected <V> V onAny(java.util.function.Function<T,V> resolved, java.util.function.Function<R,V> unresolved) {
        if(res != null) return resolved.apply(res);
        if(raw != null) return unresolved.apply(raw);
        return null;
    }

    protected T doResolve(java.util.function.Function<R,T> resolver) {
        if(raw == null) throw new RuntimeException("Must be unresolved");
        res = resolver.apply(raw);
        return res;
    }

    @Override
    public boolean validateAndInfer() {
        if(res == null) throw new RuntimeException("Can not validate a unresolved definition");
        return res.validateAndInfer();
    }

    @Override
    public Kind getKind() {
        if(res == null) throw new RuntimeException("Can not validate a unresolved definition");
        return res.getKind();
    }

    @Override
    public Module asModule() {
        if(res == null) return null;
        return res.asModule();
    }

    @Override
    public Data asData() {
        if(res == null) return null;
        return res.asData();
    }

    @Override
    public MultiMethod asMultiMethod() {
        if(res == null) return null;
        return res.asMultiMethod();
    }

    @Override
    public Trait asTrait() {
        if(res == null) return null;
        return res.asTrait();
    }

    @Override
    public Function asFunction() {
        if(res == null) return null;
        return res.asFunction();
    }

    @Override
    public Callable asCallable() {
        if(res == null) return null;
        return res.asCallable();
    }

    @Override
    public Parametric asParametric() {
        if(res == null) return null;
        return res.asParametric();
    }

    @Override
    public WithTraits asWithTraits() {
        if(res == null) return null;
        return res.asWithTraits();
    }

    @Override
    public WithArguments asWithArguments() {
        if(res == null) return null;
        return res.asWithArguments();
    }

    @Override
    public String getFullyQualifiedName() {
        if(res != null) return res.getFullyQualifiedName();
        if(raw != null) return raw.getFullyQualifiedName();
        return null;
    }

    @Override
    public void generateCode(Path buildRoot) {
        if(res == null) return;
        res.generateCode(buildRoot);
    }

    @Override
    public boolean isResolved() {
        return res != null;
    }

    @Override
    public T resolve(ContextResolver resolver) {
        if(raw != null) res = raw.resolve(resolver);
        raw = null;
        return res;
    }

    @Override
    public String toString() {
        if(res != null) return res.toString();
        return raw.toString();
    }
}
