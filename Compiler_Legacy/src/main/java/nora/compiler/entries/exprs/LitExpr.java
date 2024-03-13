package nora.compiler.entries.exprs;

import nora.compiler.entries.Instance;
import nora.compiler.resolver.ContextResolver;
import nora.compiler.processing.TypeCheckContext;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigInteger;

public class LitExpr extends Expr {
    private Object val;
    private Instance typ = null;

    public LitExpr(Object val) {
        this.val = val;
    }

    @Override
    public Expr resolve(ContextResolver resolver) {
        return this;
    }

    public Instance getAndInferenceType(TypeCheckContext context) {
        if(typ != null) return typ;
        if(val instanceof Integer i){
            if(context.getRetHint() != null && context.getRetHint().equals(context.getNumType())){
                val = Long.valueOf(i);
                typ = context.getNumType();
            } else {
                typ = context.getIntType();
            }
        }else if(val instanceof Long){
            typ = context.getNumType();
        } else if(val instanceof BigInteger) {
            typ  = context.getNumType();
        } else if(val instanceof Boolean) {
            typ = context.getBoolType();
        } else if(val instanceof String){
            typ = context.getStringType();
        } else {
            //Todo: Structural Exception
            throw new RuntimeException("Illegal Literal Type");
        }
        return typ;
    }

    @Override
    public boolean needsHints() {
        return val instanceof Integer;
    }

    @Override
    public int countLocals() {
        return 0;
    }

    @Override
    public void generateCode(OutputStreamWriter out, int ident) throws IOException {
        if(typ == TypeCheckContext.get().getIntType() || val instanceof Integer) {
            out.append("<int>").append(val.toString());
        } else if(typ == TypeCheckContext.get().getNumType() || val instanceof Long || val instanceof BigInteger) {
            out.append("<num>").append(val.toString());
        } else if(val instanceof Boolean){
            out.append("<").append(val.toString()).append(">");
        } else if(val instanceof String){
            out.append("\"").append(val.toString()).append("\"");
        } else {
            throw new RuntimeException("Illegal Literal Type");
        }
    }

    @Override
    public String toString() {
        return val.toString();
    }
}
