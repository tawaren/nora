package nora.compiler.entries.exprs;

import nora.compiler.entries.Instance;
import nora.compiler.processing.TypeCheckContext;

import java.io.OutputStreamWriter;

public abstract class UnresolvedExpr extends Expr {
    public Instance getAndInferenceType(TypeCheckContext context){
        throw new RuntimeException("Must Resolve first");
    }

    @Override
    public boolean needsHints() {
        return false;
    }

    @Override
    public void generateCode(OutputStreamWriter out, int ident) {
        throw new RuntimeException("Only Resolved Expr can generate Code");
    }

    @Override
    public int countLocals() {
        return 0;
    }
}
