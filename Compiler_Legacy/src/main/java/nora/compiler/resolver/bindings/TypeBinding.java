package nora.compiler.resolver.bindings;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Objects;

public class TypeBinding implements Binding{
    private final int arg;

    public TypeBinding(int arg) {
        this.arg = arg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeBinding that = (TypeBinding) o;
        return arg == that.arg;
    }

    @Override
    public int hashCode() {
        return Objects.hash(arg, "Type");
    }

    public int getArg() {
        return arg;
    }

    @Override
    public String toString() {
        return "Type("+arg+")";
    }

    @Override
    public void generateCode(OutputStreamWriter out) throws IOException {
        out.append("?").append(String.valueOf(arg));
    }
}
