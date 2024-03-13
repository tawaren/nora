package nora.vm.method;

import nora.vm.types.Type;

//We make a class to prevent the overhead of interface instanceOf checks
public interface Function {
    Type getType();
}
