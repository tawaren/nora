package nora.vm.nodes.hash_code.data;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import nora.vm.runtime.data.NoraData;

import java.util.Arrays;

public class DataHashCodeComposeNode extends DataHashCodeNode {
    @Children DataHashCodeNode[] hashNodes;

    protected DataHashCodeComposeNode(DataHashCodeNode[] hashNodes) {
        this.hashNodes = hashNodes;
    }

    @ExplodeLoop
    public int executeHashCode(NoraData data){
        CompilerAsserts.partialEvaluationConstant(hashNodes.length);
        var cur = 0;
        for (int i = 0; i < hashNodes.length; i++){
            cur = 31*cur + hashNodes[i].executeHashCode(data);
        }
        return cur;
    }

    public DataHashCodeComposeNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        DataHashCodeNode[] nToStrNodes = Arrays.stream(hashNodes).map(DataHashCodeNode::cloneUninitialized).toArray(DataHashCodeNode[]::new);
        return new DataHashCodeComposeNode(nToStrNodes);
    }
}
