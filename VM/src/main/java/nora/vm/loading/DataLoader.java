package nora.vm.loading;


import nora.runtime.DataBaseVisitor;
import nora.runtime.DataLexer;
import nora.runtime.DataParser;
import meta.MetaObjectLayoutHandler;
import nora.runtime.MethodParser;
import nora.vm.runtime.NoraVmContext;
import nora.vm.types.Type;
import nora.vm.types.TypeInfo;
import nora.vm.types.pattern.Placeholder;
import nora.vm.types.pattern.SimpleTypePattern;
import nora.vm.types.pattern.TemplateTypePattern;
import nora.vm.types.pattern.TypePattern;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class DataLoader extends DataBaseVisitor<Object> {
    record FieldInfo(String name, TypePattern type){}
    public record DataInfo(String name, MetaObjectLayoutHandler handler, String superData, boolean isConcrete, TypeInfo info, Type.Variance[] variances, List<FieldInfo> fieldTypes){}

    public DataInfo parse(Path file) {
        try {
            var lexer = new DataLexer(CharStreams.fromPath(file));
            var tokens = new CommonTokenStream(lexer);
            var parser = new DataParser(tokens);
            var parsed = parser.data_def();
            var vis = new DataLoader();
            return vis.visitData_def(parsed);
        } catch (Exception e){
            //Todo: Better error Handling
            throw new RuntimeException(file.toString(),e);
        }
    }


    @Override
    public DataInfo visitData_def(DataParser.Data_defContext ctx) {
        String name = visitEntity_name(ctx.name);
        MetaObjectLayoutHandler handler = null;
        if(ctx.meta != null) handler = visitHandler_ref(ctx.meta);
        String superData = null;
        if(ctx.super_ != null) superData = visitEntity_name(ctx.super_);
        boolean isConcrete = ctx.CONCRETE() != null;
        TypeInfo info = visitRoot_type(ctx.root_type()).getInfo();
        List<FieldInfo> fields = ctx.field().stream().map(this::visitField).toList();
        Type.Variance[] variances = visitGenerics(ctx.generics());
        return new DataInfo(name,handler,superData,isConcrete,info,variances,fields);
    }

    @Override
    public FieldInfo visitField(DataParser.FieldContext ctx) {
        var typ = visitType_info(ctx.type_info());
        var name = ctx.id().getText();
        return new FieldInfo(name, typ);
    }

    @Override
    public Type.Variance[] visitGenerics(DataParser.GenericsContext ctx) {
        if(ctx == null) return new Type.Variance[]{};
        return ctx.full_variance().stream().map(this::visitFull_variance).toArray(Type.Variance[]::new);
    }

    @Override
    public Type.Variance visitVariance(DataParser.VarianceContext ctx) {
        if(ctx.CONTRA() != null) return Type.Variance.Contra;
        if(ctx.CO() != null) return Type.Variance.Co;
        return Type.Variance.Inv;
    }

    @Override
    public Type.Variance visitFull_variance(DataParser.Full_varianceContext ctx) {
        if(ctx.CONTRA() != null) return Type.Variance.Contra;
        if(ctx.CO() != null) return Type.Variance.Co;
        if(ctx.INV() != null) return Type.Variance.Inv;
        return null;
    }

    @Override
    public TypePattern visitVariant_type_info(DataParser.Variant_type_infoContext ctx) {
        var info = visitType_info(ctx.type_info());
        var variance = visitVariance(ctx.variance());
        return info.withVariance(variance);
    }

    @Override
    public TypePattern visitType_info(DataParser.Type_infoContext ctx) {
        return (TypePattern) super.visitType_info(ctx);
    }

    @Override
    public SimpleTypePattern visitRoot_type(DataParser.Root_typeContext ctx) {
        var cat = Integer.parseInt(ctx.cat.getText());
        var start = Integer.parseInt(ctx.start.getText());
        var end = Integer.parseInt(ctx.end.getText());
        return new SimpleTypePattern(new TypeInfo(cat,start,end).stabilized());
    }

    @Override
    public Placeholder visitSubst_type(DataParser.Subst_typeContext ctx) {
        return new Placeholder(Integer.parseInt(ctx.NUM().getText()));
    }

    @Override
    public TemplateTypePattern visitTempl_type(DataParser.Templ_typeContext ctx) {
        var info = visitRoot_type(ctx.root_type()).getInfo();
        var args = ctx.variant_type_info().stream().map(this::visitVariant_type_info).toArray(TypePattern[]::new);
        return new TemplateTypePattern(info,args);
    }

    @Override
    public String visitEntity_name(DataParser.Entity_nameContext ctx) {
        return visitPackage_name(ctx.package_name())+"::"+visitId(ctx.id());
    }

    @Override
    public String visitPackage_name(DataParser.Package_nameContext ctx) {
        return ctx.id().stream().map(this::visitId).collect(Collectors.joining("."));
    }

    @Override
    public String visitId(DataParser.IdContext ctx) {
        return ctx.getText();
    }

    @Override
    public MetaObjectLayoutHandler visitHandler_ref(DataParser.Handler_refContext ctx) {
        var prot = NoraVmContext.get(null).getMetaProtocol(ctx.meta.getText());
        var args = ctx.handler_arg().stream().map(this::visitHandler_arg).toArray(Object[]::new);
        return (MetaObjectLayoutHandler)prot.createObject(ctx.object.getText(), args);
    }

    @Override
    public Object visitHandler_arg(DataParser.Handler_argContext ctx) {
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
}
