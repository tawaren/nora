package nora.compiler.resolver.bindings;

import nora.compiler.entries.Definition;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Objects;

public class DefBinding implements Binding{
    private final Definition def;

    public DefBinding(Definition def) {
        this.def = def;
    }

    public Definition getDef() {
        return def;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefBinding that = (DefBinding) o;
        return def.equals(that.def);
    }

    @Override
    public int hashCode() {
        return Objects.hash(def, "Definition");
    }

    @Override
    public String toString() {
        return "Def("+def+")";
    }

    @Override
    public void generateCode(OutputStreamWriter out) throws IOException {
        throw new RuntimeException("Should be handled by Instance Expr");
    }
}
