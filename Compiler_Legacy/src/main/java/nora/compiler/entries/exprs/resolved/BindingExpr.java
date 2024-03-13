package nora.compiler.entries.exprs.resolved;

import nora.compiler.entries.Instance;
import nora.compiler.resolver.bindings.ArgBinding;
import nora.compiler.resolver.bindings.Binding;
import nora.compiler.resolver.bindings.TypeBinding;
import nora.compiler.processing.TypeCheckContext;
import nora.compiler.resolver.bindings.ValBinding;

import java.io.IOException;
import java.io.OutputStreamWriter;

public class BindingExpr extends ResolvedExpr {
    private final Binding binding;
    private Instance typ = null;

    public BindingExpr(Binding binding) {
        this.binding = binding;
    }

    public Instance constType(TypeCheckContext context) {
        if(binding instanceof TypeBinding tb){
            return context.getGenerics().get(tb.getArg());
        } else {
            return null;
        }
    }

    public Binding getBinding(){
        return binding;
    }

    @Override
    public Instance getAndInferenceType(TypeCheckContext context) {
        if(typ == null) {
            typ = context.getBinding(binding);
            //Todo: make a structure exception
            if(typ == null) {
                //if (context.getRetHint() != null) {
                    //Todo: we could set the binding
                //}
                // structural? inferenz?
                throw new RuntimeException("Missing Binding");
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
        return 0;
    }

    @Override
    public void generateCode(OutputStreamWriter out, int ident) throws IOException {
        binding.generateCode(out);
    }

    @Override
    public String toString() {
        return binding.toString();
    }
}
