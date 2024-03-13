package nora.vm.specTime;

import nora.vm.method.Method;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.arith.*;
import nora.vm.nodes.bitops.*;
import nora.vm.nodes.compare.*;
import nora.vm.nodes.equality.EqNode;
import nora.vm.nodes.hash_code.HashCodeNode;
import nora.vm.nodes.string.ConcatNode;
import nora.vm.nodes.string.LengthNode;
import nora.vm.nodes.string.SubstringNode;
import nora.vm.nodes.string.to_string.ToStringNode;

import java.util.HashMap;
import java.util.Map;

public class SpecialMethodRegistry {

    //this is fully static

    @FunctionalInterface
    public interface NodeBuilder{
        NoraNode build(NoraNode[] args);
    }

    private final Map<String, String> specialMethods = new HashMap<>();
    private final Map<String, NodeBuilder> builtins = new HashMap<>();

    public void init() {
        builtins.put("add", (args) -> AddNodeGen.create(args[0], args[1]));
        builtins.put("sub", (args) -> SubNodeGen.create(args[0], args[1]));
        builtins.put("mul", (args) -> MulNodeGen.create(args[0], args[1]));
        builtins.put("div", (args) -> DivNodeGen.create(args[0], args[1]));
        builtins.put("mod", (args) -> ModNodeGen.create(args[0], args[1]));
        builtins.put("and", (args) -> AndNodeGen.create(args[0], args[1]));
        builtins.put("or", (args) -> OrNodeGen.create(args[0], args[1]));
        builtins.put("xor", (args) -> XorNodeGen.create(args[0], args[1]));
        builtins.put("not", (args) -> NotNodeGen.create(args[0]));
        builtins.put("eq", (args) -> new EqNode(args[0], args[1]));
        builtins.put("cmp", (args) -> CmpNodeGen.create(args[0], args[1]));
        builtins.put("lt", (args) -> LtNodeGen.create(args[0], args[1]));
        builtins.put("lte", (args) -> LteNodeGen.create(args[0], args[1]));
        builtins.put("gt", (args) -> GtNodeGen.create(args[0], args[1]));
        builtins.put("gte", (args) -> GteNodeGen.create(args[0], args[1]));
        builtins.put("lsh", (args) -> LeftShiftNodeGen.create(args[0], args[1]));
        builtins.put("rsh", (args) -> RightShiftNodeGen.create(args[0], args[1]));
        builtins.put("substring", (args) -> new SubstringNode(args[0], args[1], args[2]));
        builtins.put("string_length", (args) -> new LengthNode(args[0]));
        builtins.put("string_concat", (args) -> new ConcatNode(args[0], args[1]));
        builtins.put("to_string", (args) -> new ToStringNode(args[0]));
        builtins.put("hash_code", (args) -> new HashCodeNode(args[0]));

    }

    public void registerBuiltin(String name, NodeBuilder builder){
        if(builtins.containsKey(name)) throw new RuntimeException("Builtin already registered");
        builtins.put(name, builder);
    }

    public void bindSpecialMethod(String method, String builtin){
        if(specialMethods.containsKey(method)) throw new RuntimeException("Special Method already bound");
        specialMethods.put(method, builtin);
    }

    //Returns null if not a special call
    public NoraNode resolveSpecialCall(Method meth, NoraNode[] args){
        var entry = specialMethods.get(meth.getMethodIdentifier());
        if(entry == null) return null;
        var builder = builtins.get(entry);
        if(builder == null) return null;
        return builder.build(args);
    }

    public NoraNode resolveBuiltin(String name, NoraNode[] args){
        var builder = builtins.get(name);
        if(builder == null) return null;
        return builder.build(args);
    }
}
