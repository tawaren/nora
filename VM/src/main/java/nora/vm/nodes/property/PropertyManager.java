package nora.vm.nodes.property;

import com.oracle.truffle.api.staticobject.StaticProperty;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.property.reader.NoraPropertyGetNode;
import nora.vm.nodes.property.setter.NoraPropertySetNode;

public interface PropertyManager {
    String getName();
    NoraPropertySetNode getSetter(NoraNode value);
    NoraPropertyGetNode getGetter();
}
