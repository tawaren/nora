package nora.compiler.entries.unresolved;

import nora.compiler.entries.DefInstance;
import nora.compiler.entries.Instance;
import nora.compiler.entries.interfaces.Data;
import nora.compiler.entries.proxies.DataResolverProxy;
import nora.compiler.entries.ref.ParametricReference;
import nora.compiler.entries.resolved.Generic;
import nora.compiler.entries.resolved.ResolvedData;
import nora.compiler.resolver.ContextResolver;

import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class RawData extends Unresolved<Data> {

    private final boolean sealed;
    private final boolean abstr;
    private Integer typId;
    private final String handler;

    private final ParametricReference parent;
    private final List<RawGeneric> generics;
    private final List<ParametricReference> traits;
    private final List<RawArgument> fields;
    private Data primitiveChild = null;
    private final List<Data> children = new LinkedList<>();


    public RawData(String fullyQualifiedName, String handler, boolean sealed, boolean abstr, Integer typId, List<RawGeneric> generics, ParametricReference parent, LinkedList<ParametricReference> traits, List<RawArgument> arguments) {
        super(fullyQualifiedName);
        this.handler = handler;
        this.sealed = sealed;
        this.abstr = abstr;
        this.typId = typId;
        this.parent = parent;
        this.generics = generics;
        this.traits = traits;
        this.fields = arguments;
    }

    public void addChild(Data child){
        children.add(child);
    }

    public void addPrimitiveChild(Data child){
        if(typId != null) throw new RuntimeException("Only one primitive Child allowed");
        if(primitiveChild != null) throw new RuntimeException("Only one primitive Child allowed");
        primitiveChild = child;
        typId = child.getTypId();
    }

    @Override
    public Data resolve(ContextResolver resolver) {
        Instance newParent = null;
        List<Generic> rootGenerics = null;
        var dataResolver = resolver.dataContextResolver(rootGenerics);
        List<Generic> nGeneric = generics.stream().map(g -> {
            var res = g.resolve(resolver);
            dataResolver.addGeneric(res);
            return res;
        }).toList();
        if(parent != null) newParent = parent.resolve(dataResolver);
        var nTraits = traits.stream().map(m -> m.resolve(dataResolver)).collect(Collectors.toSet());
        var nFields = fields.stream().map(f -> f.resolve(dataResolver)).toList();
        List<Data> nChildren = children;
        if(primitiveChild != null){
            nChildren = new LinkedList<>();
            nChildren.add(primitiveChild);
            nChildren.addAll(children);
        }
        var self = new ResolvedData(getFullyQualifiedName(), handler, sealed, abstr, typId, newParent, nGeneric, nTraits, nFields, nChildren);
        if(newParent instanceof DefInstance di) {
            if(typId != null){
                di.getBase().asData().addPrimitiveChild(self);
            } else {
                di.getBase().asData().addChild(self);
            }
        }
        return self;
    }

    public boolean isSealed() {
        return sealed;
    }

    public boolean isAbstr() {
        return abstr;
    }

    public Integer getTypId() {
        return typId;
    }

    public int numGenerics() {
        return generics.size();
    }

    @Override
    public Data asResolverProxy() {
        return new DataResolverProxy(this);
    }

    @Override
    public Data asData() {
        return asResolverProxy();
    }
}
