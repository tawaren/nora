package nora.compiler.entries.exprs;

import nora.compiler.entries.DefInstance;
import nora.compiler.entries.Instance;
import nora.compiler.entries.resolved.Argument;
import nora.compiler.processing.TypeCollector;
import nora.compiler.resolver.ContextResolver;
import nora.compiler.processing.TypeCheckContext;

import java.io.IOException;
import java.io.OutputStreamWriter;

public class FieldExpr extends Expr {
    private final String field;
    private final Expr target;
    private Instance receiverType = null;
    private Instance typ = null;
    private int fieldIndex;

    public FieldExpr(String field, Expr target) {
        this.field = field;
        this.target = target;
    }

    //Todo: Shall we resolve field name to index or is this to early???
    @Override
    public Expr resolve(ContextResolver resolver) {
        return new FieldExpr(field, target.resolve(resolver));
    }

    @Override
    public Instance getAndInferenceType(TypeCheckContext context) {
        if(typ == null) {
            receiverType = context.withoutReturnHint(target::getAndInferenceType);
            if(receiverType.isData() && receiverType instanceof DefInstance di){
                var data = di.getBase().asData();
                if(TypeCheckContext.isTuple(data)){
                    fieldIndex =  TypeCheckContext.getTupleFieldIndex(field);
                    typ = di.getArguments().get(fieldIndex);
                } else {
                    typ = data.getFieldByName(field).typ().substitute(di.getArguments());
                    fieldIndex = data.getFields().stream().map(Argument::name).toList().indexOf(field);
                }
            } else {
                //Todo: Structural exception
                throw new RuntimeException("Only Data types can have field access");
            }
        }
        return typ;
    }

    @Override
    public boolean needsHints() {
        return false;
    }

    @Override
    public int countLocals() {
        return target.countLocals();
    }

    @Override
    public void generateCode(OutputStreamWriter out, int ident) throws IOException {
        out.append("<field>#");
        out.append(TypeCollector.get().resolveEntityString(receiverType));
        out.append("(");
        target.generateCode(out, ident);
        out.append(",").append(String.valueOf(fieldIndex)).append(")");
    }

    @Override
    public String toString() {
        return target+"->"+field;
    }
}
