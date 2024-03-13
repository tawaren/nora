package nora.vm.method;

import com.oracle.truffle.api.RootCallTarget;
import nora.vm.runtime.NoraVmContext;
import nora.vm.types.Type;

public class DefaultMethodResolver implements MethodResolver{
    private final String methodIdentifier;
    private final Type[] generics;
    public DefaultMethodResolver(String methodIdentifier, Type[] generics) {
        this.methodIdentifier = methodIdentifier;
        this.generics = generics;
    }
    @Override
    public String getMethodIdentifier() {
        return methodIdentifier;
    }

    @Override
    public Type resolveType() {
        return NoraVmContext.getLoader(null).loadMethodSig(methodIdentifier, generics);
    }

    @Override
    public RootCallTarget resolveTarget() {
        return NoraVmContext.getLoader(null).loadSpecializedMethod(methodIdentifier, generics).getCallTarget();
    }
}
