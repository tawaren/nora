package nora.compiler.entries.exprs.resolved;

import java.io.IOException;
import java.io.OutputStreamWriter;

public abstract class BlockInlinedExpr extends ResolvedExpr {
    abstract void generateBlockInlinedCode(OutputStreamWriter out, int ident) throws IOException;

    @Override
    public void generateCode(OutputStreamWriter out, int ident) throws IOException {
        out.append("{\n");
        generateBlockInlinedCode(out, ident+1);
        out.append("\n").append("\t".repeat(ident)).append("}");
    }

}
