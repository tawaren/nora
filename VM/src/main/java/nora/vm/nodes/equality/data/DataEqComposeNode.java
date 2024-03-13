package nora.vm.nodes.equality.data;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import nora.vm.runtime.data.NoraData;

import java.util.Arrays;

public class DataEqComposeNode extends DataEqNode {
    @Children DataEqNode[] cmpNodes;

    protected DataEqComposeNode(DataEqNode[] cmpNodes) {
        this.cmpNodes = cmpNodes;
    }

    @Override
    @ExplodeLoop
    public boolean executeDataEq(NoraData d1, NoraData d2){
        CompilerAsserts.partialEvaluationConstant(cmpNodes.length);
        for(int i = 0; i < cmpNodes.length; i++){
            if(!cmpNodes[i].executeDataEq(d1,d2)) return false;
        }
        return true;
    }

    public DataEqComposeNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        DataEqNode[] nCmpNodes = Arrays.stream(cmpNodes).map(DataEqNode::cloneUninitialized).toArray(DataEqNode[]::new);
        return new DataEqComposeNode(nCmpNodes);
    }
}
