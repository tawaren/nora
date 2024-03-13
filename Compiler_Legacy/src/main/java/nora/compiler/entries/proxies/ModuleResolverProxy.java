package nora.compiler.entries.proxies;

import nora.compiler.entries.Definition;
import nora.compiler.entries.BuildUnitRoot;
import nora.compiler.entries.interfaces.Module;
import nora.compiler.entries.unresolved.RawModule;
import nora.compiler.resolver.PathTreeNode;

public class ModuleResolverProxy extends BaseProxy<Module, RawModule> implements Module, BuildUnitRoot {

    public ModuleResolverProxy(RawModule rawModule) {
        super(rawModule);
    }

    @Override
    public String getName() {
        return onAny(Module::getName, RawModule::getName);
    }

    @Override
    public PathTreeNode getNode() {
        return onAny(Module::getNode, RawModule::getNode);
    }

    @Override
    public Definition resolve(PathTreeNode root) {
        return doResolve(r -> r.resolve(root));
    }

    @Override
    public boolean validateAndInfer() {
        return onResolved(Module::validateAndInfer);
    }

    @Override
    public Module asModule() {
        var res = super.asModule();
        if(res == null) return this;
        return res;
    }
}
