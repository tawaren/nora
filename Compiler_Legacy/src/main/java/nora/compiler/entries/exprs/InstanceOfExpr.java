package nora.compiler.entries.exprs;

import nora.compiler.entries.Instance;
import nora.compiler.entries.exprs.resolved.BindingExpr;
import nora.compiler.entries.exprs.resolved.InstanceExpr;
import nora.compiler.processing.TypeMismatchException;
import nora.compiler.resolver.ContextResolver;
import nora.compiler.resolver.bindings.Binding;
import nora.compiler.processing.TypeCheckContext;
import nora.compiler.resolver.bindings.TypeBinding;

import java.io.IOException;
import java.io.OutputStreamWriter;

public class InstanceOfExpr extends Expr {
    private final Expr typ;
    private final Expr target;
    private boolean isTyped = false;

    public InstanceOfExpr(Expr typ, Expr target) {
        this.typ = typ;
        this.target = target;
    }

    @Override
    public Expr resolve(ContextResolver resolver) {
        var nTyp = typ.resolve(resolver);
        var nTarget = target.resolve(resolver);
        return new InstanceOfExpr(nTyp, nTarget);
    }

    //Returns null if not const
    public Instance constType(TypeCheckContext context){
        if(typ instanceof InstanceExpr ie){
            return ie.constType();
        } else if(typ instanceof BindingExpr be){
            return be.constType(context);
        } else {
            return null;
        }
    }

    public Binding constBinding(){
        if(target instanceof BindingExpr be){
            return be.getBinding();
        } else {
            return null;
        }
    }

    @Override
    public Instance getAndInferenceType(TypeCheckContext context) {
        if(!isTyped){
            Instance t;
            Instance ct;
            Instance srcT;
            if(target.needsHints()){
                t = context.withReturnHint(context.getTypeType(), typ::getAndInferenceType);
                ct = constType(context);
                srcT = context.withReturnHint(ct, target::getAndInferenceType);
            } else {
                srcT = context.withoutReturnHint(target::getAndInferenceType);
                t = context.withReturnHint(context.getTypeHintType(srcT), typ::getAndInferenceType);
                ct = constType(context);
            }
            //Todo: structural Exception
            if(ct == null) throw new RuntimeException("Expected a constant type");
            if(!t.equals(context.getTypeType())) throw new TypeMismatchException();
            //Types must be compatible
            //todo: later we need a type compatability check considering variance on subtypes
            if(!srcT.subType(ct) && !ct.subType(srcT)) throw new TypeMismatchException();
            isTyped = true;
        }
        return context.getBoolType();
    }

    @Override
    public boolean needsHints() {
        return false;
    }

    @Override
    public int countLocals() {
        return typ.countLocals() + target.countLocals();
    }

    @Override
    public void generateCode(OutputStreamWriter out, int ident) throws IOException {
        out.append("<subTypeOf>(");
        target.generateCode(out, ident);
        out.append(",");
        typ.generateCode(out, ident);
        out.append(")");
    }

    @Override
    public String toString() {
        return target + " is "+ typ;
    }
}
