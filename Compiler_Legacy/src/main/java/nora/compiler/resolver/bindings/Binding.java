package nora.compiler.resolver.bindings;

import java.io.IOException;
import java.io.OutputStreamWriter;

public interface Binding {
    void generateCode(OutputStreamWriter out) throws IOException;
}
