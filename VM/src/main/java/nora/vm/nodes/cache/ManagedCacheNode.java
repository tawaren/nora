package nora.vm.nodes.cache;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import nora.vm.nodes.EnsureOrRetNode;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate;

//Todo: what if this is to big and should not be cached because of GarbageCollection?
//      add a no cache annotation to method
public class ManagedCacheNode extends CachedNode {
    @Child private NoraNode comp;
    public ManagedCacheNode(NoraNode comp) {
        this.comp = comp;
    }

    @Override
    public Object execute(VirtualFrame virtualFrame) {
        try {
            var res = comp.execute(virtualFrame);
            transferToInterpreterAndInvalidate();
            replace(NoraVmContext.getValueCache(null).createManagedCached(this, res));
            return res;
        } catch (EnsureOrRetNode.EarlyReturnException retExp){
            transferToInterpreterAndInvalidate();
            replace(NoraVmContext.getValueCache(null).createEarlyRetManagedCached(this, retExp));
            throw retExp;
        }
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        throw new RuntimeException("Should already be Specialised");
    }

    @Override
    public NoraNode liftCache() {
        return comp;
    }

    @Override
    public NoraNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return new ManagedCacheNode(comp);
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return comp.getType(frame);
    }
}
