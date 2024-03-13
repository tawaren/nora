package nora.compiler.entries.exprs.resolved;

import nora.compiler.entries.DefInstance;
import nora.compiler.entries.Instance;
import nora.compiler.entries.exprs.Expr;
import nora.compiler.processing.TypeCheckContext;
import nora.compiler.processing.TypeMismatchException;
import nora.compiler.resolver.ContextResolver;

import java.io.IOException;
import java.io.OutputStreamWriter;

public class TypeHintExpr extends ResolvedExpr {
    private final Expr val;
    private Instance typeHint;
    private Instance typ = null;

    public TypeHintExpr(Expr val, Instance typeHint) {
        this.val = val;
        this.typeHint = typeHint;
    }

    public Instance getAndInferenceType(TypeCheckContext context) {
        if(typ == null) {
            //Structural exception
            if (!typeHint.isData()) throw new RuntimeException("Expected a Type");
            var curHint = context.getRetHint();
            if (curHint instanceof DefInstance ri) {
                if (typeHint.isData() && typeHint instanceof DefInstance di && di.getArguments() == null) {
                    typeHint = new DefInstance(di.getBase(), ri.getArguments());
                }
            }
            typ = context.withReturnHint(typeHint, val::getAndInferenceType);
            if(!typ.fulfills(typeHint)) throw new TypeMismatchException();
        }
        return typ;
    }

    @Override
    public boolean needsHints() {
        return typeHint.isData() && typeHint instanceof DefInstance di && di.getArguments() == null;
    }

    @Override
    public int countLocals() {
        return val.countLocals();
    }

    @Override
    public void generateCode(OutputStreamWriter out, int ident) throws IOException {
        val.generateCode(out, ident);
    }

    @Override
    public String toString() {
        return val+":"+typeHint;
    }
}
