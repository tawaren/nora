package nora.vm.nodes.method;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import nora.vm.method.lookup.DispatchTable;
import nora.vm.method.lookup.MethodLookup;

@ReportPolymorphism
public abstract class MethodLookupDispatchNode extends TypedDispatchNode {
    protected final int cacheSize;
    
    public MethodLookupDispatchNode(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    //We cache 3 but in return we rewrite in general case
    @Specialization(guards = "sameTypes(types,cachedTypes)", limit = "cacheSize")
    public Object cachedLookupCall(MethodLookup lookup, Object[] args, int[] types,
                                   @Cached(value = "types", dimensions = 1) int[] cachedTypes,
                                   @Cached(value = "initDirectCall(lookup, args, cachedTypes)") DirectCallNode callNode) {
        return callDirect(callNode,args);
    }

    //We loose inlining oppertunity but get faster dispatches
    @Specialization(replaces = "cachedLookupCall")
    public Object tableLookupCall(MethodLookup lookup, Object[] args, int[] types,
                                  @Cached(neverDefault = true, value = "lookup.optimized()") DispatchTable dispatchTable,
                                  @Cached(neverDefault = true, value = "initIndirectCall()") IndirectCallNode callNode
    ) {
        CompilerAsserts.partialEvaluationConstant(dispatchTable);
        return callIndirect(callNode, dispatchTable.runtimeLookup(args).getTarget(), args);
    }

    @ExplodeLoop
    protected boolean sameTypes(int[] types, int[] cached){
        CompilerAsserts.partialEvaluationConstant(types.length);
        CompilerAsserts.partialEvaluationConstant(cached.length);
        assert types.length == cached.length;
        for(int i = 0; i < types.length; i++){
            if(types[i] != cached[i]) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "MethodLookupDispatch";
    }

    @Override
    public MethodLookupDispatchNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return MethodLookupDispatchNodeGen.create(cacheSize);
    }
}
