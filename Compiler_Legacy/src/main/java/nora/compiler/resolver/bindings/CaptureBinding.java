package nora.compiler.resolver.bindings;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Objects;

public class CaptureBinding implements Binding{
    private final int slot;

    public CaptureBinding(int slot) {
        this.slot = slot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CaptureBinding that = (CaptureBinding) o;
        return slot == that.slot;
    }

    @Override
    public int hashCode() {
        return Objects.hash(slot, "Capture");
    }

    @Override
    public String toString() {
        return "Capt("+slot+")";
    }

    @Override
    public void generateCode(OutputStreamWriter out) throws IOException {
        out.append("field(!0,").append(String.valueOf(slot)).append(")");
    }
}
