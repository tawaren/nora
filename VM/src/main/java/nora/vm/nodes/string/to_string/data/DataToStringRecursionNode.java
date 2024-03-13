package nora.vm.nodes.string.to_string.data;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
import nora.vm.nodes.equality.EqNode;
import nora.vm.nodes.equality.data.DataEqNode;
import nora.vm.nodes.equality.data.DataEqRecursionNodeGen;
import nora.vm.nodes.string.to_string.ToStringNode;
import nora.vm.nodes.string.to_string.data.DataToStringRecursionNodeGen;
import nora.vm.runtime.data.NoraData;
import nora.vm.types.schemas.utils.SchemaHandler;

@ReportPolymorphism
@ImportStatic(EqNode.class)
@NodeField(name = "recursionDepth", type = int.class)
@GenerateUncached
public abstract class DataToStringRecursionNode extends DataToStringNode {
    protected final static int INLINE_CACHE_SIZE = 3;
    abstract int getRecursionDepth();

    @Specialization(guards = {"data.handler == cachedHandler", "!recursionLimitReached()"}, limit = "INLINE_CACHE_SIZE")
    public void evalDataFast(TruffleStringBuilder builder, NoraData data,
                             @Cached("data.handler") SchemaHandler cachedHandler,
                             @Cached(value = "cachedHandler.createToStringNode(recursionDepth)", uncached = "cachedHandler.getUncachedToString()")
                                 DataToStringNode toStringNode
    ) {
        toStringNode.executeToString(builder, data);
    }

    @Specialization(guards = {"data.handler == cachedHandler", "recursionLimitReached()"}, limit = "INLINE_CACHE_SIZE")
    public void evalDataRecurse(TruffleStringBuilder builder, NoraData data,
                                @Cached(value = "data.handler", uncached = "data.handler") SchemaHandler cachedHandler,
                                @Cached(value = "cachedHandler.createToStringCallNode()", uncached = "createNullCall()")
                                    DirectCallNode toStringCallNode
    ) {
        toStringCallNode.call(builder, data);
    }

    @Specialization
    public void evalDataSlow(TruffleStringBuilder builder, NoraData data) {
        data.handler.getUncachedToString().executeToString(builder,data);
    }

    protected DirectCallNode createNullCall(){
        return null;
    }

    @Idempotent
    protected boolean recursionLimitReached() {
        if(this == DataToStringRecursionNodeGen.getUncached()) return false;
        return getRecursionDepth() >= ToStringNode.MAX_RECURSION_DEPTH;
    }

    @Override
    public DataToStringNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return DataToStringRecursionNodeGen.create(getRecursionDepth());
    }
}
