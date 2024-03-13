package nora.vm.method;

import com.oracle.truffle.api.CallTarget;

//We make a class to prevent the overhead of interface instanceOf checks
public interface Callable extends Function {
    CallTarget getTarget();
}
