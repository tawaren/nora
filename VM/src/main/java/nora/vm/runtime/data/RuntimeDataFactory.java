package nora.vm.runtime.data;

import nora.vm.types.schemas.DataSchemaHandler;

public interface RuntimeDataFactory {
    RuntimeData create(DataSchemaHandler handler, int dispatcherIndex);
}
