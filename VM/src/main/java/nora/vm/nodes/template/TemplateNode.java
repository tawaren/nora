package nora.vm.nodes.template;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import nora.vm.nodes.NoraNode;

//Just a node that must guarantee to return the same value each time
//  Probably means ignoring Frame
public abstract class TemplateNode extends NoraNode {

    @Override
    public Object execute(VirtualFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        throw new IllegalStateException("Template nodes must be eliminated before use");
    }

    @Override
    public NoraNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        throw new IllegalStateException("Template nodes must be eliminated before use");
    }
}
