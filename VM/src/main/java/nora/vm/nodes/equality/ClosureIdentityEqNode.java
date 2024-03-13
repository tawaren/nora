package nora.vm.nodes.equality;

import nora.vm.nodes.equality.data.DataEqNode;
import nora.vm.runtime.data.ClosureData;
import nora.vm.runtime.data.NoraData;

public class ClosureIdentityEqNode extends DataEqNode {
    @Override
    public boolean executeDataEq(NoraData left, NoraData right) {
        if(left instanceof ClosureData d1 && right instanceof ClosureData d2){
            return d1.identity == d2.identity;
        }
        return false;
    }

    @Override
    public DataEqNode cloneUninitialized() {
        return this;
    }
}
