package nora.vm.method;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import nora.vm.method.lookup.DispatchTable;
import nora.vm.method.lookup.MethodLookup;
import nora.vm.method.lookup.MultiMethodTable;
import nora.vm.runtime.NoraVmContext;
import nora.vm.runtime.data.IdentityObject;
import nora.vm.types.TypeUtil;
import nora.vm.types.Type;
import nora.vm.types.TypeInfo;

import java.lang.ref.SoftReference;
import java.util.Arrays;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate;

// This will be a rela type (but lazy loaded)
// Todo: figure out how to best lazy load
//   is it suffice to annotated as compiletime final something and load on first access?
//    make load outside of truffle boundary
public class MultiMethod extends IdentityObject implements Function, MethodLookup {
    //private final String name;
    public final Type[][] allDispatches;
    //lazy fetched methode (can we mark the inner compiletime constant
    private final Method[] allMethods;
    //This will be needed by arguments table to find roots
    private final Type[] argumentTypes;
    //This is the full type of the MultiMethod (stabilized)
    private final Type funType;
    //This declares if it was detected as exhaustive
    private final boolean exhaustive;

    public MultiMethod(GenericMultiMethod.MethodDispatch[] allInfos, Type[] argumentTypes, Type funType, boolean exhaustive) {
        this.allDispatches = new Type[allInfos.length][];
        this.allMethods = new Method[allInfos.length];
        this.argumentTypes = argumentTypes;
        this.exhaustive = exhaustive;
        for(int i = 0; i < allInfos.length; i++){
            this.allDispatches[i] = allInfos[i].arguments();
            this.allMethods[i] = allInfos[i].method();
        }
        if(allMethods.length == 0) throw new RuntimeException("What");
        this.funType = funType;
    }


    //allows to reduce the lookup to the template level
    public Method staticLookup(TypeInfo[] args){
        CompilerAsserts.neverPartOfCompilation();
        methodLookup:
        for (int i = 0; i < allMethods.length; i++) {
            var dispSig = allDispatches[i];
            for (int j = 0; j < args.length; j++) {
                if(!args[j].subTypeOf(dispSig[j].info)) continue methodLookup;
            }
            return allMethods[i];
        }
        return null;
    }

    @Override
    public Method runtimeLookup(Object[] args, int[] typeInfos) {
        if(optimized != null) return optimized.runtimeLookup(args);
        methodLookup:
        for (int i = 0; i < allMethods.length; i++) {
            var dispSig = allDispatches[i];
            for (int j = 0; j < typeInfos.length; j++) {
                if(!dispSig[j].info.isAssignableFromConcrete(typeInfos[j])) continue methodLookup;
            }
            return allMethods[i];
        }
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
        //We capture here as it will be optimized if called enough anyway
        TypeUtil util = NoraVmContext.getTypeUtil(null);
        methodLookup:
        for (int i = 0; i < allMethods.length; i++) {
            var dispSig = allDispatches[i];
            for (int j = 0; j < dispSig.length; j++) {
                //j+1 as 0th arg is method
                var argTypeInfo = util.extractTypeInfo(args[j+1]);
                assert dispSig[j].info.cat() == argTypeInfo.cat();
                if(!dispSig[j].info.isAssignableFromConcrete(argTypeInfo.id())) continue methodLookup;
            }
            return allMethods[i];
        }
        return null;
    }

    @Override
    public int dispatchedArgs() {
        return argumentTypes.length;
    }

    public Type[] getArgumentTypes() {
        return argumentTypes;
    }

    public Type[] getDispatchRootTypes(){
        return argumentTypes;
    }

    public Type getType() {
        return funType;
    }

    public boolean isExhaustive() {
        return exhaustive;
    }

    public int getNumMethods() {
        return allMethods.length;
    }

    public Method getMethod(int index){
        return allMethods[index];
    }

    @Override
    public String toString() {
        return Arrays.toString(allMethods);
    }

    @CompilationFinal private DispatchTable optimized = null;

    public DispatchTable optimized(){
        if(optimized == null) {
            transferToInterpreterAndInvalidate();
            //Todo: evaluate best optimisation strategy
            //1. Eliminate unreachable Methods
            //2. if(methods.length == 1){
            //    do a single method lookup
            //    new SingleMethodLookup(method, arguments)
            //}
            //3. if(arguments size < ???) {  //arguments size is: multiplication of all Arg Dimensions (dynamics are special)
            //    do a arguments table
            optimized = MultiMethodTable.create(this);
            //}
            //4. Do some form of index accelerating the search without using to much space
        }

        return optimized;
    }
}
