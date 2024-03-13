package nora.vm.nodes.method.template;

import com.oracle.truffle.api.CompilerAsserts;
import nora.vm.method.GenericMultiMethod;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.nodes.consts.ObjectNode;
import nora.vm.nodes.consts.TypeNode;
import nora.vm.nodes.string.ConcatNode;
import nora.vm.nodes.template.TemplateNode;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;
import nora.vm.types.TypeInfo;

import java.util.Arrays;

public class MultiMethodNode extends TemplateNode {
    private final String methodId;
    private final NoraNode[] generics;

    public MultiMethodNode(String methodId, NoraNode[] generics) {
        this.methodId = methodId;
        this.generics = generics;
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        var newGenerics = new Type[generics.length];
        for(int i = 0; i < newGenerics.length; i++){
            var spec = generics[i].specialise(frame);
            if(spec instanceof TypeNode tn){
                newGenerics[i] = tn.getType();
            } else {
                throw new IllegalStateException("Dynamic Methods not (yet) supported");
            }
        }
        GenericMultiMethod genericMultiMethod = getContext().getLoader().loadGenericMultiMethod(methodId);
        var multiMethod = genericMultiMethod.specialise(newGenerics);
        if(multiMethod.isExhaustive() && multiMethod.getNumMethods() == 1){
            return ConstNode.create(multiMethod.getMethod(0));
        } else {
            //Can we do this already in genericMultiMethod.specialise?
            //Does this catch anything, that the exhaustive check does not?
            var allLeaves = true;
            var argTypes = multiMethod.getArgumentTypes();
            var argTypeInfos = new TypeInfo[argTypes.length];
            for(int i = 0; i < argTypes.length; i++){
                if(!argTypes[i].isConcrete()) {
                    allLeaves = false;
                    argTypeInfos[i] = argTypes[i].info;
                    break;
                }
            }
            if(allLeaves) {
                return ConstNode.create(multiMethod.staticLookup(argTypeInfos));
            } else {
                return ConstNode.create(multiMethod);
            }
        }
    }

    @Override
    public String toString() {
        var gens = Arrays.stream(generics).map(Object::toString).toList();
        return methodId+"["+ String.join(",", gens) +"]";
    }
}
