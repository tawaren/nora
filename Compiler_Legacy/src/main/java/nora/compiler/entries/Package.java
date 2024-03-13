package nora.compiler.entries;

import nora.compiler.entries.resolved.ResolvedDefinition;

public final class Package extends ResolvedDefinition {
    public Package(String fullyQualifiedName) {
        super(fullyQualifiedName);
    }

    @Override
    public Kind getKind() {
        return Kind.Package;
    }

    @Override
    public boolean validateAndInfer() {
        return true;
    }

    @Override
    public String toString() {
        return "Package";
    }
}
