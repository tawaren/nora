package nora.vm.nodes.template;

import com.oracle.truffle.api.CompilerAsserts;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.nodes.consts.TypeNode;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.pattern.TypePattern;

public class TypePatternNode extends TemplateNode{
    private final TypePattern pat;

    public TypePatternNode(TypePattern pat) {
        this.pat = pat;
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        return ConstNode.create(pat.buildType(frame.getGenerics()));
    }

    @Override
    public String toString() {
        return "TypePatternNode("+pat+")";
    }
}
