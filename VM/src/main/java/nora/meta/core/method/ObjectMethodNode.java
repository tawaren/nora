package nora.meta.core.method;

import com.oracle.truffle.api.CompilerAsserts;
import nora.meta.core.PublicObjectLayout;
import nora.vm.method.GenericMultiMethod;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.nodes.consts.TypeNode;
import nora.vm.nodes.template.TemplateNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;
import nora.vm.types.TypeInfo;

import java.util.Arrays;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ObjectMethodNode extends TemplateNode {
    private final String methodId;
    private final String objectId;
    private final NoraNode[] objectGenerics;
    private final NoraNode[] methodGenerics;

    public ObjectMethodNode(String methodId, String objectId, NoraNode[] objectGenerics, NoraNode[] methodGenerics) {
        this.methodId = methodId;
        this.objectId = objectId;
        this.objectGenerics = objectGenerics;
        this.methodGenerics = methodGenerics;
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        //Todo: Figure out if we can resolve directly to Method or need Object Method
        //      Direct Method when target is not a PublicObjectHandled thing
        CompilerAsserts.neverPartOfCompilation();
        Type[] objGens = new Type[objectGenerics.length];
        for (int i = 0; i < objectGenerics.length; i++) {
            NoraNode gen = objectGenerics[i];
            NoraNode newType = gen.specialise(frame);
            if (newType instanceof TypeNode tn){
                objGens[i] = tn.getType();
            } else {
                throw new IllegalStateException("Dynamic types are not supported");
            }
        }
        Type[] methGens = new Type[methodGenerics.length];
        for (int i = 0; i < methodGenerics.length; i++) {
            NoraNode gen = methodGenerics[i];
            NoraNode newType = gen.specialise(frame);
            if (newType instanceof TypeNode tn){
                methGens[i] = tn.getType();
            } else {
                throw new IllegalStateException("Dynamic types are not supported");
            }
        }
        var rootHandler = NoraVmContext.getSchemaManager(null).getDataHandlerFor(objectId, objGens);
        if(rootHandler instanceof PublicObjectLayout pol){
            //Todo: we need to access disk entry for methodId
            return ConstNode.create(new ObjectMethod(methodId,pol,methGens));
        } else {
            var fullSig = new Type[objGens.length+methGens.length];
            System.arraycopy(objGens,0, fullSig, 0, objGens.length);
            System.arraycopy(methGens,0, fullSig, objGens.length, methGens.length);
            //Just a Method call with different Syntax
            return ConstNode.create(NoraVmContext.getMethodFactory(null).create(methodId,fullSig));
        }
    }

    @Override
    public String toString() {
        var gens = Stream.concat(Arrays.stream(objectGenerics),Arrays.stream(methodGenerics)).map(Object::toString).toList();
        return methodId+"["+ String.join(",", gens) +"]";
    }
}
