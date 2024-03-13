package nora.vm.nodes.hash_code;

import nora.vm.nodes.hash_code.data.DataHashCodeNode;
import nora.vm.runtime.data.ClosureData;
import nora.vm.runtime.data.NoraData;

public class ClosureIdentityHashCodeNode extends DataHashCodeNode {
    @Override
    public int executeHashCode(NoraData data) {
        if(data instanceof ClosureData cd){
            return cd.identity.hashCode();
        }
        return 0;
    }

    @Override
    public DataHashCodeNode cloneUninitialized() {
        return this;
    }
}
