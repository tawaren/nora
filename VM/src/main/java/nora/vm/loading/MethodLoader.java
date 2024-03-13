package nora.vm.loading;

import nora.vm.method.EntryPoint;
import nora.vm.nodes.*;
import nora.vm.nodes.consts.*;
import nora.vm.nodes.method.opt.*;
import nora.vm.nodes.method.opt.template.GenericTailLoopDataCall;
import nora.vm.nodes.method.template.GenericDispatchNode;
import nora.vm.nodes.method.template.MethodNode;
import nora.vm.nodes.method.template.MultiMethodNode;
import nora.vm.nodes.template.*;
import nora.runtime.*;
import nora.vm.nodes.type.ConcreteTypeOfNodeGen;
import nora.vm.nodes.type.SimpleInstanceOfNode;
import nora.vm.nodes.type.TypeOfNodeGen;
import nora.vm.nodes.type.template.GenericInstanceOfNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.types.Type;
import nora.vm.types.TypeInfo;
import nora.vm.types.pattern.Placeholder;
import nora.vm.types.pattern.SimpleTypePattern;
import nora.vm.types.pattern.TemplateTypePattern;
import nora.vm.types.pattern.TypePattern;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.Pair;
import com.oracle.truffle.api.strings.TruffleString;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MethodLoader extends MethodBaseVisitor<Object> {
    private final Map<Path, Pair<TypePattern[],EntryPoint>> loadCache = new HashMap<>();

    public Pair<TypePattern[],EntryPoint> parse(Path file) {
        return loadCache.computeIfAbsent(file, (Path f) -> {
            try {
                var lexer = new MethodLexer(CharStreams.fromPath(file));
                var tokens = new CommonTokenStream(lexer);
                var parser = new MethodParser(tokens);
                var parsed = parser.method_def();
                var vis = new MethodLoader();
                return vis.visitMethod_def(parsed);
            } catch (Exception e){
                e.printStackTrace();
                //Todo: Better error Handling
                throw new RuntimeException(file.toString(),e);
            }
        });
    }

    //-1 means no recursion optimisation enabled
    private int recursionParams = -1;
    private int recStartPos = 0;

    private boolean isCtxRec = false;
    @Override
    public Pair<TypePattern[],EntryPoint> visitMethod_def(MethodParser.Method_defContext ctx) {
        recursionParams = -1;
        recStartPos = 0;
        var locals = Integer.parseInt(ctx.loc.getText());
        var sig = ctx.type_info().stream()
                .map(this::visitType_info)
                .map(tp -> tp.withVariance(Type.Variance.Contra)) //We will correct return next line
                .toArray(TypePattern[]::new);
        sig[sig.length-1] = sig[sig.length-1].withVariance(Type.Variance.Co);
        if(ctx.RECURSION() != null || ctx.CTX_RECURSION() != null){
            //+1 is for the self
            recursionParams = Integer.parseInt(ctx.rec.getText())+1;
            recStartPos = locals;
            locals+=recursionParams;
        }
        if(ctx.CTX_RECURSION() != null){
            isCtxRec = true;
            //One for the Accumulator & One for the setter property
            locals+=2;
        }
        var code = visitExpr(ctx.expr());
        if(recursionParams != -1){
            var setters = new ArgSetterNode[recursionParams];
            for(int i = 0; i < recursionParams; i++){
                setters[i] = ArgSetterNodeGen.create(new ArgNode(i), recStartPos+i);
            }
            if(ctx.CTX_RECURSION() != null){
                return new Pair<>(sig,new EntryPoint(locals,new TailLoopDataHeader(recStartPos,setters,code)));
            } else {
                return new Pair<>(sig,new EntryPoint(locals,new TailLoopHeader(recStartPos,setters,code)));
            }
        } else {
            return new Pair<>(sig,new EntryPoint(locals,code));
        }
    }

    @Override
    public NoraNode visitExpr(MethodParser.ExprContext ctx) {
        return (NoraNode) visitExpr0(ctx.expr0());
    }

    @Override
    public NoraNode visitBlock(MethodParser.BlockContext ctx) {
        var lets = ctx.let().stream().map(this::visitLet).toArray(StmNode[]::new);
        return new BlockNode(lets, visitExpr(ctx.expr()));
    }

    //Todo: Do Frame slots have to be object reference safe if so we need to cache
    @Override
    public StmNode visitLet(MethodParser.LetContext ctx) {
        var slot = Integer.parseInt(ctx.slot().NUM().getText());
        return LetNodeGen.create(slot, visitExpr(ctx.expr()));
    }

    //Todo: Do Frame slots have to be object reference safe if so we need to cache
    @Override
    public NoraNode visitSlot(MethodParser.SlotContext ctx) {
        return new ValNodeUntyped(Integer.parseInt(ctx.NUM().getText()));
    }

    @Override
    public NoraNode visitArg(MethodParser.ArgContext ctx) {
        var arg = Integer.parseInt(ctx.NUM().getText());
        if(recursionParams != -1){
            return new ValNodeUntyped(arg+recStartPos);
        } else {
            return new ArgNode(arg);
        }
    }

    @Override
    public NoraNode visitTarg(MethodParser.TargContext ctx) {
        return new TypePatternNode(visitType_info(ctx.type_info()));
    }

    @Override
    public NoraNode visitLit(MethodParser.LitContext ctx) {
        if(ctx.NUM() != null){
            if(ctx.INTNOTE() != null){
                return ConstNode.create(Integer.parseInt(ctx.NUM().getText()));
            } else {
                var number = new BigInteger(ctx.NUM().getText());
                try {
                    return ConstNode.create(number.longValueExact());
                } catch (ArithmeticException ex){
                    return ConstNode.create(number);
                }
            }
        } else if(ctx.STRING() != null) {
            var raw = ctx.STRING().getText();
            var sub = raw.substring(1, raw.length()-1);
            return ConstNode.create(TruffleString.fromJavaStringUncached(sub, TruffleString.Encoding.UTF_16));
        } else if(ctx.BOOLEAN() != null){
            var raw = ctx.BOOLEAN().getText();
            if(raw.equals("<true>")) return ConstNode.create(true);
            return ConstNode.create(false);
        }
        throw new IllegalStateException("Constant Visit Rule must be added");
    }


    @Override
    public NoraNode visitCreate(MethodParser.CreateContext ctx){
        String id = visitEntity_name(ctx.entity_name());
        var genTypeExprs = ctx.generics().expr().stream().map(this::visitExpr).toArray(NoraNode[]::new);
        if(ctx.TAILCREATE() != null) {
            if(!isCtxRec)throw new IllegalStateException("Only Context Recursive functions can have tail creates");
            //Allow tail-rec argument
            var args = visitArguments(ctx.arguments());
            return new GenericTailLoopDataCall(id,genTypeExprs,args, recStartPos+recursionParams);
        } else {
            //create : CREATE WS* '#' WS* entity_name WS* generics WS* arguments;
            var args = visitArguments(ctx.arguments());
            return new GenericCreateNode(id,genTypeExprs,args);
        }

    }

    @Override
    public NoraNode visitField(MethodParser.FieldContext ctx) {
        var id = visitEntity_name(ctx.entity_name());
        var data = visitExpr(ctx.expr());
        var genTypeExprs = ctx.generics().expr().stream().map(this::visitExpr).toArray(NoraNode[]::new);
        var index = Integer.parseInt(ctx.NUM().getText());
        return new GenericFieldNode(id, genTypeExprs, data, index);
    }

    @Override
    public NoraNode visitIfThenElse(MethodParser.IfThenElseContext ctx) {
        var cond = visitExpr(ctx.cond);
        var then = visitExpr(ctx.then);
        var other = visitExpr(ctx.other);
        var typ = visitTarg(ctx.typeHint().targ());
        return new UntypedIfNode(cond,then,other, typ);
    }

    @Override
    public NoraNode visitSubTypeOf(MethodParser.SubTypeOfContext ctx) {
        var val = visitExpr(ctx.val);
        var typ = visitExpr(ctx.typ);
        return new GenericInstanceOfNode(val,typ);
    }

    @Override
    public NoraNode visitTypeOf(MethodParser.TypeOfContext ctx) {
        var val = visitExpr(ctx.val);
        return TypeOfNodeGen.create(val);
    }

    @Override
    public NoraNode visitMethod(MethodParser.MethodContext ctx) {
        String id = visitEntity_name(ctx.entity_name());
        var gens = visitGenerics(ctx.generics());
        return new MethodNode(id,gens);
    }

    @Override
    public NoraNode visitMulti_method(MethodParser.Multi_methodContext ctx) {
        String id = visitEntity_name(ctx.entity_name());
        var gens = visitGenerics(ctx.generics());
        return new MultiMethodNode(id,gens);
    }

    @Override
    public NoraNode visitClosure(MethodParser.ClosureContext ctx) {
        var locals = Integer.parseInt(ctx.NUM().getText());
        var genTypeExprs = ctx.generics().expr().stream().map(this::visitExpr).toArray(NoraNode[]::new);
        var args = visitArguments(ctx.arguments());
        var code = visitExpr(ctx.expr());
        var funType = visitTarg(ctx.targ());
        return new GenericClosureNode(genTypeExprs, args,new EntryPoint(locals, code), funType);
    }

    @Override
    public NoraNode visitCall(MethodParser.CallContext ctx) {
        var targ = visitExpr(ctx.expr());
        var args = visitArguments(ctx.arguments());
        return new GenericDispatchNode(targ, args);
    }

    @Override
    public NoraNode visitTail_rec(MethodParser.Tail_recContext ctx) {
        var args = visitArguments(ctx.arguments());
        if(recursionParams != -1){
            //+1 is for the this slot
            assert args.length+1 == recursionParams;
            DelayedArgSetterNode head = null;
            for(int i = args.length-1; i >= 0; i--){
                //+1 is to skip the this slot
                var newOne = DelayedArgSetterNodeGen.create(args[i], recStartPos+i+1);
                if(head != null) newOne.setNext(head);
                head = newOne;
            }
            return new TailLoopCall(head);
        } else {
            throw new RuntimeException("Only tail recursive function can use tail recursive calls");
        }
    }

    @Override
    public NoraNode visitPrimitive(MethodParser.PrimitiveContext ctx) {
        var args = visitArguments(ctx.arguments());
        var res = NoraVmContext.getSpecialMethodRegistry(null).resolveBuiltin(visitId(ctx.id()), args);
        if(res == null) throw new RuntimeException("Builtin "+visitId(ctx.id())+" is missing");
        return res;
    }

    @Override
    public NoraNode[] visitGenerics(MethodParser.GenericsContext ctx) {
        return ctx.expr().stream().map(this::visitExpr).toArray(NoraNode[]::new);
    }

    @Override
    public NoraNode[] visitArguments(MethodParser.ArgumentsContext ctx) {
        return ctx.expr().stream().map(this::visitExpr).toArray(NoraNode[]::new);
    }

    @Override
    public TypePattern visitVariant_type_info(MethodParser.Variant_type_infoContext ctx) {
        var info = visitType_info(ctx.type_info());
        var variance = visitVariance(ctx.variance());
        return info.withVariance(variance);
    }

    @Override
    public Type.Variance visitVariance(MethodParser.VarianceContext ctx) {
        if(ctx.CO() != null) return Type.Variance.Co;
        if(ctx.CONTRA() != null) return Type.Variance.Contra;
        return Type.Variance.Inv;
    }

    @Override
    public TypePattern visitType_info(MethodParser.Type_infoContext ctx) {
        return (TypePattern) super.visitType_info(ctx);
    }

    @Override
    public SimpleTypePattern visitRoot_type(MethodParser.Root_typeContext ctx) {
        var cat = Integer.parseInt(ctx.cat.getText());
        var start = Integer.parseInt(ctx.start.getText());
        var end = Integer.parseInt(ctx.end.getText());
        return new SimpleTypePattern(new TypeInfo(cat,start,end).stabilized());
    }

    @Override
    public Placeholder visitSubst_type(MethodParser.Subst_typeContext ctx) {
        return new Placeholder(Integer.parseInt(ctx.NUM().getText()));
    }

    @Override
    public TemplateTypePattern visitTempl_type(MethodParser.Templ_typeContext ctx) {
        var info = visitRoot_type(ctx.root_type()).getInfo();
        var args = ctx.variant_type_info().stream().map(this::visitVariant_type_info).toArray(TypePattern[]::new);
        return new TemplateTypePattern(info,args);
    }

    @Override
    public String visitEntity_name(MethodParser.Entity_nameContext ctx) {
        return visitPackage_name(ctx.package_name())+"::"+visitId(ctx.id());
    }

    @Override
    public String visitPackage_name(MethodParser.Package_nameContext ctx) {
        return ctx.id().stream().map(this::visitId).collect(Collectors.joining("."));
    }

    @Override
    public String visitId(MethodParser.IdContext ctx) {
        return ctx.v1.getText();
    }
}
