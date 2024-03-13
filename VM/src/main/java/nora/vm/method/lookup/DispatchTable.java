package nora.vm.method.lookup;

import nora.vm.method.Method;
import nora.vm.method.MultiMethod;
import nora.vm.runtime.NoraVmContext;
import nora.vm.types.DispatchCoordinator;
import nora.vm.types.Type;
import nora.vm.types.TypeInfo;

import java.util.function.Consumer;

public abstract class DispatchTable {
    public abstract Method runtimeLookup(Object[] args);
}
