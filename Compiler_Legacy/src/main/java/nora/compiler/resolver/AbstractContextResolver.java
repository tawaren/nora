package nora.compiler.resolver;

import nora.compiler.resolver.bindings.Binding;
import nora.compiler.resolver.bindings.DefBinding;
import nora.compiler.resolver.bindings.ValBinding;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class AbstractContextResolver implements ContextResolver{

    protected Map<String, Stack<Binding>> bindings = new HashMap<>();
    protected int nextFreeSlot = 0;

    @Override
    public <T> T withBinding(String name, BiFunction<ContextResolver, Binding, T> body){
        var binds = bindings.computeIfAbsent(name, n -> new Stack<>());
        var bind = new ValBinding(nextFreeSlot++);
        binds.push(bind);
        var res = body.apply(this, bind);
        binds.pop();
        return res;
    }

    @Override
    public Binding resolve(String name) {
        var bind = bindings.get(name);
        if(bind != null && !bind.isEmpty()) return bind.peek();
        return null;
    }

    @Override
    public <T> T withCapturingContext(List<String> argNames, Function<ContextResolver, T> body) {
        return body.apply(new ClosureContextResolver(argNames,this));
    }

    @Override
    public List<Binding> getAllCaptures() {
        return List.of();
    }
}
