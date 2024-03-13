package nora.vm.runtime.data;

import nora.vm.method.EntryPoint;
import nora.vm.types.Type;
import nora.vm.types.schemas.ClosureSchemaHandler;

public interface ClosureDataFactory {
    ClosureData create(EntryPoint target, Type funType, Object label, ClosureSchemaHandler handler);
}
