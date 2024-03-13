package nora.compiler.entries.exprs.resolved;

import nora.compiler.entries.Instance;
import nora.compiler.entries.exprs.Expr;
import nora.compiler.resolver.bindings.Binding;
import nora.compiler.processing.TypeCheckContext;
import nora.compiler.resolver.bindings.ValBinding;

import java.io.IOException;
import java.io.OutputStreamWriter;

public class LetExpr extends BlockInlinedExpr {
    private final Binding bind;
    private final Expr val;
    private final Expr res;
    private Instance typ = null;

    public LetExpr(Binding bind, Expr val, Expr res) {
        this.bind = bind;
        this.val = val;
        this.res = res;
    }

    //Todo: can we di this now??
    //      if we get an InferenceFailed,
    //      do inference res and set val if we have type hint

    //todo: in theory we could:
    //      if val needs hint =>
    //      add val as special mutable thing to bind
    //      then execute res
    //       if res gets to a val node that use the special thing
    //        it does getAndInferenceType on the val node (with current RetHint)
    //        then inplace updates the mutable binding
    //      on way back we check if the bind is resolved
    //      however the bind resolve has hard time to tell if it needs hint without a context
    //       the needsHint would need a Set<Binding> containing the unresolved or resolved bindings
    @Override
    public Instance getAndInferenceType(TypeCheckContext context) {
        if(typ == null){
            var bindT = context.withoutReturnHint(val::getAndInferenceType);
            typ = context.withBinding(bind,bindT,res::getAndInferenceType);
        }
        return typ;
    }

    @Override
    public boolean needsHints() {
        return res.needsHints();
    }

    @Override
    public int countLocals() {
        return 1 + val.countLocals() + res.countLocals();
    }

    @Override
    public void generateBlockInlinedCode(OutputStreamWriter out, int ident) throws IOException {
        if(bind instanceof ValBinding vb){
            out.append("\t".repeat(ident));
            out.append("<let> $").append(String.valueOf(vb.slot)).append(" = ");
            val.generateCode(out, ident);
            out.append(";\n");
            if(res instanceof BlockInlinedExpr bi) {
                bi.generateBlockInlinedCode(out, ident);
            } else {
                out.append("\t".repeat(ident));
                res.generateCode(out, ident);
            }
        }
    }

    @Override
    public String toString() {
        return "let "+bind+" = "+val+" in "+res;
    }
}
