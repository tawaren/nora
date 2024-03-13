package nora.compiler.entries.resolved;

import nora.compiler.entries.Definition;
import nora.compiler.entries.interfaces.*;

import nora.compiler.entries.interfaces.Data;
import nora.compiler.entries.interfaces.Function;
import nora.compiler.entries.interfaces.Trait;
import nora.compiler.entries.interfaces.Module;
import nora.compiler.entries.interfaces.MultiMethod;
import nora.compiler.resolver.ContextResolver;


import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public abstract class ResolvedDefinition implements Definition {

    public final String fullyQualifiedName;

    protected ResolvedDefinition(String fullyQualifiedName) {
        this.fullyQualifiedName = fullyQualifiedName;
    }

    @Override
    public boolean isResolved() {
        return true;
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
    public Callable asCallable() { return null;}

    @Override
    public Parametric asParametric() {
        return null;
    }

    @Override
    public WithTraits asWithTraits() { return null; }

    @Override
    public WithArguments asWithArguments() {
        return null;
    }

    @Override
    public ResolvedDefinition resolve(ContextResolver resolver) {
        return this;
    }

    @Override
    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    @Override
    public void generateCode(Path buildRoot) {
        throw new RuntimeException("Not implemented for this definition");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResolvedDefinition that)) return false;
        return fullyQualifiedName.equals(that.fullyQualifiedName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullyQualifiedName);
    }

    @Override
    public String toString() {
        return fullyQualifiedName;
    }

    protected Path targetFile(Path base){
        var dirFileSplit = fullyQualifiedName.split("::");
        var dir = dirFileSplit[0].replace(".", File.separator);
        return base.resolve(Path.of(dir, dirFileSplit[1]+".nora"));
    }
}
