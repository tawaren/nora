package nora.compiler.resolver;

import nora.compiler.entries.Instance;
import nora.compiler.entries.resolved.Generic;
import nora.compiler.entries.resolved.Variance;
import nora.compiler.resolver.bindings.ArgBinding;
import nora.compiler.resolver.bindings.Binding;
import nora.compiler.resolver.bindings.CaptureBinding;
import nora.compiler.resolver.bindings.DefBinding;

import java.util.*;

public class ClosureContextResolver extends AbstractContextResolver{
    private final ContextResolver parent;

    ClosureContextResolver(List<String> argNames, ContextResolver parent) {
        int argCount = 0;
        for(String arg: argNames){
            var binds = bindings.computeIfAbsent(arg, n -> new Stack<>());
            binds.push(new ArgBinding(argCount++));
        }
        this.parent = parent;
    }

    private List<Binding> captures = new LinkedList<>();
    private Map<Binding, CaptureBinding> inner = new HashMap<>();

    @Override
    public Binding resolve(String name) {
        var binds = super.resolve(name);
        if(binds != null) return binds;
        var captured = parent.resolve(name);
        if(captured instanceof DefBinding) return captured;
        var res = inner.get(captured);
        if(res != null) return res;
        var capture = new CaptureBinding(captures.size());
        captures.add(captured);
        inner.put(captured, capture);
        return capture;
    }

    @Override
    public List<Binding> getAllCaptures() {
        return captures;
    }

    @Override
    public GenericContextResolver methodContextResolver(List<String> argNames) {
        return parent.methodContextResolver(argNames);
    }

    @Override
    public GenericContextResolver dataContextResolver(List<Generic> rootGenerics) {
        return parent.dataContextResolver(rootGenerics);
    }

    @Override
    public Instance getGenericBound(int arg) {
        return parent.getGenericBound(arg);
    }

    @Override
    public Variance getGenericVariance(int arg) {
        return parent.getGenericVariance(arg);
    }
}
