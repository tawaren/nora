package nora.vm.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

public class TypedRetNode extends ExecutionNode{
    @Child private NoraNode retValue;

    private final Type nonRetType;
    public TypedRetNode(NoraNode retValue, Type nonRetType) {
        this.nonRetType = nonRetType;
        this.retValue = retValue;
    }

    @Override
    public Object execute(VirtualFrame virtualFrame) {
        throw new EnsureOrRetNode.EarlyReturnException(retValue.execute(virtualFrame));
    }


    @Override
    public String toString() {
        return "return "+retValue;
    }

    @Override
    public TypedRetNode cloneUninitialized() {
        return new TypedRetNode(retValue.cloneUninitialized(), nonRetType);
    }

    @Override
    public Type getType(SpecFrame frame) {
        return nonRetType;
    }

    @Override
    public boolean isUnboxed() {
        return false;
    }

    @Override
    public int complexity() {
        return 1;
    }
}
