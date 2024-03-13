package nora.vm.types;

import nora.vm.loading.Loader;
import nora.vm.loading.TypeFamilyLoader;
import nora.vm.runtime.data.RuntimeData;

import java.util.HashMap;
import java.util.Map;


//Todo: Make instance
//This class has all the index specific stuff extracted
//  It is static to ensure good inlining
public class DispatchCoordinator {

    public record TypeNode(int start, int length){}
    public record TypeFamily(TypeNode[] all, TypeInfo[] concrete){}

    public final Map<Integer, TypeFamily> families = new HashMap<>();

    //Todo: This will need help from some type family files
    //      No Lazy loading an sparse stuff will be supported at the start

    //is called as soon as a create node in an executing method is created
    // However, no 100% certainty of existence as it may be in an if
    // Delaying this to runtime however is illadvicable
    //  it is just an int that the DispatchCoordinator can use to identify the type or compute dispatchIndexes
    //  This gets the Loader as it is the only one that has it
    //  But it is the right place anyway
    public int assignIndex(Type t, Loader loader){
        assert t.isConcrete();
        var family = families.computeIfAbsent(t.info.cat(), c -> construct(loader.loadFullTypeFamily(c)));
        return family.all[t.info.id()].start;
    }

    //computes an offset such as all the concrete subtypes start at that index
    // and they have continous indexes for now
    // could be lowered
    public int getRootTypeInfo(Type argRootType) {
        var family = families.get(argRootType.info.cat());
        return family.all[argRootType.info.id()].start;
    }

    //Lists all types that a dispatch table for the argument type must support
    // Note: the array is ordered according to dispatch index
    //       forall i : getDispatchIndex(getDispatchSubtypes(type)[i], getRootTypeInfo(type)) == i
    public TypeInfo[] getDispatchSubtypes(Type type) {
        var family = families.get(type.info.cat());
        var node =  family.all[type.info.id()];
        var res = new TypeInfo[node.length];
        System.arraycopy(family.concrete,node.start,res,0,node.length);
        return res;
    }

    //Todo: For Truffle this must be a Node so it can be specialised
    //
    // rootInfo = getRootTypeInfo(type)
    //  where extractType(arg).isSubType(type)
    public static int getDispatchIndex(Object arg, int rootInfo) {
        if(arg instanceof RuntimeData rd) return rd.dispatcherIndex - rootInfo;
        return 0;
    }

    private TypeFamily construct(TypeFamilyLoader.TypeData[] allTypes){
        var allNodes = new TypeNode[allTypes.length];
        //Do the concrete Nodes;
        var concretes = 0;
        for(int i = 0; i < allTypes.length; i++){
            if(allTypes[i].info().isConcrete()) allNodes[i] = new TypeNode(concretes++,1);
        }

        var concreteTypes = new TypeInfo[concretes];
        concretes = 0;
        //Do the non concrete nodes + the concreate types
        for(int i = 0; i < allTypes.length; i++){
            if(allTypes[i].info().isConcrete()){
                concreteTypes[concretes++] = allTypes[i].info();
            } else {
                var info = allTypes[i].info();
                var fullLen  = info.end - info.start;
                var fullStart = (i - fullLen);
                var concreteStart = allNodes[fullStart].start;
                var concreteLen = (concretes - concreteStart);
                allNodes[i] = new TypeNode(concreteStart,concreteLen);

                /*TypeNode endNode = allNodes[allTypes[i].info().end];
                s = Math.max(s,i+1);
                while (allNodes[s] == null) s++; //scans ahead to the next concrete
                var startNode = allNodes[s];
                allNodes[i] = new TypeNode(startNode.start,endNode.start-startNode.start+1);
                s=i+1;
                */
            }
        }

        return new TypeFamily(allNodes, concreteTypes);
    }
}
