package nora.compiler.entries.exprs.resolved;

import nora.compiler.entries.DefInstance;
import nora.compiler.entries.Instance;
import nora.compiler.entries.exprs.Expr;
import nora.compiler.processing.InferenceFailedException;
import nora.compiler.processing.TypeCheckContext;
import nora.compiler.processing.TypeCollector;
import nora.compiler.resolver.bindings.TypeBinding;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.List;

public class InstanceExpr extends ResolvedExpr {
    private Instance binding;
    private Instance typ = null;

    public InstanceExpr(Instance binding) {
        this.binding = binding;
    }

    public Instance constType(){
        if(binding.isData()) {
            return binding;
        } else {
            return null;
        }
    }

    public Instance getAndInferenceType(TypeCheckContext context) {
        if(typ != null) return typ;
        if(binding.isCallable() && binding instanceof DefInstance di){
            if(di.getArguments() == null){
                var hints = context.getRetHint();
                if(hints != null) {
                    binding = context.inferFunArgs(binding);
                } else if(di.getBase().asParametric().getGenerics().isEmpty()){
                    binding = new DefInstance(di.getBase(), List.of());
                } else {
                    throw new InferenceFailedException();
                }
                //We after changing it
                // Todo: make the validation exception
                //We are in the body, where variance does not matter for instantiation
                //  it will only matter for assignements but this is checked later
                if(!binding.validateAndInfer(null)) throw new RuntimeException("Invalid Binding Type");
            }
            typ = context.getTypeOfCallable(binding);
        } else if(binding.isData() && binding instanceof DefInstance di) {
            if(di.getArguments() == null){
                var retHint = context.getRetHint();
                if(retHint instanceof DefInstance ri && context.isTypeHintType(retHint)){
                    var typ = ri.getArguments().get(0);
                    binding = new DefInstance(di.getBase(), ((DefInstance)typ).getArguments());
                }
            }
            // Todo: make the validation exception
            //We are in the body, where variance does not matter for instantiation
            //  it will only matter for assignements but this is checked later
            if(!binding.validateAndInfer(null)) throw new RuntimeException("Invalid Binding Type");
            typ = context.getTypeType();
        } else {
            //Todo: Make structural Exception
            throw new RuntimeException("Illegal Literal Type");
        }
        return typ;
    }

    @Override
    public boolean needsHints() {
        if(binding instanceof DefInstance di && di.getArguments() == null){
            return di.isParametric() && di.getBase().asParametric().getGenerics().size() != 0;
        }
        return false;
    }

    @Override
    public int countLocals() {
        return 0;
    }

    @Override
    public void generateCode(OutputStreamWriter out, int ident) throws IOException {
        if(binding instanceof DefInstance di){
            if(binding.isData()){
                var typ = TypeCollector.get().resolveTypeString(di, true);
                out.append(typ);
            } else {
                if(binding.isMultiMethod()){
                    out.append("<multi_method>#");
                } else if(binding.isFunction()){
                    out.append("<method>#");
                }
                out.append(TypeCollector.get().resolveEntityString(binding));
            }
        }
    }

    @Override
    public String toString() {
        return binding.toString();
    }
}
