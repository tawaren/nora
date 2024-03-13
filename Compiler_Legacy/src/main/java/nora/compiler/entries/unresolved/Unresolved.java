package nora.compiler.entries.unresolved;

import nora.compiler.entries.Definition;
import nora.compiler.entries.interfaces.*;
import nora.compiler.entries.interfaces.Module;
import nora.compiler.resolver.ContextResolver;

import java.nio.file.Path;

public abstract class Unresolved<T extends Definition> implements Definition {
    private final String fullyQualifiedName;

    protected Unresolved(String fullyQualifiedName) {
        this.fullyQualifiedName = fullyQualifiedName;
    }

    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    public abstract Definition asResolverProxy();

    @Override
    public boolean validateAndInfer() {
        throw new RuntimeException("Can only validate resolved");
    }

    @Override
    public boolean isResolved() {
        return false;
    }

    @Override
    public Kind getKind() {
        return null;
    }

    @Override
    public Module asModule() {
        return null;
    }

    @Override
    public Data asData() {
        return null;
    }

    @Override
    public MultiMethod asMultiMethod() {
        return null;
    }

    @Override
    public Trait asTrait() {
        return null;
    }

    @Override
    public Function asFunction() {
        return null;
    }

    @Override
    public Callable asCallable() { return null; }

    @Override
    public Parametric asParametric() {
        return null;
    }

    @Override
    public WithTraits asWithTraits() { return null;  }

    @Override
    public WithArguments asWithArguments() {
        return null;
    }

    @Override
    public abstract T resolve(ContextResolver resolver);

    @Override
    public void generateCode(Path buildRoot) {
        throw new RuntimeException("Only allowed on Resolved files");
    }
}
