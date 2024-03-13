package nora.vm.nodes.type;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import nora.vm.nodes.ExecutionNode;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;
import nora.vm.types.TypeInfo;


public  class SimpleInstanceOfNode extends ExecutionNode {
    @Child NoraNode srcIndex;
    private final TypeInfo superType;

    public SimpleInstanceOfNode(NoraNode srcIndex, TypeInfo superType) {
        this.srcIndex = srcIndex;
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
            var srcT = srcIndex.executeInt(virtualFrame);
            return superType.isAssignableFromConcrete(srcT);
        } catch (UnexpectedResultException | ClassCastException e) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new IllegalStateException("Type Error: "+e);
        }

    }

    @Override
    public String toString() {
        return "subtypeOf("+srcIndex+", "+superType+")";
    }

    @Override
    public SimpleInstanceOfNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return new SimpleInstanceOfNode(srcIndex.cloneUninitialized(), superType);
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return NoraVmContext.getTypeUtil(null).BooleanType;
    }
}
