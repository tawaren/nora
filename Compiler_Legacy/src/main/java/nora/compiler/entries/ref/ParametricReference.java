package nora.compiler.entries.ref;

import nora.compiler.entries.Instance;
import nora.compiler.resolver.ContextResolver;

import java.util.List;

//Todo: can we have a generics inferenz based on parameter types?
public class ParametricReference implements Resolvable<Instance> {
    private final Reference mainRef;
    private final List<ParametricReference> applies;

    public ParametricReference(Reference mainRef, List<ParametricReference> applies) {
        this.mainRef = mainRef;
        this.applies = applies;
    }

    @Override
    public Instance resolve(ContextResolver resolver) {
        List<Instance> args = null;
        if(applies != null) args = applies.stream().map(e -> e.resolve(resolver)).toList();
        return mainRef.resolveInstance(resolver, args);
    }

    public static ParametricReference primitive(String str){
        return new ParametricReference(new Reference(str), null);
    }

    public String getName(){
        return mainRef.getName();
    }

    public List<ParametricReference> getApplies() {
        return applies;
    }
}
