package nora.compiler.entries.exprs.resolved;

import nora.compiler.entries.DefInstance;
import nora.compiler.entries.Instance;
import nora.compiler.entries.exprs.Expr;
import nora.compiler.entries.resolved.Argument;
import nora.compiler.processing.InferenceFailedException;
import nora.compiler.processing.TypeCollector;
import nora.compiler.resolver.bindings.Binding;
import nora.compiler.processing.TypeCheckContext;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class LambdaExpr extends ResolvedExpr {
    private final List<Binding> captures;
    private final List<Argument> args;
    private List<Instance> captureT = null;

    private final Expr body;
    private Instance typ = null;
    private int locals;

    public LambdaExpr(List<Binding> captures, List<Argument> args, Expr body) {
        this.captures = captures;
        this.args = new ArrayList<>(args); //Make sure its mutable
        this.body = body;
    }

    @Override
    public Instance getAndInferenceType(TypeCheckContext context) {
        if(typ == null) {
            captureT = captures.stream().map(context::getBinding).toList();
            var retHint = context.getRetHint();
            if(retHint instanceof DefInstance di && TypeCheckContext.isFunction(di.getBase())){
                var sig = di.getArguments();

                for(int i = 0; i < args.size(); i++){
                    var arg = args.get(i);
                    if(arg.typ() == null) args.set(i,new Argument(arg.name(),sig.get(i)));
                }

                var closureRet = sig.get(sig.size()-1);
                var bodyT = context.inClosure(captures, args, ctx -> ctx.withReturnHint(closureRet, body::getAndInferenceType));
                if(closureRet == null){
                    var nSig = new LinkedList<>(sig);
                    nSig.removeLast();
                    typ = context.getFunctionType(nSig, bodyT);
                } else {
                    typ = retHint;
                }
            } else {
                if(args.stream().anyMatch(a -> a.typ() == null)) throw new InferenceFailedException();
                var bodyT = context.inClosure(captures, args, body::getAndInferenceType);
                typ = context.getFunctionType(args.stream().map(Argument::typ).toList(), bodyT);
            }
        }
        return typ;
    }

    @Override
    public boolean needsHints() {
        for(Argument arg: args) {
            if(arg.typ() == null) return true;
        }
        return body.needsHints();
    }

    @Override
    public int countLocals() {
        locals = body.countLocals();
        return 0;
    }

    @Override
    public void generateCode(OutputStreamWriter out, int ident) throws IOException {
        out.append("<closure>[");
        for (Iterator<Instance> it = captureT.iterator(); it.hasNext();) {
            var typRes = TypeCollector.get().resolveTypeString(typ, true);
            out.append(typRes);
            if (it.hasNext()) out.append(",");
        }
        out.append("](");
        for (Iterator<Binding> it = captures.iterator(); it.hasNext();) {
            it.next().generateCode(out);
            if (it.hasNext()) out.append(",");
        }
        out.append(")[");
        var typRes = TypeCollector.get().resolveTypeString(typ, true);
        out.append(typRes).append("]{");
        out.append(String.valueOf(locals)).append("} ");
        body.generateCode(out, ident);
    }

    @Override
    public String toString() {
        return "lambda("+String.join(", ", captures.stream().map(Object::toString).toList())+")("+String.join(", ", args.stream().map(Object::toString).toList())+") -> "+body;
    }
}
