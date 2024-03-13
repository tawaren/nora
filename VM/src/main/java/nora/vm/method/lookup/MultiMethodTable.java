package nora.vm.method.lookup;

import nora.vm.method.Method;
import nora.vm.method.MultiMethod;
import nora.vm.runtime.NoraVmContext;
import nora.vm.types.Type;
import nora.vm.types.TypeInfo;

import java.util.function.Consumer;

public class MultiMethodTable extends DispatchTable {
    private final Table table;
    private final int[] rootDispatchInfo;

    private MultiMethodTable(Table table, Type[] argRootTypes) {
        this.table = table;
        this.rootDispatchInfo = new int[argRootTypes.length];
        var coordinator = NoraVmContext.getDispatchCoordinator(null);
        for(int i = 0; i < argRootTypes.length; i++){
            this.rootDispatchInfo[i] = coordinator.getRootTypeInfo(argRootTypes[i]);
        }
    }

    //Todo: Test if faster specialized on index over table and nested loops
    private static void forEachIndex(int cur, int[] dimensions, int[] index, Consumer<int[]> f){
        if(cur < dimensions.length){
            for(int i = 0; i < dimensions[cur]; i++){
                index[cur] = i;
                forEachIndex(cur+1, dimensions, index, f);
            }
        } else {
            f.accept(index);
        }
    }

    private static void forEachIndex(int[] dimensions, Consumer<int[]> f){
        var index = new int[dimensions.length];
        forEachIndex(0,dimensions,index,f);
    }

    public static MultiMethodTable create(MultiMethod multiMethod){
        var coordinator = NoraVmContext.getDispatchCoordinator(null);
        var sig = multiMethod.getDispatchRootTypes();
        var numArgs = sig.length;
        assert numArgs > 0;
        var types = new TypeInfo[numArgs][];
        var dimensions = new int[numArgs];
        for(int i = 0; i < numArgs; i++){
            var concretes = coordinator.getDispatchSubtypes(sig[i]);
            types[i] = concretes;
            dimensions[i] = concretes.length;
        }

        var table = Table.create(dimensions);
        var args = new TypeInfo[numArgs];
        forEachIndex(dimensions, (int[] index) -> {
            for(int i = 0; i < index.length; i++){
                args[i] = types[i][index[i]];
            }
            table.set(index,multiMethod.staticLookup(args));
        });
        return new MultiMethodTable(table, sig);
    }

    @Override
    public Method runtimeLookup(Object[] args) {
        return table.get(args, rootDispatchInfo);
        //This seems slower but margin to samll to be conclusive
        //return table.get(args, (arg, i) -> DispatchCoordinator.getDispatchIndex(arg, rootDispatchInfo[i]));
    }

    @Override
    public String toString() {
        return table.toString();
    }
}
