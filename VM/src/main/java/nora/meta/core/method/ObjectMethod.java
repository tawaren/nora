package nora.meta.core.method;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import nora.meta.core.PrivateObjectLayout;
import nora.meta.core.PublicObjectLayout;
import nora.vm.method.Function;
import nora.vm.method.Method;
import nora.vm.method.lookup.DispatchTable;
import nora.vm.method.lookup.MethodLookup;
import nora.vm.runtime.NoraVmContext;
import nora.vm.runtime.data.IdentityObject;
import nora.vm.runtime.data.RuntimeData;
import nora.vm.types.Type;
import nora.vm.types.TypeInfo;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate;

public class ObjectMethod extends IdentityObject implements Function, MethodLookup {
    final String methodId;
    final PublicObjectLayout rootLayout;
    //Just the Method generics without the object ones
    final Type[] generics;
    @CompilationFinal private Type funType;

    public ObjectMethod(String methodId, PublicObjectLayout rootLayout, Type[] generics) {
        this.methodId = methodId;
        this.rootLayout = rootLayout;
        this.generics = generics;
    }

    @Override
    public Type getType() {
        if(funType == null){
            transferToInterpreterAndInvalidate();
            //Requires that their is an entry for the public one in addition to private ones
            var objGens = rootLayout.type.applies;
            var publicSig = new Type[objGens.length+generics.length];
            System.arraycopy(objGens,0,publicSig,0,objGens.length);
            System.arraycopy(generics,0,publicSig,objGens.length,generics.length);
            funType = NoraVmContext.getLoader(null).loadMethodSig(methodId, publicSig);
        }
        return funType;
    }

    @Override
    public Method staticLookup(TypeInfo[] args) {
        //Sadly we can not get the method without the secret argument
        //But the private schemas do not have their own type
        return null;
    }
    private static final int hotThreshold = 15;
    private int slowOptimCounter;

    @Override
    public Method slowRuntimeLookup(Object[] args) {
        if(optimized != null) return optimized.runtimeLookup(args);
        if(++slowOptimCounter == hotThreshold) {
            return optimized().runtimeLookup(args);
        }
        return runtimeLookup(args, null);
    }

    String generatePrivateId(PrivateObjectLayout handler){
        return methodId+"__"+handler.getMethodSuffix();
    }
    @Override
    public Method runtimeLookup(Object[] args, int[] types) {
        if(optimized != null) return optimized.runtimeLookup(args);
        assert args.length > 0;
        assert args[0] instanceof RuntimeData;
        var handler = ((RuntimeData)args[0]).handler;
        assert handler instanceof PrivateObjectLayout;
        var sig = ((PrivateObjectLayout) handler).fullObjectSignature();
        var nSig = new Type[sig.length+generics.length];
        System.arraycopy(sig, 0, nSig, 0, sig.length);
        System.arraycopy(generics, 0, nSig, sig.length, generics.length);
        return NoraVmContext.getMethodFactory(null).create(methodId, nSig);
    }

    @CompilationFinal private DispatchTable optimized = null;

    public DispatchTable optimized(){
        if(optimized == null) {
            transferToInterpreterAndInvalidate();
            optimized = ObjectMethodTable.create(this);
        }
        return optimized;
    }

    @Override
    public int dispatchedArgs() {
        return 1;
    }
}
