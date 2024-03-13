package nora.compiler.entries;

import nora.compiler.entries.resolved.ResolvedDefinition;

public final class Root extends ResolvedDefinition {
    public Root(String fullyQualifiedName) {
        super(fullyQualifiedName);
    }

    @Override
    public Kind getKind() {
        return Kind.Root;
    }

    @Override
    public boolean validateAndInfer() {
        return true;
    }

    @Override
    public String toString() {
        return "Root";
    }
}
