package nora.vm.nodes.consts;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import nora.vm.runtime.NoraVmContext;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

import java.math.BigInteger;

public class LongNode extends ConstNode{
    private final long value;

    LongNode(long value) {
        this.value = value;
    }

    @Override
    Object getConstant() {
        return value;
    }

    @Override
    public long executeLong(VirtualFrame virtualFrame) throws UnexpectedResultException {
        return value;
    }

    @Override
    public BigInteger executeBigInteger(VirtualFrame virtualFrame) throws UnexpectedResultException {
        return BigInteger.valueOf(value);
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return NoraVmContext.getTypeUtil(null).NumType;
    }
}
