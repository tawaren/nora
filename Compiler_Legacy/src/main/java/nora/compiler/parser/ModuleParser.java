package nora.compiler.parser;

import nora.compiler.entries.*;
import nora.compiler.ModuleBaseVisitor;
import nora.compiler.ModuleLexer;
import nora.compiler.entries.exprs.*;
import nora.compiler.entries.exprs.resolved.BindingExpr;
import nora.compiler.entries.interfaces.Function;
import nora.compiler.entries.ref.ParametricReference;
import nora.compiler.entries.ref.Reference;
import nora.compiler.entries.resolved.Variance;
import nora.compiler.entries.unresolved.*;
import nora.compiler.entries.unresolved.RawModule;
import nora.compiler.processing.BuiltinMapper;
import nora.compiler.processing.TypeCheckContext;
import nora.compiler.resolver.PathTreeNode;
import nora.compiler.resolver.bindings.ArgBinding;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.misc.Pair;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

public class ModuleParser extends ModuleBaseVisitor<Object> {

    private final RawModule module;

    public ModuleParser(PathTreeNode  parentNode, String name, String fullyQualifiedName) {
        this.module = new RawModule(fullyQualifiedName);
        var moduleNode = parentNode.addAndDescend(name,module.asModule());
        this.module.setModuleNode(moduleNode);
    }

    public static void parseModule(Path moduleFile, String moduleName, String fullyQualifiedName, PathTreeNode parentNode){
        try {
            var lexer = new ModuleLexer(CharStreams.fromPath(moduleFile));
            var tokens = new CommonTokenStream(lexer);
            var parser = new nora.compiler.ModuleParser(tokens);
            var parsed = parser.file_def();
            var vis = new ModuleParser(parentNode, moduleName, fullyQualifiedName);
            vis.visitFile_def(parsed);
        } catch (Exception e){
            //Todo: Better error Handling
            throw new RuntimeException(moduleFile.toString(),e);
        }
    }

    @Override
    public Import visitImport_def(nora.compiler.ModuleParser.Import_defContext ctx) {
        var imp = new Import(ctx.import_path().getText());
        module.addImport(imp);
        return imp;
    }

    @Override
    public RawModule visitModule_def(nora.compiler.ModuleParser.Module_defContext ctx) {
        if(!ctx.id().getText().equals(module.getName())) throw new RuntimeException("Module must be in a file with the same name");
        ctx.definition().forEach(this::visitDefinition);
        return module;
    }

    private String curDef;
    private List<ParametricReference> curGenerics;
    //Todo: this is not safe in case it is shadowed
    //      but this will most likely result in error
    private void setCurGenerics(List<RawGeneric> gens){
        curGenerics = new LinkedList<>();
        for(RawGeneric gen: gens){
            curGenerics.add(new ParametricReference(new Reference(gen.name()), null));
        }
    }

    Variance defaultVariance = null;

    @Override
    public RawData visitType_def(nora.compiler.ModuleParser.Type_defContext ctx) {
        var name = ctx.id().getText();
        curDef = name;
        var fullName = module.getFullyQualifiedName()+"::"+name;
        var sealed = ctx.SEALED() != null;
        var abstr = ctx.ABSTRACT() != null;
        defaultVariance = Variance.Covariance;
        List<RawGeneric> generics = new LinkedList<>();
        if(ctx.generics_def() != null){
            ctx.generics_def().variant_generic_def().forEach(c -> generics.add(visitVariant_generic_def(c)));
        }
        setCurGenerics(generics);

        ParametricReference parent = null;
        if(ctx.super_ != null) parent = visitParametric_ref(ctx.super_ );

        var traits = new LinkedList<ParametricReference>();
        if(ctx.traits() !=  null){
            ctx.traits().parametric_ref().stream().map(this::visitParametric_ref).forEach(traits::add);
        }


        List<RawArgument> arguments = new LinkedList<>();
        if(ctx.argument_def() != null){
            ctx.argument_def().forEach(a -> arguments.add(visitArgument_def(a)));
        }

        Integer typId = null;
        if(ctx.type_id_annotation() != null) typId = Integer.parseInt(ctx.type_id_annotation().int_().getText());
        String handler = null;
        if(ctx.layout_handler_annotation() != null) handler = ctx.layout_handler_annotation().id().getText();
        var data = new RawData(fullName, handler, sealed, abstr, typId, generics, parent, traits, arguments);
        module.addData(name, data);
        return data;
    }

    @Override
    public RawGeneric visitGeneric_hint_def(nora.compiler.ModuleParser.Generic_hint_defContext ctx) {
        var base = visitGeneric_def(ctx.generic_def());
        if(ctx.parametric_ref() == null) return base;
        var hint = visitParametric_ref(ctx.parametric_ref());
        return new RawGeneric(base.name(), base.variance(), base.typ(), hint);
    }

    @Override
    public RawGeneric visitGeneric_def(nora.compiler.ModuleParser.Generic_defContext ctx) {
        ParametricReference bound = null;
        if(ctx.parametric_ref() != null) bound = visitParametric_ref(ctx.parametric_ref());
        return new RawGeneric(ctx.id().getText(), null, bound, null);
    }

    @Override
    public RawGeneric visitVariant_generic_def(nora.compiler.ModuleParser.Variant_generic_defContext ctx) {
        ParametricReference bound = null;
        var generic_def = ctx.generic_def();
        if(generic_def.parametric_ref() != null) bound = visitParametric_ref(generic_def.parametric_ref());
        Variance variance = defaultVariance;
        if(ctx.variance() != null) variance = visitVariance(ctx.variance());
        return new RawGeneric(generic_def.id().getText(), variance, bound, null);
    }

    @Override
    public Variance visitVariance(nora.compiler.ModuleParser.VarianceContext ctx) {
        if(ctx.ADD() != null) return Variance.Covariance;
        if(ctx.SUB() != null) return Variance.Contravariance;
        if(ctx.INV() != null) return Variance.Invariance;
        return defaultVariance;
    }

    @Override
    public RawArgument visitArgument_def(nora.compiler.ModuleParser.Argument_defContext ctx) {
        return new RawArgument(ctx.id().getText(), visitParametric_ref(ctx.parametric_ref()));
    }

    @Override
    public RawTrait visitTrait_def(nora.compiler.ModuleParser.Trait_defContext ctx) {
        var name = ctx.id().getText();
        curDef = name;
        var fullName = module.getFullyQualifiedName()+"::"+name;
        defaultVariance = Variance.Covariance;
        List<RawGeneric> generics = new LinkedList<>();
        if(ctx.generics_def() != null){
            ctx.generics_def().variant_generic_def().forEach(c -> generics.add(visitVariant_generic_def(c)));
        }
        setCurGenerics(generics);

        var traits = new LinkedList<ParametricReference>();
        if(ctx.traits() !=  null){
            ctx.traits().parametric_ref().stream().map(this::visitParametric_ref).forEach(traits::add);
        }



        RawTrait trait = new RawTrait(fullName, generics, traits);;
        module.addTrait(name, trait);
        return trait;
    }

    /*
    @Override
    public RawMarking visitMarking_def(nora.compiler.ModuleParser.Marking_defContext ctx) {
        var ref = visitReference(ctx.reference());
        var marking = new RawMarking(ref);
        ctx.traits().reference().forEach(r -> marking.addTrait(visitReference(r)));
        module.addMarking(marking);
        return marking;
    }*/

    private boolean isTail = false;
    private <T> T notInTailPos(Supplier<T> f){
        var oldTail = isTail;
        isTail = false;
        var res = f.get();
        isTail = oldTail;
        return res;
    }

    private boolean needsCtxTailRecVersion = false;
    private boolean isTailRec = false;
    private <T> T withPotentialTailCalls(Supplier<T> f){
        isTailRec = false;
        needsCtxTailRecVersion = false;
        var oldTail = isTail;
        isTail = true;
        var res = f.get();
        isTail = oldTail;
        return res;
    }

    private Function.RecursionMode getMode(){
        if(!isTailRec) return Function.RecursionMode.NotRecursive;
        if(!needsCtxTailRecVersion) return Function.RecursionMode.CallRecursive;
        return Function.RecursionMode.ContextRecursive;
    }

    @Override
    public RawFunction visitFunction_def(nora.compiler.ModuleParser.Function_defContext ctx) {
        var name = ctx.name.getText();
        curDef = name;
        var fullName = module.getFullyQualifiedName()+"::"+name;
        if(ctx.prim != null) BuiltinMapper.addMapping(fullName, ctx.prim.getText());
        defaultVariance = null;
        List<RawGeneric> generics = List.of();
        if(ctx.generics_hint_def() != null){
            generics = ctx.generics_hint_def().generic_hint_def().stream().map(this::visitGeneric_hint_def).toList();
        }
        setCurGenerics(generics);

        ParametricReference ret = null;
        if(ctx.parametric_ref() != null) ret = visitParametric_ref(ctx.parametric_ref());

        List<RawArgument> args = new LinkedList<>();
        if(ctx.arguments_def() != null){
            args.addAll(ctx.arguments_def().argument_def().stream().map(this::visitArgument_def).toList());
        }

        Expr exp = withPotentialTailCalls( () ->{
            if(ctx.PRIMITIVE() == null){
                return visitExpr(ctx.expr());
            } else {
                List<Expr> primArgs = new LinkedList<>();
                notInTailPos( () -> {
                    for(int i = 0; i < args.size(); i++){
                        primArgs.add(new BindingExpr(new ArgBinding(i)));
                    }
                    return null;
                });
                return new PrimitiveExpr(ctx.prim.getText(),primArgs);
            }
        });

        var method = new RawFunction(fullName, getMode(), generics, args, ret, exp);

        module.addFunction(name, method);
        return method;
    }

    @Override
    public RawMultiMethod visitMulti_method_def(nora.compiler.ModuleParser.Multi_method_defContext ctx) {
        var name = ctx.id().getText();
        curDef = name;
        var fullName = module.getFullyQualifiedName()+"::"+name;
        var sealed = ctx.SEALED() != null;
        var partial = ctx.PARTIAL() != null;
        defaultVariance = null;
        List<RawGeneric> generics = new LinkedList<>();
        if(ctx.generics_hint_def() != null){
            //Todo: here we need to grab the link when suported
            ctx.generics_hint_def().generic_hint_def().forEach(c -> generics.add(visitGeneric_hint_def(c)));
        }
        setCurGenerics(generics);

        var ret = visitParametric_ref(ctx.parametric_ref());
        List<RawArgument> dyn = new LinkedList<>();
        if(ctx.dynamic != null) ctx.dynamic.argument_def().stream().map(this::visitArgument_def).forEach(dyn::add);
        List<RawArgument> stat = new LinkedList<>();
        if(ctx.static_ != null) ctx.static_.argument_def().stream().map(this::visitArgument_def).forEach(stat::add);


        var fun = new RawMultiMethod(fullName, sealed, partial, generics, dyn, stat, ret);
        module.addMultiMethod(name, fun);
        return fun;
    }



    private int antiConflictPostFix = 0;

    @Override
    public RawCaseMethod visitCase_method_def(nora.compiler.ModuleParser.Case_method_defContext ctx) {
        var multiMethod = visitParametric_ref(ctx.multi);
        String name;
        var nameAnnot = ctx.case_annotations().name_annotation();
        if(nameAnnot != null){
            name = nameAnnot.id().getText();
        } else {
            name = multiMethod.getName()+"__"+(antiConflictPostFix++);
        }
        curDef = name;
        var fullName = module.getFullyQualifiedName()+"::"+name;
        if(ctx.prim != null) BuiltinMapper.addMapping(fullName, ctx.prim.getText());
        defaultVariance = null;
        List<RawGeneric> generics = new LinkedList<>();
        if(ctx.generics_hint_def() != null){
            ctx.generics_hint_def().generic_hint_def().forEach(c -> generics.add(visitGeneric_hint_def(c)));
        }
        setCurGenerics(generics);

        ParametricReference ret = null;
        if(ctx.ret != null) ret = visitParametric_ref(ctx.ret);

        List<RawArgument> dyn = new LinkedList<>();
        if(ctx.dynamic != null) ctx.dynamic.argument_def().stream().map(this::visitArgument_def).forEach(dyn::add);
        List<RawArgument> stat = new LinkedList<>();
        if(ctx.static_ != null) ctx.static_.argument_def().stream().map(this::visitArgument_def).forEach(stat::add);
        Expr exp = withPotentialTailCalls(() ->{
            if(ctx.PRIMITIVE() == null){
                return visitExpr(ctx.expr());
            } else {
                List<Expr> primArgs = new LinkedList<>();
                notInTailPos(() -> {
                    for(int i = 0; i < dyn.size()+stat.size(); i++){
                        primArgs.add(new BindingExpr(new ArgBinding(i)));
                    }
                    return null;
                });
                return new PrimitiveExpr(ctx.prim.getText(),primArgs);
            }
        });
        Reference before = null;
        Reference after = null;
        var posAnot = ctx.case_annotations().position_annotation();
        if(posAnot!= null){
            if(posAnot.AFTER() != null) after = visitReference(posAnot.reference());
            if(posAnot.BEFORE() != null) before = visitReference(posAnot.reference());
        }

        var method = new RawCaseMethod(fullName, getMode(), before, after, generics, multiMethod, dyn, stat, ret, exp);
        module.addCaseMethod(name, method);
        return method;
    }

    @Override
    public ParametricReference visitParametric_ref(nora.compiler.ModuleParser.Parametric_refContext ctx) {
        return (ParametricReference)super.visitParametric_ref(ctx);
    }

    @Override
    public ParametricReference visitPlainRef(nora.compiler.ModuleParser.PlainRefContext ctx) {
        var main = visitReference(ctx.reference());
        List<ParametricReference> appls = null;
        if(ctx.reference().SELF() != null){
            if(ctx.type_appl() != null) throw new IllegalArgumentException("Self can not have arguments");
            appls = curGenerics;
        } else if(ctx.type_appl() != null) {
            appls = visitType_appl(ctx.type_appl());
        }
        return new ParametricReference(main, appls);
    }

    @Override
    public ParametricReference visitFunRef(nora.compiler.ModuleParser.FunRefContext ctx) {
        var sig = ctx.parametric_ref().stream().map(this::visitParametric_ref).toList();
        return new ParametricReference(new Reference("nora.lang.Primitives.Function"), sig);
    }

    @Override
    public Object visitTupleRef(nora.compiler.ModuleParser.TupleRefContext ctx) {
        var sig = ctx.parametric_ref().stream().map(this::visitParametric_ref).toList();
        return new ParametricReference(new Reference("nora.lang.Primitives.Tuple"), sig);
    }

    @Override
    public Reference visitReference(nora.compiler.ModuleParser.ReferenceContext ctx) {
        if(ctx.SELF() != null) return new Reference(curDef);
        var path = String.join(".", ctx.id().stream().map(RuleContext::getText).toList());
        return new Reference(path);
    }

    @Override
    public List<ParametricReference> visitType_appl(nora.compiler.ModuleParser.Type_applContext ctx) {
        return ctx.parametric_ref().stream().map(this::visitParametric_ref).toList();
    }

    @Override
    public Expr visitExpr(nora.compiler.ModuleParser.ExprContext ctx) {
        if (ctx.parametric_ref() != null) {
            var typ = visitParametric_ref(ctx.parametric_ref());
            var trg = visitExpr(ctx.expr());
            return new TypeHintExpr(trg, typ);
        } else {
            return visitPrio9(ctx.prio9());

        }
    }

    @Override
    public Expr visitPrio9(nora.compiler.ModuleParser.Prio9Context ctx) {
        if(ctx.op1 != null && ctx.op2 != null){
            var args = notInTailPos(() -> {
                var arg1 = visitPrio9(ctx.op1);
                var arg2 = visitPrio8(ctx.op2);
                return List.of(arg1,arg2);
            });
            //Could be tailcall but is primitive anyway
            return new CallExpr(new LinkExpr(ParametricReference.primitive("nora.lang.Logic.or")), args);
        } else  {
            return visitPrio8(ctx.prio8());
        }
    }

    @Override
    public Expr visitPrio8(nora.compiler.ModuleParser.Prio8Context ctx) {
        if(ctx.op1 != null && ctx.op2 != null){
            var args = notInTailPos(() -> {
                var arg1 = visitPrio8(ctx.op1);
                var arg2 = visitPrio7(ctx.op2);
                return List.of(arg1,arg2);
            });
            //Could be tailcall but is primitive anyway
            return new CallExpr(new LinkExpr(ParametricReference.primitive("nora.lang.Logic.xor")), args);
        } else  {
            return visitPrio7(ctx.prio7());
        }
    }

    @Override
    public Expr visitPrio7(nora.compiler.ModuleParser.Prio7Context ctx) {
        if(ctx.op1 != null && ctx.op2 != null){
            var args = notInTailPos(() -> {
                var arg1 = visitPrio7(ctx.op1);
                var arg2 = visitPrio6(ctx.op2);
                return List.of(arg1,arg2);
            });
            //Could be tailcall but is primitive anyway
            return new CallExpr(new LinkExpr(ParametricReference.primitive("nora.lang.Logic.and")), args);
        } else  {
            return visitPrio6(ctx.prio6());
        }
    }

    @Override
    public Expr visitPrio6(nora.compiler.ModuleParser.Prio6Context ctx) {
        if(ctx.op1 != null && ctx.op2 != null){
            var args = notInTailPos(() -> {
                var arg1 = visitPrio6(ctx.op1);
                var arg2 = visitPrio5(ctx.op2);
                return List.of(arg1,arg2);
            });
            //Could be tailcall but is primitive anyway
            if(ctx.BANG() != null) {
                var eq = notInTailPos(() -> new CallExpr(new LinkExpr(ParametricReference.primitive("nora.lang.Compare.eq")), args));
                return new CallExpr(new LinkExpr(ParametricReference.primitive("nora.lang.Logic.not")), List.of(eq));
            } else {
                return new CallExpr(new LinkExpr(ParametricReference.primitive("nora.lang.Compare.eq")), args);
            }
        } else  {
            return visitPrio5(ctx.prio5());
        }
    }

    @Override
    public Expr visitPrio5(nora.compiler.ModuleParser.Prio5Context ctx) {
        if(ctx.op1 != null && ctx.op2 != null){
            var args = notInTailPos(() -> {
                var arg1 = visitPrio5(ctx.op1);
                var arg2 = visitPrio4(ctx.op2);
                return List.of(arg1,arg2);
            });
            //Could be tailcall but is primitive anyway
            if(ctx.LT() != null) {
                if(ctx.EQ() != null){
                    return new CallExpr(new LinkExpr(ParametricReference.primitive("nora.lang.Compare.lte")), args);
                } else {
                    return new CallExpr(new LinkExpr(ParametricReference.primitive("nora.lang.Compare.lt")), args);
                }
            } else {
                if(ctx.EQ() != null){
                    return new CallExpr(new LinkExpr(ParametricReference.primitive("nora.lang.Compare.gte")), args);
                } else {
                    return new CallExpr(new LinkExpr(ParametricReference.primitive("nora.lang.Compare.gt")), args);
                }
            }
        } else  {
            return visitPrio4(ctx.prio4());
        }
    }

    @Override
    public Expr visitPrio4(nora.compiler.ModuleParser.Prio4Context ctx) {
        if(ctx.op1 != null && ctx.op2 != null){
            var args = notInTailPos(() -> {
                var arg1 = visitPrio4(ctx.op1);
                var arg2 = visitPrio3(ctx.op2);
                return List.of(arg1,arg2);
            });
            //Could be tailcall but is primitive anyway
            if(ctx.ADD() != null) {
                return new CallExpr(new LinkExpr(ParametricReference.primitive("nora.lang.Arith.add")), args);
            } else {
                return new CallExpr(new LinkExpr(ParametricReference.primitive("nora.lang.Arith.sub")), args);
            }
        } else  {
            return visitPrio3(ctx.prio3());
        }
    }

    @Override
    public Expr visitPrio3(nora.compiler.ModuleParser.Prio3Context ctx) {
        if(ctx.op1 != null && ctx.op2 != null){
            var args = notInTailPos(() -> {
                var arg1 = visitPrio3(ctx.op1);
                var arg2 = visitPrio2(ctx.op2);
                return List.of(arg1,arg2);
            });
            //Could be tailcall but is primitive anyway
            if(ctx.MUL() != null) {
                return new CallExpr(new LinkExpr(ParametricReference.primitive("nora.lang.Arith.mul")), args);
            } else if(ctx.DIV() != null) {
                return new CallExpr(new LinkExpr(ParametricReference.primitive("nora.lang.Arith.div")), args);
            } else {
                return new CallExpr(new LinkExpr(ParametricReference.primitive("nora.lang.Arith.mod")), args);
            }
        } else  {
            return visitPrio2(ctx.prio2());
        }
    }

    @Override
    public Expr visitPrio2(nora.compiler.ModuleParser.Prio2Context ctx) {
        if(ctx.op1 != null){
            var arg1 = notInTailPos(() -> visitPrio2(ctx.op1));
            //Could be tailcall but is primitive anyway
            if(ctx.BANG() != null) {
                return new CallExpr(new LinkExpr(ParametricReference.primitive("nora.lang.Logic.not")), List.of(arg1));
            } else {
                return new CallExpr(new LinkExpr(ParametricReference.primitive("nora.lang.Arith.inv")), List.of(arg1));
            }
        } else  {
            return visitPrio1(ctx.prio1());
        }
    }

    @Override
    public Expr visitPrio1(nora.compiler.ModuleParser.Prio1Context ctx) {
        if(ctx.id() != null){
            var field = ctx.id().getText();
            var target = notInTailPos(() -> visitPrio1(ctx.prio1()));
            return new FieldExpr(field,target);
        } else{
            return visitPrio0(ctx.prio0());
        }

    }

    @Override
    public Expr visitPrio0(nora.compiler.ModuleParser.Prio0Context ctx) {
        return (Expr)super.visitPrio0(ctx);
    }

    @Override
    public Expr visitScope(nora.compiler.ModuleParser.ScopeContext ctx) {
        if(ctx.expr() != null){
            return visitExpr(ctx.expr());
        } else {
            return visitBaseExpr(ctx.baseExpr());
        }
    }

    @Override
    public Expr visitBaseExpr(nora.compiler.ModuleParser.BaseExprContext ctx) {
        return (Expr)super.visitBaseExpr(ctx);
    }

    @Override
    public Expr visitLet(nora.compiler.ModuleParser.LetContext ctx) {
        var name = ctx.id().getText();
        var val = notInTailPos(() -> visitExpr(ctx.val));
        var cont = visitExpr(ctx.cont);
        if(ctx.parametric_ref() != null){
            val = new TypeHintExpr(val, visitParametric_ref(ctx.parametric_ref()));
        }
        return new LetExpr(name,val,cont);
    }

    @Override
    public Expr visitIf(nora.compiler.ModuleParser.IfContext ctx) {
        var cond = notInTailPos(() -> visitExpr(ctx.cond));
        var then = visitExpr(ctx.then);
        var other = visitExpr(ctx.other);
        return new IfExpr(cond,then,other);
    }

    @Override
    public Expr visitInstanceOf(nora.compiler.ModuleParser.InstanceOfContext ctx) {
        var typ = visitParametric_ref(ctx.parametric_ref());
        var trg = notInTailPos(() -> visitScope(ctx.scope()));
        return new InstanceOfExpr(new LinkExpr(typ), trg);
    }

    //Used to detect tail context calls
    private boolean inTailContext = false;
    private <T> T withInTailContext(Supplier<T> f){
        var oldInTailContext = inTailContext;
        //only top level non nested creates can be context tail called
        inTailContext = isTail & !inTailContext;
        var res = f.get();
        isTail = oldInTailContext;
        return res;
    }

    /*@Override
    public Expr visitCreate(nora.compiler.ModuleParser.CreateContext ctx) {
        Map<String, Expr> fields = notInTailPos(() -> {
            if(ctx.named_exprs() != null){
                return visitNamed_exprs(ctx.named_exprs());
            } else {
                return Map.of();
            }
        });
        var target = visitParametric_ref(ctx.parametric_ref());
        return new CreateExpr(target,fields,false);
    }*/
    @Override
    public Expr visitCreate(nora.compiler.ModuleParser.CreateContext ctx) {
        Map<String, Expr> fields = new HashMap<>();
        boolean tailCtxRec = withInTailContext(() -> {
            if(ctx.named_exprs() != null){
                var tailCtxRecDetected = false;
                for(nora.compiler.ModuleParser.Named_exprContext namedCtx: ctx.named_exprs().named_expr()){
                    var kv = visitNamed_expr(namedCtx);
                    fields.put(Objects.requireNonNullElseGet(kv.a, () -> "$" + fields.size()), kv.b);
                    //we detected a tail recursion
                    if(isTailRec) {
                        tailCtxRecDetected = true;
                        //do not detect anymore
                        isTailRec = false;
                        isTail = false;
                    }
                }
                return tailCtxRecDetected;
            } else {
                return false;
            }
        });
        if(tailCtxRec){
            //Restore for further detection
            isTailRec = true;
            isTail = true;
            needsCtxTailRecVersion = true;
        }
        var target = visitParametric_ref(ctx.parametric_ref());
        return new CreateExpr(target,fields,tailCtxRec);
    }

    @Override
    public Object visitTuple(nora.compiler.ModuleParser.TupleContext ctx) {
        var target = ParametricReference.primitive("nora.lang.Primitives.Tuple");
        Map<String, Expr> fields = new HashMap<>();
        boolean tailCtxRec = withInTailContext(() -> {
            var tailCtxRecDetected = false;
            for(var exp: ctx.expr()){
                var res = visitExpr(exp);
                fields.put(TypeCheckContext.getTupleFieldName(fields.size()), res);
                //we detected a tail recursion
                if(isTailRec) {
                    tailCtxRecDetected = true;
                    //do not detect anymore
                    isTailRec = false;
                    isTail = false;
                }
            }
            return tailCtxRecDetected;
        });
        if(tailCtxRec){
            //Restore for further detection
            isTailRec = true;
            isTail = true;
            needsCtxTailRecVersion = true;
        }
        return new CreateExpr(target,fields,tailCtxRec);
    }

    @Override
    public Expr visitCall(nora.compiler.ModuleParser.CallContext ctx) {
        var args = new LinkedList<Expr>();
        var target = notInTailPos(() -> {
            if(ctx.exprs() != null) ctx.exprs().expr().stream().map(this::visitExpr).forEach(args::add);
            return visitScope(ctx.scope());
        });
        //See if we are tail recursive
        if(target instanceof LinkExpr se && se.isSelf(curDef, curGenerics)){
            if(isTail) isTailRec = true;
            return new CallExpr(target,args, isTail);
        } else {
            return new CallExpr(target,args);
        }
    }

    @Override
    public Pair<String,Expr> visitNamed_expr(nora.compiler.ModuleParser.Named_exprContext ctx) {
        String name = null;
        if(ctx.id() != null) name = ctx.id().getText();
        var value = visitExpr(ctx.expr());
        return new Pair<>(name, value);
    }

    @Override
    public Map<String, Expr> visitNamed_exprs(nora.compiler.ModuleParser.Named_exprsContext ctx) {
        var map = new HashMap<String, Expr>();
        ctx.named_expr().stream()
                .map(this::visitNamed_expr).
                forEach(kv -> map.put(Objects.requireNonNullElseGet(kv.a, () -> "$" + map.size()), kv.b));
        return map;
    }


    @Override
    public RawArgument visitClosure_argument_def(nora.compiler.ModuleParser.Closure_argument_defContext ctx) {
        ParametricReference typ = null;
        if(ctx.parametric_ref() != null) typ = visitParametric_ref(ctx.parametric_ref());
        return new RawArgument(ctx.id().getText(), typ);

    }

    @Override
    public Expr visitClosure(nora.compiler.ModuleParser.ClosureContext ctx) {
        //Todo: currently we do not make closure tail calls
        //      as the self refers to the enclosing function
        var args = ctx.closure_argument_def().stream()
                .map(this::visitClosure_argument_def).toList();
        //If we support closure tail calls do this in a new context
        var body =  notInTailPos(() -> visitExpr(ctx.expr()));
        return new LambdaExpr(args, body);
    }

    @Override
    public Expr visitSymbol(nora.compiler.ModuleParser.SymbolContext ctx) {
        var appl = ctx.plainRef().type_appl();
        if(appl == null && ctx.plainRef().reference().id().size() == 1) {
            return new SymbolExpr(ctx.plainRef().reference().id(0).getText());
        } else {
            return new LinkExpr(visitPlainRef(ctx.plainRef()));
        }
    }

    @Override
    public Expr visitLit(nora.compiler.ModuleParser.LitContext ctx) {
        if(ctx.int_() != null) {
            var big = new BigInteger(ctx.int_().getText());
            try {
                return new LitExpr(big.intValueExact());
            } catch (ArithmeticException ex){
                try {
                    return new LitExpr(big.longValueExact());
                } catch (ArithmeticException ex2){
                    return new LitExpr(big);
                }
            }
        } else if(ctx.BOOL() != null){
            return new LitExpr(ctx.BOOL().getText().equals("true"));
        } else {
            var text = ctx.STRING().getText();
            return new LitExpr(text.substring(1, text.length()-1));
        }
    }
}
