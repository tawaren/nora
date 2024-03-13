package nora.vm.nodes.type;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import nora.vm.nodes.ExecutionNode;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;
import nora.vm.types.TypeInfo;


public  class FullInstanceOfNode extends ExecutionNode {
    @Child NoraNode srcType;
    private final Type superType;

    public FullInstanceOfNode(NoraNode srcType, Type superType) {
        this.srcType = srcType;
        this.superType = superType;
    }

    @Override
    public Object execute(VirtualFrame virtualFrame) {
        try {
            return executeBoolean(virtualFrame);
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new IllegalStateException("Type Error");
        }
    }

    @Override
    public boolean executeBoolean(VirtualFrame virtualFrame) throws UnexpectedResultException {
        try {
            var srcT = (Type)srcType.executeObject(virtualFrame);
            return srcT.subTypeOf(superType);
        } catch (UnexpectedResultException | ClassCastException e) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new IllegalStateException("Type Error: "+e);
        }
    }

    @Override
    public String toString() {
        return "subtypeOf("+srcType+", "+superType+")";
    }

    @Override
    public FullInstanceOfNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return new FullInstanceOfNode(srcType.cloneUninitialized(), superType);
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return NoraVmContext.getTypeUtil(null).BooleanType;
    }
}
