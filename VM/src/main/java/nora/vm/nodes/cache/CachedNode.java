package nora.vm.nodes.cache;

import nora.vm.nodes.NoraNode;

//Marker for all nodes that resolve to a constant after one execution
public abstract class CachedNode extends NoraNode {
    public abstract NoraNode liftCache();

    @Override
    public int complexity() {
        return 1;
    }
}
