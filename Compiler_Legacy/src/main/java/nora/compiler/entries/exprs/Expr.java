package nora.compiler.entries.exprs;

import nora.compiler.entries.Instance;
import nora.compiler.entries.ref.Resolvable;
import nora.compiler.processing.TypeCheckContext;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

//Todo: we need a type inferenz pass
public abstract class Expr implements Resolvable<Expr> {
    public abstract Instance getAndInferenceType(TypeCheckContext context);
    public abstract boolean needsHints();
    public abstract void generateCode(OutputStreamWriter out, int ident) throws IOException;
    public abstract int countLocals();
}
