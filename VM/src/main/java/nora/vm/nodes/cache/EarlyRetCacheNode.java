package nora.vm.nodes.cache;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import nora.vm.nodes.EnsureOrRetNode;
import nora.vm.nodes.NoraNode;
import nora.vm.runtime.ValueCache;
import nora.vm.specTime.SpecFrame;

import java.lang.ref.Reference;

public class EarlyRetCacheNode extends CachedNode{
    @Child NoraNode original;
    private final Reference<ValueCache.CacheEntry> cachedValue;

    public EarlyRetCacheNode(NoraNode original, Reference<ValueCache.CacheEntry> cachedValue) {
        this.original = original;
        this.cachedValue = cachedValue;
    }

    @Override
    public Object execute(VirtualFrame virtualFrame) {
        var res = cachedValue.get();
        if(res != null) {
            var val = res.access();
            CompilerAsserts.partialEvaluationConstant(val);
            throw (EnsureOrRetNode.EarlyReturnException)val;
        } else {
            //This will immediately replace itself with a new copy of EarlyRetCacheNode (after evaluating)
            return replace(original).execute(virtualFrame);
        }
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        throw new RuntimeException("Should already be Specialised");
    }

    @Override
    public int complexity() {
        return 2;
    }

    @Override
    public NoraNode cloneUninitialized() {
        return new EarlyRetCacheNode(original.cloneUninitialized(), cachedValue) ;
    }

    @Override
    public NoraNode liftCache() {
        return original;
    }

}
