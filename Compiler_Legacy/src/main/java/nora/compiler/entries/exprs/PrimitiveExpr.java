package nora.compiler.entries.exprs;

import nora.compiler.entries.Instance;
import nora.compiler.processing.TypeCheckContext;
import nora.compiler.resolver.ContextResolver;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.List;

public class PrimitiveExpr extends Expr {
    private final String name;
    private final List<Expr> args;
    private Instance typ = null;

    public PrimitiveExpr(String name, List<Expr> args) {
        this.name = name;
        this.args = args;
    }

    @Override
    public Expr resolve(ContextResolver resolver) {
        //Is a special expression and comes with resolved args
        return new PrimitiveExpr(name, args);
    }

    @Override
    public Instance getAndInferenceType(TypeCheckContext context) {
        if(typ == null){
            context.withoutReturnHint(c -> args.stream().map(a -> a.getAndInferenceType(c)).toList());
            typ = context.getRetHint();
        }
        return typ;
    }

    @Override
    public boolean needsHints() {
        return false;
    }

    @Override
    public int countLocals() {
        return args.stream().mapToInt(Expr::countLocals).sum();
    }

    @Override
    public void generateCode(OutputStreamWriter out, int ident) throws IOException {
        out.append("<primitive>#").append(name).append("(");
        for (Iterator<Expr> it = args.iterator(); it.hasNext();) {
            it.next().generateCode(out, ident);
            if (it.hasNext()) out.append(",");
        }
        out.append(")");
    }

    @Override
    public String toString() {
        return "primitive#"+name+"("+String.join(", ", args.stream().map(Object::toString).toList())+")";
    }
}
