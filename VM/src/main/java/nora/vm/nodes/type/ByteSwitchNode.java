package nora.vm.nodes.type;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate;

public class ByteSwitchNode extends SwitchNode {
    @Child NoraNode inner;

    protected ByteSwitchNode(NoraNode inner) {
        this.inner = inner;
    }

    @Override
    public Object execute(VirtualFrame virtualFrame) {
        try {
            return inner.executeInt(virtualFrame);
        } catch (UnexpectedResultException e) {
            transferToInterpreterAndInvalidate();
            throw new RuntimeException("Type Missmatch: expected Int got "+e.getResult().getClass(), e);
        }
    }

    @Override
    public byte executeByte(VirtualFrame virtualFrame) throws UnexpectedResultException {
        return inner.executeByte(virtualFrame);
    }

    @Override
    public NoraNode cloneUninitialized() {
        return new ByteSwitchNode(inner.cloneUninitialized());
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        var newInner = inner.specialise(frame);
        if(newInner instanceof ConstNode) return newInner;
        if(newInner instanceof CachedNode cn) return new CacheNode(new ByteSwitchNode(cn.liftCache()));
        return new ByteSwitchNode(newInner);
    }

    @Override
    public String toString() {
        return inner.toString();
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return NoraVmContext.getTypeUtil(null).IntType;
    }

}
