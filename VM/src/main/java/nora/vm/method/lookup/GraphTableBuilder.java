package nora.vm.method.lookup;

import nora.vm.method.MultiMethod;
import nora.vm.runtime.NoraVmContext;
import nora.vm.types.DispatchCoordinator;
import nora.vm.types.Type;
import nora.vm.types.TypeInfo;

import java.util.HashMap;
import java.util.Map;

//Todo: Finish
//      Hower will only make sense for a large usecase:
//      Example: 32 components 16 events, each component handles 2 events
//      Further, it needs to be capable of handling more than 64 Methods
public class GraphTableBuilder {

    private record FullStateKey(int depth, long states){}
    interface StateEntry{};
    private record StateNode(int offset, FullStateKey[] layer) implements StateEntry{};
    private record StateLeaf(int methodIndex) implements StateEntry{};

    private final DispatchCoordinator coordinator = NoraVmContext.getDispatchCoordinator(null);
    private final Map<FullStateKey, StateEntry> layers = new HashMap<>();
    private final Type[][] disp;
    private final Type[] sig;
    private int nextFreeIndex;
    private int[] table;

    public GraphTableBuilder(MultiMethod multiMethod) {
        disp = multiMethod.allDispatches;
        sig = multiMethod.getDispatchRootTypes();
    }

    private long filter(long state, TypeInfo typeInfo, int depth){
        var res = state;
        for(int i = 0; i < disp.length; i++){
            var mask = (1 << i);
            //still active
            if(((1L << i) & state) != 0){
                var dispSig = disp[i][depth];
                if(!dispSig.info.isAssignableFromConcrete(typeInfo.id())){
                    res = res ^ mask;
                }
            }
        }
        return res;
    }

    private void buildMethodEntry(long state){
        var key = new FullStateKey(disp.length, state);
        if(layers.containsKey(key)) return;
        int methodIndex = 0;
        int m = 1;
        while ((state & m) == 0) {
            m = m << 1;
            methodIndex++;
        }
        layers.put(key, new StateLeaf(methodIndex));
    }

    private void buildGraph(long state, int depth) {
        var concretes = coordinator.getDispatchSubtypes(sig[depth]);
        var key = new FullStateKey(depth, state);
        if(layers.containsKey(key)) return;
        var layer = new FullStateKey[concretes.length];
        layers.put(key, new StateNode(nextFreeIndex, layer));
        nextFreeIndex += concretes.length;
        for (int i = 0; i < concretes.length; i++) {
            var concrete = concretes[i];
            var subState = filter(state, concrete, depth);
            var nDept = depth+1;
            layer[i] = new FullStateKey(nDept, subState);
            if(nDept < disp.length){
                buildGraph(subState, nDept);
            } else {
                buildMethodEntry(subState);
            }
        }
    }

    public int buildTable(FullStateKey key) {
        if(key.depth >= sig.length) {
            return ((StateLeaf)layers.get(key)).methodIndex;
        } else {
            var layer = ((StateNode)layers.get(key));
            var entries = layer.layer;
            var offset = layer.offset;
            for(int i = 0; i < entries.length; i++) {
                table[offset+i] = buildTable(entries[i]);
            }
            return offset;
        }
    }

    public int[] create() {
        //This approach fails for bigger tables
        // However we could use more than one long or a BigInteger
        // But fo now leave it at this
        if(disp.length > 64) throw new RuntimeException("Table To large");
        long curState = (1L << (disp.length-1))-1;
        buildGraph(curState,0);
        table = new int[nextFreeIndex];
        buildTable(new FullStateKey(0,curState));
        return table;
    }
}
