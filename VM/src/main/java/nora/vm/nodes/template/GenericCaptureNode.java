package nora.vm.nodes.template;

import com.oracle.truffle.api.CompilerAsserts;
import nora.vm.nodes.CaptureNode;
import nora.vm.nodes.NoraNode;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

public class GenericCaptureNode extends TemplateNode {
    private final int fieldIndex;

    public GenericCaptureNode(int fieldIndex) {
        this.fieldIndex = fieldIndex;
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        Type[] captures = frame.getCaptureTypes();
        var handler = getContext().getSchemaManager().getClosureHandlerFor(captures);
        var prop = handler.getCapture(fieldIndex).getGetter();
        return new CaptureNode(prop);
    }
}
