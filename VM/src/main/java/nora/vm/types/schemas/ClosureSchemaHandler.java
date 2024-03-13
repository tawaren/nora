package nora.vm.types.schemas;

import com.oracle.truffle.api.interop.TruffleObject;
import nora.vm.method.EntryPoint;
import nora.vm.nodes.property.PropertyManager;
import nora.vm.runtime.data.ClosureData;
import nora.vm.runtime.data.ClosureDataFactory;
import nora.vm.types.Type;
import nora.vm.types.schemas.utils.SchemaHandler;

public abstract class ClosureSchemaHandler extends SchemaHandler implements TruffleObject {
    public final ClosureDataFactory factory;

    protected ClosureSchemaHandler(ClosureDataFactory factory) {
        this.factory = factory;
    }

    public ClosureData create(EntryPoint code, Type funType, Object label){
        return factory.create(code,funType, label, this);
    }
    public abstract PropertyManager getCapture(int slot);
    public abstract PropertyManager[] getAllCaptures();


}
