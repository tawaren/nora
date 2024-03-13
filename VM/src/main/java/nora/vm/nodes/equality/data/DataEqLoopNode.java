package nora.vm.nodes.equality.data;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import nora.vm.nodes.property.reader.NoraPropertyGetNode;
import nora.vm.runtime.data.NoraData;
import nora.vm.runtime.data.RuntimeData;
import nora.vm.types.schemas.DataSchemaHandler;

import java.util.Arrays;

public class DataEqLoopNode extends DataEqNode {
    @Child DataEqNode main;
    @Child NoraPropertyGetNode recursionProp;
    @Child DataEqNode fallback;
    private final DataSchemaHandler loopCheck;

    protected DataEqLoopNode(DataEqNode main, NoraPropertyGetNode recursionProp, DataEqNode fallback, DataSchemaHandler loopCheck) {
        this.main = main;
        this.recursionProp = recursionProp;
        this.fallback = fallback;
        this.loopCheck = loopCheck;
    }

    @Override
    public boolean executeDataEq(NoraData d1, NoraData d2){
        do{
            if(!main.executeDataEq(d1,d2)) return false;
            var inter1 = recursionProp.executeObjectGet(d1);
            var inter2 = recursionProp.executeObjectGet(d2);
            //if primitive rec they may not be nora data
            if(!(inter1 instanceof NoraData nd1) || nd1.handler != loopCheck) break;
            if(!(inter2 instanceof NoraData nd2) || nd2.handler != loopCheck) break;
            d1 = (NoraData)inter1;
            d2 = (NoraData)inter2;
        } while (true);
        return fallback.executeDataEq(d1,d2);
    }

    public DataEqLoopNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return new DataEqLoopNode(main.cloneUninitialized(),recursionProp,fallback.cloneUninitialized(), loopCheck);
    }
}
