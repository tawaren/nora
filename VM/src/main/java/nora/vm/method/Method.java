package nora.vm.method;

import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.RootCallTarget;
import nora.vm.method.lookup.DispatchTable;
import nora.vm.method.lookup.MethodLookup;
import nora.vm.runtime.NoraVmContext;
import nora.vm.types.TypeUtil;
import nora.vm.runtime.data.IdentityObject;
import nora.vm.types.Type;
import nora.vm.types.TypeInfo;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate;

public class Method extends IdentityObject implements Callable, MethodLookup {

    //Note: we can not make GC collectible as reloadign results in different closureIds
    //      &we are lucky we found a solution their
    @CompilationFinal private RootCallTarget target = null;
    @CompilationFinal private Type funType = null;
    private final MethodResolver resolver;

    //For use in MetaLanguage Protocols
    public Method(MethodResolver resolver) {
        this.resolver = resolver;
    }

    public Method(String methodIdentifier, Type[] generics) {
        this.resolver = new DefaultMethodResolver(methodIdentifier, generics);
    }

    public Type getType(){
        if(funType == null){
            transferToInterpreterAndInvalidate();
            funType = resolver.resolveType();
        }
        return funType;
    }

    @Override
    public String toString() {
        return getMethodIdentifier();
    }

    public RootCallTarget getTarget(){
        if(target == null){
            transferToInterpreterAndInvalidate();
            target = resolver.resolveTarget();
        }
        return target;
    }

    private boolean isMatch(TypeInfo[] args){
        for (int j = 0; j < args.length; j++) {
            if(!args[j].subTypeOf(funType.applies[j].type().info)) return false;
        }
        return true;
    }

    private boolean isMatch(int[] args){
        for (int j = 0; j < args.length; j++) {
            if(!funType.applies[j].type().info.isAssignableFromConcrete(args[j])) return false;
        }
        return true;
    }

    private boolean isMatch(Object[] args){
        TypeUtil typeUtil = NoraVmContext.getTypeUtil(null);
        for (int j = 0; j < args.length; j++) {
            if(!typeUtil.extractTypeInfo(args[j+1]).subTypeOf(funType.applies[j].type().info)) return false;
        }
        return true;
    }

    @Override
    public Method staticLookup(TypeInfo[] args) {
        assert isMatch(args);
        //Note: Static Typechecking should ensure this to be true
        return this;
    }

    @Override
    public Method slowRuntimeLookup(Object[] args) {
        assert isMatch(args);
        //Note: Static Typechecking should ensure this to be true
        return this;
    }

    @Override
    public Method runtimeLookup(Object[] args, int[] types) {
        assert isMatch(args);
        assert isMatch(types);
        //Note: Static Typechecking should ensure this to be true
        return this;
    }

    @Override
    public DispatchTable optimized() {
        return new DispatchTable() {
            @Override
            public Method runtimeLookup(Object[] args) {
                assert isMatch(args);
                //Note: Static Typechecking should ensure this to be true
                return Method.this;
            }
        };
    }

    @Override
    public int dispatchedArgs() {
        return 0;
    }

    public String getMethodIdentifier() {
        return resolver.getMethodIdentifier();
    }


}
