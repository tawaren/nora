package nora.compiler.entries.unresolved;

import nora.compiler.entries.*;
import nora.compiler.entries.interfaces.Module;
import nora.compiler.entries.proxies.BaseProxy;
import nora.compiler.entries.proxies.CaseMethodResolverProxy;
import nora.compiler.entries.proxies.ModuleResolverProxy;
import nora.compiler.entries.resolved.ResolvedModule;
import nora.compiler.resolver.ContextResolver;
import nora.compiler.resolver.ImportResolver;
import nora.compiler.resolver.PathTreeNode;
import nora.compiler.resolver.RawContextResolver;

import java.util.LinkedList;
import java.util.List;

public final class RawModule extends Unresolved<Module> implements BuildUnitRoot {
    private PathTreeNode moduleNode = null;
    private final List<Import> imports = new LinkedList<>();
    //private final List<RawMarking> markings = new LinkedList<>();
    private final List<CaseMethodResolverProxy> caseMethods = new LinkedList<>();

    public RawModule(String fullyQualifiedName) {
        super(fullyQualifiedName);
    }

    public void setModuleNode(PathTreeNode moduleNode) {
        if(this.moduleNode != null) throw new RuntimeException("Already set");
        this.moduleNode = moduleNode;
    }

    public void addImport(Import imp) {
        imports.add(imp);
    }


    public PathTreeNode addData(String name, RawData data) {
        return moduleNode.addAndDescend(name, data.asData());
    }

    public PathTreeNode addMultiMethod(String name, RawMultiMethod fun) {
        return moduleNode.addAndDescend(name, fun.asMultiMethod());
    }

    public PathTreeNode addFunction(String name, RawFunction fun) {
        return moduleNode.addAndDescend(name, fun.asFunction());
    }

    public void addCaseMethod(String name, RawCaseMethod method) {
        var proxy = method.asResolverProxy();
        moduleNode.addAndDescend(name, proxy);
        caseMethods.add(proxy);
    }

    public PathTreeNode addTrait(String name, RawTrait trait) {
        return moduleNode.addAndDescend(name, trait.asTrait());
    }

    /*public void addMarking(RawMarking marking) {
        markings.add(marking);
    }*/

    public String getName(){
        return moduleNode.getName();
    }

    public PathTreeNode getNode(){
        return moduleNode;
    }

    public ResolvedModule resolve(PathTreeNode root) {
        if(root.getEntry() != ROOT) throw new RuntimeException("Needs Rootresolver");
        var baseResolver = new ImportResolver(root);
        //Add imports
        for(Import imp:imports){
            baseResolver.addImport(imp.importString());
        }
        //Add members
        for(String name:moduleNode.listEntries()){
            var entry = moduleNode.getChild(name);
            baseResolver.addDefinition(entry);
        }
        var baseContext = new RawContextResolver(baseResolver);
        return resolve(baseContext);
    }

    @Override
    public ResolvedModule resolve(ContextResolver resolver) {
        for(String name:moduleNode.listEntries()){
            var entry = moduleNode.getChild(name);
            var def = entry.getEntry();
            if(!(def instanceof BaseProxy<?,?>)) throw new RuntimeException("Need container for save inline resolving");
            //we can ignore res as it is sored in DefinitionContainer
            def.resolve(resolver);
        }

        //Resolve the cases for the multi methods and add them to the Method
        caseMethods.stream().map(cm -> cm.resolve(resolver)).forEach(cm -> {
            var multiInst = cm.getMultiMethod();
            if(multiInst instanceof DefInstance di){
                di.getBase().asMultiMethod().addCaseMethod(cm);
            } else {
                throw new RuntimeException("Can not resolve Multi Method");
            }
        });

        //Applies all markings
        //markings.stream().map(m -> m.resolve(resolver)).forEach(Marking::execute);
        return new ResolvedModule(getFullyQualifiedName(), moduleNode);
    }

    @Override
    public Module asResolverProxy() {
        return new ModuleResolverProxy(this);
    }

    @Override
    public Module asModule() {
        return asResolverProxy();
    }
}
