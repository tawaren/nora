package nora.compiler.entries.exprs;

import nora.compiler.entries.DefInstance;
import nora.compiler.entries.Instance;
import nora.compiler.entries.resolved.Argument;
import nora.compiler.processing.InferenceFailedException;
import nora.compiler.processing.TypeMismatchException;
import nora.compiler.resolver.ContextResolver;
import nora.compiler.processing.TypeCheckContext;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class CallExpr extends Expr {
    private final Expr target;
    private final List<Expr> args;
    private final boolean isTailSelfCall;
    private Instance typ = null;

    public CallExpr(Expr target, List<Expr> args, boolean isTailSelfCall) {
        this.target = target;
        this.args = args;
        this.isTailSelfCall = isTailSelfCall;
    }

    public CallExpr(Expr target, List<Expr> args) {
        this(target,args, false);
    }


    @Override
    public Expr resolve(ContextResolver resolver) {
        var nTarget = target.resolve(resolver);
        var nArgs = args.stream().map(a -> a.resolve(resolver)).toList();
        return new CallExpr(nTarget, nArgs, isTailSelfCall);
    }

    @Override
    public Instance getAndInferenceType(TypeCheckContext context) {
        if(typ == null){
            var respectHints = true;
            var madeProgress = true;
            Instance[] argTs = new Instance[args.size()];
            Instance funT = null;
            while (funT == null & madeProgress){
                madeProgress = false;
                //evaluation order depends on hint requirements who needs it more, the arguments or the target
                for(int i = 0; i < argTs.length; i++){
                    var e = args.get(i);
                    if(argTs[i] == null && (!e.needsHints() || !respectHints)){
                        try {
                            argTs[i] = context.withoutReturnHint(e::getAndInferenceType);
                            madeProgress = true;
                            //lets try the function again after this success
                            if(!respectHints) break;
                        } catch (InferenceFailedException ignored){ }
                    }
                }
                try {
                    funT = context.withReturnHint(context.getFunctionType(Arrays.stream(argTs).toList(), context.getRetHint()), target::getAndInferenceType);
                } catch (InferenceFailedException ignored){ }
                //if we did not get their with respecting hints, more iterations will not help
                // but ignoring hints can be seen as progress
                if(respectHints) madeProgress = true;
                respectHints = false;
            }
            if(funT == null) throw new InferenceFailedException();
            List<Instance> allArgs;
            if(funT.isCallable() && funT instanceof DefInstance di){
                var base = di.getBase().asCallable();
                var applies = di.getArguments();
                allArgs = base.getArgs().stream().map(Argument::typ).map(t -> t.substitute(applies)).toList();
                typ = base.getReturnType().substitute(applies);
            } else if(funT.isData() && funT instanceof DefInstance di) {
                //it could be a function data object
                if(TypeCheckContext.isFunction(di.getBase())){
                    var applies = di.getArguments();
                    allArgs = applies.subList(0, applies.size()-1);
                    typ = applies.get(applies.size()-1);
                } else {
                    throw new TypeMismatchException();
                    //throw new RuntimeException("Only functions can be called");
                }
            } else {
                throw new TypeMismatchException();
                //throw new RuntimeException("Only functions can be called");
            }
            for(int i = 0; i < argTs.length; i++){
                if(argTs[i] == null){
                    var e = args.get(i);
                    argTs[i] = context.withReturnHint(allArgs.get(i),e::getAndInferenceType);
                }
            }
            //Now do the type check
            if(argTs.length != allArgs.size()) throw new TypeMismatchException();
            int i = 0;
            for(Instance argT:argTs){
                var paramT = allArgs.get(i++);
                if(!argT.subType(paramT))  throw new TypeMismatchException();
            }
        }
        return typ;
    }

    @Override
    public boolean needsHints() {
        return target.needsHints();
    }

    @Override
    public int countLocals() {
        return target.countLocals() + args.stream().mapToInt(Expr::countLocals).sum();
    }

    @Override
    public void generateCode(OutputStreamWriter out, int ident) throws IOException {
        if(isTailSelfCall){
            out.append("<tail-rec>");
        } else {
            out.append("<call> ");
            target.generateCode(out,ident);
        }
        out.append("(");
        for (Iterator<Expr> it = args.iterator(); it.hasNext();) {
            it.next().generateCode(out, ident);
            if (it.hasNext()) out.append(",");
        }
        out.append(")");
    }

    @Override
    public String toString() {
        return target+"("+String.join(", ", args.stream().map(Object::toString).toList())+")";
    }
}
