package nora.vm.nodes.method.template;

import com.oracle.truffle.api.CompilerAsserts;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.nodes.consts.ObjectNode;
import nora.vm.nodes.consts.TypeNode;
import nora.vm.nodes.template.TemplateNode;
import nora.vm.runtime.MethodCreator;
import nora.vm.runtime.NoraVmContext;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

import java.util.Arrays;

public class MethodNode extends TemplateNode {
    private final String methodId;
    private final NoraNode[] generics;

    public MethodNode(String methodId, NoraNode[] generics) {
        this.methodId = methodId;
        this.generics = generics;
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        MethodCreator methodFactory = NoraVmContext.getMethodFactory(null);
        var newGenerics = new Type[generics.length];
        for(int i = 0; i < newGenerics.length; i++){
            var spec = generics[i].specialise(frame);
            if(spec instanceof TypeNode tn){
                newGenerics[i] = tn.getType();
            } else {
                throw new IllegalStateException("Dynamic Methods are not supported");
            }
        }
        return ConstNode.create(methodFactory.create(methodId, newGenerics));
    }

    @Override
    public String toString() {
        var args = Arrays.stream(generics).map(Object::toString).toList();
        return methodId+"["+String.join(",", args) +"]";
    }
}
