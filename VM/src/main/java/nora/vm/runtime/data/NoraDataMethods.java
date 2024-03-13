package nora.vm.runtime.data;

import nora.vm.types.Type;
import nora.vm.types.TypeInfo;

//We need those in a interface as StaticObject do not like them on classes
public interface NoraDataMethods {
    Type getType();
    TypeInfo getTypeInfo();
}
