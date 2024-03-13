package nora.vm.types.schemas.utils;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
import nora.vm.nodes.ArgNode;
import nora.vm.nodes.equality.data.DataEqNode;
import nora.vm.nodes.equality.calls.EqDataCallRootNode;
import nora.vm.nodes.hash_code.data.DataHashCodeNode;
import nora.vm.nodes.hash_code.calls.HashCodeDataCallRootNode;
import nora.vm.nodes.property.PropertyManager;
import nora.vm.nodes.string.to_string.data.DataToStringNode;
import nora.vm.nodes.string.to_string.calls.ToStringDataCallRootNode;
import nora.vm.runtime.data.NoraData;

import static nora.vm.runtime.ValueCache.objectSizeCheck;

public abstract class SchemaHandler {
    public abstract PropertyManager[] getAllProperties();
    public abstract PropertyManager getProperty(int slot);

    private DataEqNode UNCACHED_EQ;
    public DataEqNode getUncachedEq(){
        if(UNCACHED_EQ == null) UNCACHED_EQ = createEqNode(-1);
        return UNCACHED_EQ;
    }
    public abstract DataEqNode createEqNode(int recursionDepth);

    private DirectCallNode eqCallNodeCache;
    public DirectCallNode createEqCallNode(){
        if(eqCallNodeCache != null) return eqCallNodeCache;
        var node = createEqNode(0);
        var root = new EqDataCallRootNode(new ArgNode(0),new ArgNode(1),node);
        eqCallNodeCache = DirectCallNode.create(root.getCallTarget());
        return eqCallNodeCache;
    }


    private DataToStringNode UNCACHED_TO_STRING;

    public DataToStringNode getUncachedToString(){
        if(UNCACHED_TO_STRING == null) UNCACHED_TO_STRING = createToStringNode(-1);
        return UNCACHED_TO_STRING;
    }
    public abstract DataToStringNode createToStringNode(int recursionDepth);

    private DirectCallNode toStringCallNodeCache;
    public DirectCallNode createToStringCallNode(){
        if(toStringCallNodeCache != null) return toStringCallNodeCache;
        var node = createToStringNode(0);
        var root = new ToStringDataCallRootNode(new ArgNode(0),new ArgNode(1),node);
        toStringCallNodeCache = DirectCallNode.create(root.getCallTarget());
        return toStringCallNodeCache;
    }

    private DataHashCodeNode UNCACHED_HASH_CODE;
    public DataHashCodeNode getUncachedHashCode(){
        if(UNCACHED_HASH_CODE == null) UNCACHED_HASH_CODE = createHashCodeNode(-1);
        return UNCACHED_HASH_CODE;
    }

    public abstract DataHashCodeNode createHashCodeNode(int recursionDepth);

    private DirectCallNode hashCodeCallNodeCache;
    public DirectCallNode createHashCodeCallNode(){
        if(hashCodeCallNodeCache != null) return hashCodeCallNodeCache;
        var node = createHashCodeNode(0);
        var root = new HashCodeDataCallRootNode(new ArgNode(0),node);
        hashCodeCallNodeCache = DirectCallNode.create(root.getCallTarget());
        return hashCodeCallNodeCache;
    }

    //-1 means no cache
    public int size(NoraData data, int budget, int depthLimit) {
        //if isInlineCache return 1;
        //if managedCache return -1;
        if(depthLimit == 0) return -1;
        budget = budget - 1; //1 to account for us
        if(budget <= 0) return -1;
        for(PropertyManager prop: getAllProperties()){
            var res = prop.getGetter().executeObjectGet(data);
            //Assuming Closures become runtime data
            budget = objectSizeCheck(res, budget, depthLimit-1);
            if(budget <= 0) return -1;
        }
        return budget;
    }

}
