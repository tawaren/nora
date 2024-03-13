package nora.vm.method.lookup;

import nora.vm.method.Method;
import nora.vm.types.Type;
import nora.vm.types.TypeInfo;

public interface MethodLookup{
    //is used statically to build arguments tables and indexes
    Method staticLookup(TypeInfo[] args);
    //used at runtime needs to ensure that the result is resolved
    Method slowRuntimeLookup(Object[] args);
    Method runtimeLookup(Object[] args, int[] types);

    DispatchTable optimized();

    Type getType();

    int dispatchedArgs();
}
