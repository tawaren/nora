package nora.compiler.entries.exprs;

import nora.compiler.entries.Instance;
import nora.compiler.processing.InferenceFailedException;
import nora.compiler.processing.TypeCollector;
import nora.compiler.processing.TypeMismatchException;
import nora.compiler.resolver.ContextResolver;
import nora.compiler.processing.TypeCheckContext;

import java.io.IOException;
import java.io.OutputStreamWriter;

public class IfExpr extends Expr {
    private final Expr cond;
    private final Expr then;
    private final Expr other;
    private Instance typ = null;

    public IfExpr(Expr cond, Expr then, Expr other) {
        this.cond = cond;
        this.then = then;
        this.other = other;
    }

    @Override
    public Expr resolve(ContextResolver resolver) {
        var nCond = cond.resolve(resolver);
        var nThen = then.resolve(resolver);
        var nOther = other.resolve(resolver);
        return new IfExpr(nCond,nThen, nOther);
    }

    @Override
    public Instance getAndInferenceType(TypeCheckContext context) {
        if(typ == null) {
            var c = context.withReturnHint(context.getBoolType(), cond::getAndInferenceType);
            if(!c.equals(context.getBoolType())) throw new TypeMismatchException();
            Instance thenT = null;
            Instance elseT = null;
            if(!other.needsHints()) {
                elseT = other.getAndInferenceType(context);
            }
            //We do some flow typing to make it more convinient for the programmer
            //  Further this means we do not need casts (these are unsave anyway)
            if(cond instanceof InstanceOfExpr ioe){
                var ct = ioe.constType(context);
                var cv = ioe.constBinding();
                //Flow typing oppertunity detected
                if(ct != null && cv != null && !context.getBinding(cv).subType(ct)) {
                    if(elseT != null && context.getRetHint() == null && then.needsHints()){
                        thenT = context.withReturnHint(elseT, ctx -> ctx.withBinding(cv,ct, true, then::getAndInferenceType));
                    } else {
                        try {
                            thenT = context.withBinding(cv,ct, true, then::getAndInferenceType);
                        } catch (InferenceFailedException ife){
                            elseT = other.getAndInferenceType(context);
                            thenT = context.withReturnHint(elseT, ctx -> ctx.withBinding(cv,ct, true, then::getAndInferenceType));
                        }
                    }
                }
            }

            if(thenT == null){
                if(elseT != null && context.getRetHint() == null && then.needsHints()){
                    thenT = context.withReturnHint(elseT, then::getAndInferenceType);
                } else {
                    try {
                        thenT = then.getAndInferenceType(context);
                    } catch (InferenceFailedException ife){
                        elseT = other.getAndInferenceType(context);
                        thenT = context.withReturnHint(elseT, then::getAndInferenceType);
                    }
                }
            }

            if (elseT == null) {
                if(context.getRetHint() == null && other.needsHints()){
                    elseT = context.withReturnHint(thenT, other::getAndInferenceType);
                } else {
                    try {
                        elseT = other.getAndInferenceType(context);
                    } catch (InferenceFailedException ife){
                        thenT = then.getAndInferenceType(context);
                        elseT = context.withReturnHint(thenT, other::getAndInferenceType);
                    }
                }

            }

            var allThenT = Instance.collectDataHierarchy(thenT);
            var allElseT = Instance.collectDataHierarchy(elseT);
            for(Instance inst:allThenT) {
                if(allElseT.contains(inst)) {
                    typ = inst;
                    break;
                }
            }
            if(typ == null) throw new TypeMismatchException();
        }
        return typ;
    }

    @Override
    public boolean needsHints() {
        return then.needsHints() && other.needsHints();
    }

    @Override
    public int countLocals() {
        return cond.countLocals() + then.countLocals() + other.countLocals();
    }

    @Override
    public void generateCode(OutputStreamWriter out, int ident) throws IOException {
        out.append("<if>[");
        out.append(TypeCollector.get().resolveTypeString(typ, true));
        out.append("] ");
        cond.generateCode(out, ident+1);
        out.append("\n").append("\t".repeat(ident)).append("<then> ");
        then.generateCode(out, ident+1);
        out.append("\n").append("\t".repeat(ident)).append("<else> ");
        other.generateCode(out, ident+1);
    }

    @Override
    public String toString() {
        return "if " + cond + " then "+ then+" else "+other;
    }
}
