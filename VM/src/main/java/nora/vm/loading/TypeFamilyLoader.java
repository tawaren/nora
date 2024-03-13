package nora.vm.loading;


import nora.runtime.*;
import nora.vm.types.TypeInfo;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.nio.file.Path;
import java.util.stream.Collectors;

//Note: Not cached as the Dispatcher already does
public class TypeFamilyLoader extends TypeFamilyBaseVisitor<Object> {
    private int family;
    private TypeInfo[] acc;
    private String[] names;

    public record TypeData(String name, TypeInfo info){};

    public TypeData[] parse(Path file) {
        //Todo: prevent an recursion attack where we have cyclic supertype deps
        try {
            var lexer = new TypeFamilyLexer(CharStreams.fromPath(file));
            var tokens = new CommonTokenStream(lexer);
            var parser = new TypeFamilyParser(tokens);
            var parsed = parser.family_def();
            var vis = new TypeFamilyLoader();
            return vis.visitFamily_def(parsed);
        } catch (Exception e){
            //Todo: Better error Handling
            throw new RuntimeException(file.toString(),e);
        }
    }

    @Override
    public TypeData[] visitFamily_def(TypeFamilyParser.Family_defContext ctx) {
        family = Integer.parseInt(ctx.family.getText());
        var len = Integer.parseInt(ctx.length.getText());
        acc = new TypeInfo[len];
        names = new String[len];
        visitNames(ctx.names());
        visitStructure(ctx.structure());
        var res = new TypeData[len];
        for(int i = 0; i < res.length; i++){
            res[i] = new TypeData(names[i], acc[i]);
        }
        return res;
    }

    @Override
    public Object visitNames(TypeFamilyParser.NamesContext ctx) {
        var i = 0;
        for(TypeFamilyParser.Entity_nameContext et: ctx.entity_name()){
            names[i++] = visitEntity_name(et);
        }
        return null;
    }

    @Override
    public Void visitStructure(TypeFamilyParser.StructureContext ctx) {
        var end = 0;
        for(TerminalNode tm: ctx.NUM()){
            var len = Integer.parseInt(tm.getText());
            var start = end-len;
            var info = new TypeInfo(family,start,end).stabilized();
            acc[end++] = info;
        }
        return null;
    }

    @Override
    public String visitEntity_name(TypeFamilyParser.Entity_nameContext ctx) {
        return visitPackage_name(ctx.package_name())+"::"+visitId(ctx.id());
    }

    @Override
    public String visitPackage_name(TypeFamilyParser.Package_nameContext ctx) {
        return ctx.id().stream().map(this::visitId).collect(Collectors.joining("."));
    }

    @Override
    public String visitId(TypeFamilyParser.IdContext ctx) {
        return ctx.v1.getText();
    }
}
