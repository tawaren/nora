package nora.vm.nodes.consts;

import com.oracle.truffle.api.CompilerAsserts;
import nora.vm.runtime.NoraVmContext;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

public class TypeNode extends ConstNode {
    private final Type value;

    TypeNode(Type value) {
        this.value = value;
    }

    @Override
    Object getConstant() {
        return value.info;
    }

    public Type getType() {
        return value;
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return NoraVmContext.getTypeUtil(null).TypeType;
    }
}
