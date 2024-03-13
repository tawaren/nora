package nora.compiler.resolver.bindings;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Objects;

public class ValBinding implements Binding{
    public final int slot;

    public ValBinding(int slot) {
        this.slot = slot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValBinding that = (ValBinding) o;
        return slot == that.slot;
    }

    @Override
    public int hashCode() {
        return Objects.hash(slot, "Val");
    }

    @Override
    public String toString() {
        return "Val("+slot+")";
    }

    @Override
    public void generateCode(OutputStreamWriter out) throws IOException {
        out.append("$").append(String.valueOf(slot));
    }
}
