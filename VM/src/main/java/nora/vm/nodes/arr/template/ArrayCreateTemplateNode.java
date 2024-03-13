package nora.vm.nodes.arr.template;

import com.oracle.truffle.api.nodes.UnexpectedResultException;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.arr.CreateObjectArray;
import nora.vm.nodes.arr.CreatePrimitiveArray;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.nodes.consts.TypeNode;
import nora.vm.nodes.template.TemplateNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;
import java.math.BigInteger;

public class ArrayCreateTemplateNode extends TemplateNode {
    @Children private NoraNode[] elems;
    @Child private NoraNode type;

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        var util = NoraVmContext.getTypeUtil(null);
        NoraNode elemType = type.specialise(frame);
        Type eType;
        if (elemType instanceof TypeNode tn){
            eType = tn.getType();
        } else {
            throw new IllegalStateException("Dynamic types are not supported");
        }
        var nElems = new NoraNode[elems.length];
        var allConst = true;
        var allCache = true;
        for(int i = 0; i < nElems.length; i++){
            var nElem = elems[i].specialise(frame);
            if(!(nElem instanceof ConstNode)) allConst = false;
            if(!(nElem instanceof CachedNode)) allCache = false;
            nElems[i] = nElem;
        }

        if(allConst) return ConstNode.create(genConstArray(nElems,eType));
        if(allCache) {
            for(int i = 0; i < nElems.length; i++){
                nElems[i] = ((CachedNode)nElems[i]).liftCache();
            }
        }
        NoraNode result;
        if(!util.isSimpleType(eType.info)){
            result = new CreateObjectArray(nElems,new Type(util.ArrayTypeInfo, new Type.TypeParameter[]{new Type.TypeParameter(Type.Variance.Co, eType)}));
        } else {
            result = new CreatePrimitiveArray(nElems, util.getKind(eType.info));
        }
        if(allCache) return new CacheNode(result);
        return result;
    }

    private Object genConstArray(NoraNode[] elems, Type elemType){
        var util = NoraVmContext.getTypeUtil(null);
        if(!util.isSimpleType(elemType.info)){
            return switch (util.getKind(elemType.info)){
                case BOOL -> createBoolArray(elems);
                case BYTE -> createByteArray(elems);
                case INT -> createIntArray(elems);
                case NUM -> createNumArray(elems);
                case STRING, FUNCTION, ARRAY, DATA, TYPE -> throw new RuntimeException("Not a primitive array type");
            };
        } else {
           var res = new Object[elems.length+1];
           res[0] = new Type(util.ArrayTypeInfo, new Type.TypeParameter[]{new Type.TypeParameter(Type.Variance.Co, elemType)});
           for(int i = 0; i < res.length; i++) res[i+1] = elems[i].execute(null);
           return res;
        }
    }

    private boolean[] createBoolArray(NoraNode[] nElems) {
        try{
            var res = new boolean[nElems.length];
            for (int i = 0; i < res.length; i++){
                res[i] = nElems[i].executeBoolean(null);
            }
            return res;
        } catch (UnexpectedResultException e) {
            throw new RuntimeException("Type Missmatch");
        }
    }
    private byte[] createByteArray(NoraNode[] nElems) {
        try{
            var res = new byte[nElems.length];
            for (int i = 0; i < res.length; i++){
                res[i] = nElems[i].executeByte(null);
            }
            return res;
        } catch (UnexpectedResultException e) {
            throw new RuntimeException("Type Missmatch");
        }
    }

    private int[] createIntArray(NoraNode[] nElems) {
        try{
            var res = new int[nElems.length];
            for (int i = 0; i < res.length; i++){
                res[i] = nElems[i].executeInt(null);
            }
            return res;
        } catch (UnexpectedResultException e) {
            throw new RuntimeException("Type Missmatch");
        }
    }

    private Object createNumArray(NoraNode[] nElems) {
        try{
            var util = NoraVmContext.getTypeUtil(null);
            var res = new Object[nElems.length+1];
            var allLong = true;
            res[0] = util.NumArrayType;
            for (int i = 0; i < res.length; i++){
                res[i+1] = nElems[i].execute(null);
                if(res[i+1] instanceof BigInteger) allLong = false;
            }
            if(allLong) {
                var longRes = new long[nElems.length];
                for (int i = 0; i < longRes.length; i++){
                    longRes[i] = (Long)res[i+1];
                }
                return longRes;
            }
            return res;
        } catch (ClassCastException e) {
            throw new RuntimeException("Type Missmatch");
        }
    }


}
