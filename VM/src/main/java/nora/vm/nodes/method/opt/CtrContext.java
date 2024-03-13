package nora.vm.nodes.method.opt;

import com.oracle.truffle.api.staticobject.StaticProperty;
import nora.vm.runtime.data.NoraData;
import nora.vm.runtime.data.RuntimeData;

public final class CtrContext {
    public final NoraData result;
    public final StaticProperty property;

    public CtrContext(NoraData result, StaticProperty property) {
        this.result = result;
        this.property = property;
    }

    public void adaptContext(Object res){
        property.setObject(result, res);
    }
}
