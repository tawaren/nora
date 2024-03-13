package nora.vm.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

public abstract class ExecutionNode extends NoraNode {
    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        throw new RuntimeException("Should already be Specialised");
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        System.out.println("Warning getType() called on an execution node - consider overwriting: "+getClass());
        return null;
    }
}
