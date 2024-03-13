package nora.vm.nodes.hash_code.data;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.DirectCallNode;
import nora.vm.nodes.equality.EqNode;
import nora.vm.nodes.hash_code.HashCodeNode;
import nora.vm.runtime.data.NoraData;
import nora.vm.types.schemas.utils.SchemaHandler;

@ReportPolymorphism
@ImportStatic(EqNode.class)
@NodeField(name = "recursionDepth", type = int.class)
@GenerateUncached
public abstract class DataHashCodeRecursionNode extends DataHashCodeNode {
    protected final static int INLINE_CACHE_SIZE = 3;

    abstract int getRecursionDepth();

    @Specialization(guards = {"left.handler == cachedHandler", "!recursionLimitReached()"}, limit = "INLINE_CACHE_SIZE")
    public int evalDataFast(NoraData left,
                                @Cached("left.handler") SchemaHandler cachedHandler,
                                @Cached(value = "cachedHandler.createHashCodeNode(recursionDepth)", uncached = "cachedHandler.getUncachedHashCode()")
                                DataHashCodeNode hashNode
    ) {
        return hashNode.executeHashCode(left);
    }

    @Specialization(guards = {"left.handler == cachedHandler", "recursionLimitReached()"}, limit = "INLINE_CACHE_SIZE")
    public int evalDataRecurse(NoraData left,
                                   @Cached(value = "left.handler", uncached = "left.handler") SchemaHandler cachedHandler,
                                   @Cached(value = "cachedHandler.createHashCodeCallNode()", uncached = "createNullCall()")
                                   DirectCallNode hashCallNode
    ) {
        assert hashCallNode != null; //Should never be called cached
        return (Integer) hashCallNode.call(left);
    }

    @Specialization
    public int evalDataSlow(NoraData left) {
        return left.handler.getUncachedHashCode().executeHashCode(left);
    }

    protected DirectCallNode createNullCall(){
        return null;
    }

    @Idempotent
    protected boolean recursionLimitReached() {
        if(this == DataHashCodeRecursionNodeGen.getUncached()) return false;
        return getRecursionDepth() >= HashCodeNode.MAX_RECURSION_DEPTH;
    }

    @Override
    public DataHashCodeNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return DataHashCodeRecursionNodeGen.create(getRecursionDepth());
    }
}
