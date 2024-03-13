package nora.vm.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.strings.TruffleString;
import nora.vm.method.Function;
import nora.vm.runtime.data.RuntimeData;
import nora.vm.types.TypeInfo;

import java.math.BigInteger;

public abstract class ForwardNode extends NoraNode {
    @Override
    public Object execute(VirtualFrame virtualFrame) {
        throw new RuntimeException("A forward node should overwrite all executes");
    }

    @Override
    public boolean executeBoolean(VirtualFrame virtualFrame) throws UnexpectedResultException {
        throw new RuntimeException("A forward node should overwrite all primitive executes");
    }

    @Override
    public byte executeByte(VirtualFrame virtualFrame) throws UnexpectedResultException {
        throw new RuntimeException("A forward node should overwrite all primitive executes");
    }

    @Override
    public int executeInt(VirtualFrame virtualFrame) throws UnexpectedResultException {
        throw new RuntimeException("A forward node should overwrite all primitive executes");
    }

    @Override
    public long executeLong(VirtualFrame virtualFrame) throws UnexpectedResultException {
        throw new RuntimeException("A forward node should overwrite all primitive executes");
    }
}
