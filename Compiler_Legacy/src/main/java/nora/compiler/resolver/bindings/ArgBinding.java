package nora.compiler.resolver.bindings;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Objects;

public class ArgBinding implements Binding{
    private final int arg;
    public ArgBinding(int arg) {
        this.arg = arg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArgBinding that = (ArgBinding) o;
        return arg == that.arg;
    }

    @Override
    public int hashCode() {
        return Objects.hash(arg, "Arg");
    }

    @Override
    public String toString() {
        return "Arg("+arg+")";
    }

    @Override
    public void generateCode(OutputStreamWriter out) throws IOException {
        out.append("!").append(String.valueOf(arg+1));
    }
}
