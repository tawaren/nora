package nora.vm.nodes.equality.data;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.DirectCallNode;
import nora.vm.nodes.equality.EqNode;
import nora.vm.nodes.equality.EqPlainNodeGen;
import nora.vm.runtime.data.NoraData;
import nora.vm.types.schemas.utils.SchemaHandler;

@ReportPolymorphism
@ImportStatic(EqNode.class)
@NodeField(name = "recursionDepth", type = int.class)
@GenerateUncached
public abstract class DataEqRecursionNode extends DataEqNode {
    protected final static int INLINE_CACHE_SIZE = 3;

    abstract int getRecursionDepth();

    @Specialization(guards = "left == right")
    public boolean evalDataRef(NoraData left, NoraData right) {
        return true;
    }

    @Specialization(guards = "left.handler != right.handler")
    public boolean evalDataDiff(NoraData left, NoraData right) {
        return false;
    }

    @Specialization(guards = {"left.handler == cachedHandler","right.handler == cachedHandler", "!recursionLimitReached()"}, limit = "INLINE_CACHE_SIZE")
    public boolean evalDataFast(NoraData left, NoraData right,
                                @Cached("left.handler") SchemaHandler cachedHandler,
                                @Cached(value = "cachedHandler.createEqNode(recursionDepth)", uncached = "cachedHandler.getUncachedEq()")
                                    DataEqNode eqNode
    ) {
        return eqNode.executeDataEq(left, right);
    }

    @Specialization(guards = {"left.handler == cachedHandler","right.handler == cachedHandler", "recursionLimitReached()"}, limit = "INLINE_CACHE_SIZE")
    public boolean evalDataRecurse(NoraData left, NoraData right,
                                @Cached(value = "left.handler", uncached = "left.handler") SchemaHandler cachedHandler,
                                @Cached(value = "cachedHandler.createEqCallNode()", uncached = "createNullCall()")
                                       DirectCallNode eqCallNode
    ) {
        assert eqCallNode != null; //Should never be called cached
        return (Boolean) eqCallNode.call(left, right);
    }

    @Specialization
    public boolean evalDataSlow(NoraData left, NoraData right) {
        assert left.handler == right.handler;
        return left.handler.getUncachedEq().executeDataEq(left, right);
    }

    protected DirectCallNode createNullCall(){
        return null;
    }

    @Idempotent
    protected boolean recursionLimitReached() {
        if(this == DataEqRecursionNodeGen.getUncached()) return false;
        return getRecursionDepth() >= EqNode.MAX_RECURSION_DEPTH;
    }

    @Override
    public DataEqNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return DataEqRecursionNodeGen.create(getRecursionDepth());
    }
}
