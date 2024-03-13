package nora.vm.method;

import com.oracle.truffle.api.RootCallTarget;
import nora.vm.types.Type;

public interface MethodResolver {

    String getMethodIdentifier();

    Type resolveType();

    RootCallTarget resolveTarget();
}
