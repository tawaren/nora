package nora.vm.loading;

import nora.runtime.*;
import meta.MetaLanguageObject;
import meta.MetaMethodCaseFilter;
import meta.MetaMethodLoadFilter;
import nora.vm.method.GenericMultiMethod;
import nora.vm.runtime.NoraVmContext;
import nora.vm.types.Type;
import nora.vm.types.TypeInfo;
import nora.vm.types.pattern.*;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class MultiMethodLoader extends MultiMethodBaseVisitor<Object> {
    private final Map<Path, GenericMultiMethod> loadCache = new HashMap<>();

    public GenericMultiMethod parse(Path file) throws IOException {
        return loadCache.computeIfAbsent(file, (Path f) -> {
            try {
                var lexer = new MultiMethodLexer(CharStreams.fromPath(file));
                var tokens = new CommonTokenStream(lexer);
                var parser = new MultiMethodParser(tokens);
                var parsed = parser.generic_method_def();
                var vis = new MultiMethodLoader();
                return vis.visitGeneric_method_def(parsed);
            } catch (Exception e){
                //Todo: Better error Handling
                throw new RuntimeException(file.toString(),e);
            }
        });
    }


    @Override
    public GenericMultiMethod visitGeneric_method_def(MultiMethodParser.Generic_method_defContext ctx) {
        List<GenericMultiMethod.GenericDispatchInfo> dispatchInfo = new ArrayList<>();
        for(MultiMethodParser.Dispatch_infoContext di: ctx.dispatch_info()){
            var info = visitDispatch_info(di);
            if(info != null) dispatchInfo.add(info);
        }
        var sig = ctx.typesig();
        List<TypePattern> dynArgTypes = new ArrayList<>();
        for(MultiMethodParser.Type_infoContext ti: sig.typeargs().type_info()){
            //Variance is irrelevant for signature as it is only built never matched
            dynArgTypes.add(visitType_info(ti));
        }

        List<TypePattern> statArgTypes = new ArrayList<>();
        for(MultiMethodParser.Type_infoContext ti: sig.directTypeSig().typeargs().type_info()){
            //Variance is irrelevant for signature as it is only built never matched
            statArgTypes.add(visitType_info(ti));
        }

        //Variance is irrelevant for signature as it is only built never matched
        var retType = visitType_info(sig.directTypeSig().ret);
        return new GenericMultiMethod(dispatchInfo.toArray(GenericMultiMethod.GenericDispatchInfo[]::new), dynArgTypes.toArray(TypePattern[]::new), statArgTypes.toArray(TypePattern[]::new), retType);
    }

    @Override
    public GenericMultiMethod.GenericDispatchInfo visitDispatch_info(MultiMethodParser.Dispatch_infoContext ctx) {
        var fullMethodName = visitEntity_name(ctx.entity_name());
        var filters = new LinkedList<MetaMethodCaseFilter>();
        if(ctx.case_filters() != null) {
            var sucess = ctx.case_filters().case_filter().stream().map(this::visitCase_filter).allMatch(filter -> {
                if(filter == null){
                    return false;
                }else if(filter instanceof MetaMethodCaseFilter cf){
                    filters.add(cf);
                    return true;
                } else if(filter instanceof MetaMethodLoadFilter mf){
                    return mf.loadCheck(fullMethodName);
                } else {
                    return false;
                }
            });
            if(!sucess) {
                System.out.println("Method was filtered during loading: "+fullMethodName);
                return null;
            }
        }

        List<TypePattern> generics = new ArrayList<>();
        int typParams = 0;
        if(ctx.genargs() != null){
            for(MultiMethodParser.Type_infoContext ti:ctx.genargs().type_info()){
                generics.add(visitType_info(ti));
            }
            if(ctx.genargs().typParams != null){
                typParams = Integer.parseInt(ctx.genargs().typParams.getText());
            }
        }

        var sig = ctx.directTypeSig();
        List<TypePattern> arguments = new ArrayList<>();
        for(MultiMethodParser.Type_infoContext ti: sig.typeargs().type_info()){
            arguments.add(visitType_info(ti));
        }

        //Todo: add the MetaMethodCaseFilter
        var retType = visitType_info(sig.ret);
        var genArray = generics.toArray(TypePattern[]::new);
        var argArray = arguments.toArray(TypePattern[]::new);
        var filterArray = filters.toArray(MetaMethodCaseFilter[]::new);

        return new GenericMultiMethod.GenericDispatchInfo(typParams, genArray, argArray, retType, fullMethodName, filterArray);
    }


    @Override
    public MetaLanguageObject visitCase_filter(MultiMethodParser.Case_filterContext ctx) {
        var prot = NoraVmContext.get(null).getMetaProtocol(ctx.meta.getText());
        var args = ctx.filter_arg().stream().map(this::visitFilter_arg).toArray(Object[]::new);
        return prot.createObject(ctx.object.getText(), args);
    }

    @Override
    public Object visitFilter_arg(MultiMethodParser.Filter_argContext ctx) {
        if(ctx.NUM() != null) return new BigInteger(ctx.NUM().getText());
        if(ctx.STRING() != null) {
            var str = ctx.STRING().getText();
            return str.substring(1,str.length()-1);
        }
        if(ctx.BOOL() != null) {
            return ctx.BOOL().getText().equals("<true>");
        }
        if(ctx.type_info() != null) return visitType_info(ctx.type_info());
        //Todo: shall we return something else then String?
        if(ctx.entity_name() != null) return visitEntity_name(ctx.entity_name());
        return null;
    }


    @Override
    public TypePattern visitVariant_type_info(MultiMethodParser.Variant_type_infoContext ctx) {
        var res = (TypePattern)super.visitVariant_type_info(ctx);
        if(ctx.CO() != null) return res.withVariance(Type.Variance.Co);
        if(ctx.CONTRA() != null) return res.withVariance(Type.Variance.Contra);
        return res.withVariance(Type.Variance.Inv);
    }

    @Override
    public TypePattern visitType_info(MultiMethodParser.Type_infoContext ctx) {
        return (TypePattern) super.visitType_info(ctx);
    }

    @Override
    public SimpleTypePattern visitRoot_type(MultiMethodParser.Root_typeContext ctx) {
        var cat = Integer.parseInt(ctx.cat.getText());
        var start = Integer.parseInt(ctx.start.getText());
        var end = Integer.parseInt(ctx.end.getText());
        return new SimpleTypePattern(new TypeInfo(cat,start,end).stabilized());
    }

    @Override
    public Placeholder visitSubst_type(MultiMethodParser.Subst_typeContext ctx) {
        return new Placeholder(Integer.parseInt(ctx.NUM().getText()));
    }

    @Override
    public TemplateTypePattern visitTempl_type(MultiMethodParser.Templ_typeContext ctx) {
        var info = visitRoot_type(ctx.root_type()).getInfo();
        var args = ctx.variant_type_info().stream().map(this::visitVariant_type_info).toArray(TypePattern[]::new);
        return new TemplateTypePattern(info,args);
    }

    @Override
    public String visitEntity_name(MultiMethodParser.Entity_nameContext ctx) {
        return visitPackage_name(ctx.package_name())+"::"+visitId(ctx.id());
    }

    @Override
    public String visitPackage_name(MultiMethodParser.Package_nameContext ctx) {
        return ctx.id().stream().map(this::visitId).collect(Collectors.joining("."));
    }

    @Override
    public String visitId(MultiMethodParser.IdContext ctx) {
        return ctx.v1.getText();
    }
}
