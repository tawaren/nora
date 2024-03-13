package nora.compiler.entries.resolved;

import nora.compiler.resolver.PathTreeNode;
import nora.compiler.entries.interfaces.Module;


public final class ResolvedModule extends ResolvedDefinition implements Module {
    private final PathTreeNode moduleNode;

    public ResolvedModule(String fullyQualifiedName, PathTreeNode moduleNode) {
        super(fullyQualifiedName);
        this.moduleNode = moduleNode;
    }

    @Override
    public String getName(){
        return moduleNode.getName();
    }

    @Override
    public PathTreeNode getNode(){
        return moduleNode;
    }

    @Override
    public Kind getKind() {
        return Kind.Module;
    }

    @Override
    public Module asModule() {
        return this;
    }

    private Boolean valid = null;
    private boolean cacheValidationRes(boolean res){
        if(!res) throw new RuntimeException("Validation failed"); //Better for now
        valid = res;
        return res;
    }

    @Override
    public boolean validateAndInfer() {
        if(valid != null) return valid;
        //Prevents cycles
        valid = false;
        for(String name:moduleNode.listEntries()){
            var entry = moduleNode.getChild(name);
            if(!entry.getEntry().validateAndInfer()) cacheValidationRes(false);
        }
        return cacheValidationRes(true);
    }
}
