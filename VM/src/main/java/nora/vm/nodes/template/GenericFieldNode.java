package nora.vm.nodes.template;

import com.oracle.truffle.api.CompilerAsserts;
import nora.vm.nodes.*;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.nodes.consts.TypeNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.types.schemas.DataSchemaHandler;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

public class GenericFieldNode extends TemplateNode {
    private final String id;
    private final NoraNode[] genTypeExprs;
    private final int fieldIndex;
    private final NoraNode dataNode;

    public GenericFieldNode(String id, NoraNode[] genTypeExprs, NoraNode dataNode, int fieldIndex) {
        this.id = id;
        this.genTypeExprs = genTypeExprs;
        this.fieldIndex = fieldIndex;
        this.dataNode = dataNode;
    }

    //Note in reality it can be a subclass of this due to variance
    private NoraNode createNode(DataSchemaHandler handler, NoraNode target){
        CompilerAsserts.neverPartOfCompilation();
        var prop = handler.getProperty(fieldIndex).getGetter();
        var typ = prop.getPropertyType();
        return FieldNodeGen.create(fieldIndex, typ, NoraVmContext.getTypeUtil(null).getKind(typ.info),target);
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        Type[] generics = new Type[genTypeExprs.length];
        for (int i = 0; i < genTypeExprs.length; i++) {
            NoraNode gen = genTypeExprs[i];
            NoraNode newType = gen.specialise(frame);
            if (newType instanceof TypeNode tn){
                generics[i] = tn.getType();
            } else {
                throw new IllegalStateException("Dynamic types are not supported");
            }
        }
        NoraNode newTarget = dataNode.specialise(frame);
        if(newTarget instanceof ConstNode){
            var rt = newTarget.executeNoraData(null);
            var handler = rt.handler;
            var res = handler.getProperty(fieldIndex).getGetter().executeGenericGet(rt);
            return ConstNode.create(res);
        } else if(newTarget instanceof CachedNode cn){
            //Note in reality it can be a subclass of this due to variance
            var superHandler = NoraVmContext.getSchemaManager(null).getDataHandlerFor(id, generics);
            return new CacheNode(createNode(superHandler, cn.liftCache()));
        } else {
            //Note in reality it can be a subclass of this due to variance
            var superHandler = NoraVmContext.getSchemaManager(null).getDataHandlerFor(id, generics);
            return createNode(superHandler, newTarget);
        }

    }
}
