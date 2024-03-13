package nora.compiler.processing;

import nora.compiler.entries.*;
import nora.compiler.entries.DefInstance;
import nora.compiler.entries.Instance;
import nora.compiler.entries.resolved.Argument;
import nora.compiler.entries.resolved.Generic;
import nora.compiler.entries.resolved.Variance;
import nora.compiler.resolver.PathTreeNode;
import nora.compiler.resolver.bindings.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;


//TODO no more singleton
public class TypeCheckContext {
    private static TypeCheckContext initial = null;

    public static void init(PathTreeNode rootNode){
        if(initial == null){
            initial = new TypeCheckContext(rootNode);
        } else {
            throw  new RuntimeException("Already inited");
        }
    }

    public static TypeCheckContext get(){
        if(initial == null) throw  new RuntimeException("Not inited");
        return initial;
    }

    private final Instance intInst;
    private final Instance numInst;
    private final Instance boolInst;
    private final Instance stringInst;
    private final Definition typBase;
    private final Instance typInst;
    private final Definition funBase;
    private final Definition tupleBase;


    public TypeCheckContext(PathTreeNode rootNode) {
        numInst = new DefInstance(rootNode.navigatePath("nora.lang.Primitives.Num").getEntry(), List.of());
        intInst = new DefInstance(rootNode.navigatePath("nora.lang.Primitives.Int").getEntry(), List.of());
        boolInst = new DefInstance(rootNode.navigatePath("nora.lang.Primitives.Bool").getEntry(), List.of());
        stringInst = new DefInstance(rootNode.navigatePath("nora.lang.Primitives.String").getEntry(), List.of());
        typBase = rootNode.navigatePath("nora.lang.Primitives.Type").getEntry();
        typInst = new DefInstance(typBase, List.of());
        funBase = rootNode.navigatePath("nora.lang.Primitives.Function").getEntry();
        tupleBase = rootNode.navigatePath("nora.lang.Primitives.Tuple").getEntry();
    }

    //Todo: Later add tuple
    public static boolean isSpecialVarargDef(Definition f){
        return switch (f.getFullyQualifiedName()){
            case "nora.lang.Primitives::Function" -> true;
            case "nora.lang.Primitives::Tuple" -> true;
            default -> false;
        };
    }

    public static List<Variance> getVarargVariance(Definition f, List<Instance> arguments) {
        switch (f.getFullyQualifiedName()){
            case "nora.lang.Primitives::Function":
                var args = new LinkedList<Variance>(Collections.nCopies(arguments.size()-1,Variance.Contravariance));
                args.add(Variance.Covariance);
                return args;
            case "nora.lang.Primitives::Tuple": return arguments.stream().map(a -> Variance.Covariance).toList();
            default: return List.of();
        }
    }


    public static boolean isFunction(Definition f){
        return f.getFullyQualifiedName().equals("nora.lang.Primitives::Function");
    }

    public static boolean isTuple(Definition f){
        return f.getFullyQualifiedName().equals("nora.lang.Primitives::Tuple");
    }

    public Instance getNumType() {
        return numInst;
    }


    public Instance getIntType() {
        return intInst;
    }

    public Instance getBoolType() {
        return boolInst;
    }
    public Instance getStringType() {
        return stringInst;
    }
    public Instance getTypeType() {  return typInst;}

    public Instance getFunctionType(List<Instance> args, Instance ret) {
        var sig = Stream.concat(args.stream(), Stream.of(ret)).toList();
        return new DefInstance(funBase, sig);
    }

    //A special marker just for inferenz not used otherwise
    public Instance getTypeHintType(Instance typ){
        return new DefInstance(typBase, List.of(typ));
    }

    public boolean isTypeHintType(Instance typ){
        return typ instanceof DefInstance di && di.getBase().equals(typBase) && di.getArguments().size() == 1;
    }

    public Instance inferDataArgs(Instance binding, List<Instance> fields) {
        if(binding instanceof DefInstance di && di.getArguments() == null){
            var isTuple = isTuple(di.getBase());
            //If we got it delivered on a pallet use it
            if(retHint != null && retHint instanceof DefInstance ri && ri.getArguments() != null){
                return new DefInstance(di.getBase(), ri.getArguments());
            }
            var par = di.getBase().asWithArguments();
            if(par != null){
                //It has none, that is easy
                if(!isTuple && par.getGenerics().size() == 0) {
                    return new DefInstance(di.getBase(), List.of());
                }

                Instance[] argMatchers;
                List<Instance> argTypes;
                if(isTuple){
                    argMatchers = new Instance[fields.size()];
                    argTypes = fields;

                } else {
                    argMatchers = new Instance[par.getGenerics().size()];
                    argTypes = par.getArgs().stream().map(Argument::typ).toList();
                }
                int i = 0;
                for(Instance inst: argTypes){
                    inst.match(fields.get(i++), argMatchers);
                }
                //Find marked supertype of the evaluated markers
                i = 0;
                for(Generic g: par.getGenerics()){
                    if (argMatchers[i] == null && g.hint() != null) {
                        //Todo: I hate this conversion - a final version should mostly use arrays
                        argMatchers[i] = g.hint().substitute(Arrays.stream(argMatchers).toList());
                    }
                    if(argMatchers[i] == null) throw new InferenceFailedException();                    var bound = g.bound();
                    if(bound != null && bound.isTrait() && bound instanceof DefInstance di2){
                        argMatchers[i] = argMatchers[i].findSuperWithTrait(di2.getBase().asTrait());
                        if(argMatchers[i] == null) throw new InferenceFailedException();
                    }
                    i++;
                }
                return new DefInstance(di.getBase(), Arrays.stream(argMatchers).toList());
            }
        }
        throw new RuntimeException("Should not happen");
    }

    public Instance inferFunArgs(Instance binding) {
        if(binding instanceof DefInstance di && di.getArguments() == null){
            var par = di.getBase().asWithArguments();
            if(par != null){
                if(par.getGenerics().size() == 0) {
                    return new DefInstance(di.getBase(), List.of());
                }
                if(retHint instanceof DefInstance ri && isFunction(ri.getBase())) {
                    var sig = ri.getArguments();
                    var args = new ArrayList<>(sig.subList(0, sig.size() - 1));
                    var ret = sig.get(sig.size() - 1);
                    var argMatchers = new Instance[par.getGenerics().size()];
                    var argTypes = par.getArgs().stream().map(Argument::typ).toArray(Instance[]::new);
                    for (int i = 0; i < argTypes.length; i++) {
                        Instance inst = argTypes[i];
                        inst.match(args.get(i), argMatchers);
                        //We can not really do this in data that is why the special handling
                        if(args.get(i) == null)  args.set(i,inst);

                    }
                    if (ret != null && binding.isCallable()) {
                        di.getBase().asCallable().getReturnType().match(ret, argMatchers);
                    }
                    //Find marked supertype of the evaluated markers
                    int i = 0;
                    for (Generic g : par.getGenerics()) {
                        if (argMatchers[i] == null && g.hint() != null) {
                            //Todo: I hate this conversion - a final version should mostly use arrays
                            argMatchers[i] = g.hint().substitute(Arrays.stream(argMatchers).toList());
                        }
                        if(argMatchers[i] == null) throw new InferenceFailedException();
                        var bound = g.bound();
                        if (bound != null && bound.isTrait() && bound instanceof DefInstance di2) {
                            argMatchers[i] = argMatchers[i].findSuperWithTrait(di2.getBase().asTrait());
                            if (argMatchers[i] == null) throw new InferenceFailedException();
                        }
                        i++;
                    }
                    return new DefInstance(di.getBase(), Arrays.stream(argMatchers).toList());
                }
            }
        }
        throw new RuntimeException("Should not happen");
    }

    public Instance getTypeOfCallable(Instance binding) {
        if(binding instanceof DefInstance di){
            var cal = di.getBase().asCallable();
            var applies = di.getArguments();
            if(cal != null){
                var signature = new LinkedList<Instance>();
                for(Argument arg: cal.getArgs()){
                    signature.add(arg.typ().substitute(applies));
                }
                signature.add(cal.getReturnType().substitute(applies));
                return new DefInstance(funBase, signature);
            }
        }
        throw new RuntimeException("Ups");
    }

    private Map<Binding, Instance> bindings = new HashMap<>();
    private List<Instance> generics = new LinkedList<>();
    private Instance retHint = null;

    public <T> T withSignature(List<Generic> gens, List<Argument> args, Function<TypeCheckContext, T> f){
        var oldGenerics = generics;
        generics = new LinkedList<>();
        var oldBindings = bindings;
        bindings = new HashMap<>();
        Instance oldRetHint = retHint;
        retHint = null;
        //Add them
        int i = 0;
        for(Generic g: gens){
            if(bindings.put(new TypeBinding(i), typInst) != null){
                throw new RuntimeException("Ups non unique bindings");
            }
            generics.add(new GenericInstance(g.variance(), i++, g.bound()));
        }
        var res = withArgs(args, f);
        //Remove them
        bindings = oldBindings;
        generics = oldGenerics;
        retHint = oldRetHint;
        return res;

    }

    public List<Instance> getGenerics(){
        return generics;
    }

    public <T> T withBinding(Binding bind, Instance typ, Function<TypeCheckContext, T> f){
        return withBinding(bind,typ,false, f);
    }

    public <T> T withBinding(Binding bind, Instance typ, boolean shadowAllowed, Function<TypeCheckContext, T> f){
        if(bind instanceof DefBinding) throw new RuntimeException("Type context can only bind local definitions");
        var old = bindings.put(bind, typ);
        if(old != null && !shadowAllowed){
            throw new RuntimeException("Ups non unique bindings");
        }
        var res = f.apply(this);
        if(shadowAllowed && old != null){
            bindings.put(bind, old);
        } else if(bindings.remove(bind) == null){
            throw new RuntimeException("Ups non unique bindings");
        }
        return res;
    }


    private  <T> T withArgs(List<Argument> args, Function<TypeCheckContext, T> f){
        List<Instance> oldArguments = new LinkedList<>();
        for(int i = 0; i < args.size(); i++){
            var bind = new ArgBinding(i);
            if(args.get(i).typ() == null) throw new RuntimeException("");
            oldArguments.add(bindings.put(bind, args.get(i).typ()));
        }
        var res = f.apply(this);
        int i = 0;
        for(Instance inst: oldArguments){
            var bind = new ArgBinding(i++);
            bindings.put(bind,inst);
        }
        return res;
    }

    public <T> T inClosure(List<Binding> capture, List<Argument> args, Function<TypeCheckContext, T> f){
        return withoutReturnHint(c -> {
            List<Instance> oldCaptures = new LinkedList<>();
            List<Instance> newCaptures = new LinkedList<>();
            int i = 0;
            for(Binding bi:capture){
                var bind = new CaptureBinding(i++);
                oldCaptures.add(bindings.get(bind));
                newCaptures.add(bindings.get(bi));
            }
            i = 0;
            for(Instance inst: newCaptures){
                var bind = new CaptureBinding(i++);
                bindings.put(bind,inst);
            }
            var res = withArgs(args,f);
            i = 0;
            for(Instance inst: oldCaptures){
                var bind = new CaptureBinding(i++);
                bindings.put(bind,inst);
            }
            return res;
        });
    }

    public <T> T withReturnHint(Instance ret, Function<TypeCheckContext, T> f) {
        Instance oldHint = retHint;
        retHint = ret;
        var res = f.apply(this);
        retHint = oldHint;
        return res;
    }

    public <T> T withoutReturnHint(Function<TypeCheckContext, T> f) {
        Instance oldHint = retHint;
        retHint = null;
        var res = f.apply(this);
        retHint = oldHint;
        return res;
    }

    public Instance getRetHint() {
        return retHint;
    }

    public Instance getBinding(Binding binding){
        if(binding instanceof DefBinding) throw new RuntimeException("Type context can only bind local definitions");
        var res = bindings.get(binding);
        if(res == null) throw new RuntimeException("Unbound binding");
        return res;
    }

    //Todo: I do not like this
    //      We need patternmatching
    private static List<String > tupleFieldNames = List.of(
            "first", "second", "third", "fourth", "fifth", "sixth", "seventh"
    );

    public static String getTupleFieldName(int index){
        if(index < tupleFieldNames.size()) return tupleFieldNames.get(index);
        return "_"+index+"_";
    }

    public static int getTupleFieldIndex(String name){
        var res = tupleFieldNames.indexOf(name);
        if(res != -1) return res;
        try {
            return Integer.parseInt(name.substring(1, name.length()-1));
        }catch (NumberFormatException ex){
            throw new RuntimeException("Unknown field");
        }
    }

}
