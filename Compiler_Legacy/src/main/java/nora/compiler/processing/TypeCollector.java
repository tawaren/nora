package nora.compiler.processing;

import nora.compiler.entries.DefInstance;
import nora.compiler.entries.GenericInstance;
import nora.compiler.entries.Instance;
import nora.compiler.entries.interfaces.Data;
import nora.compiler.entries.resolved.Generic;
import nora.compiler.resolver.PathTreeNode;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class TypeCollector {

    private final static TypeCollector initial = new TypeCollector();

    public static TypeCollector get(){
        return initial;
    }

    public record TypeBase(int cat, int start, int end){
        @Override
        public String toString() {
            return "{"+cat+","+start+","+end+"}";
        }
    }
    public interface Type{
        String toVarianceString(boolean printSign);
    };
    public record ImplType(String varianceSign, TypeBase base, List<Type> applies) implements Type {
        @Override
        public String toString() {
            if (applies.isEmpty()) return base.toString();
            return base.toString() + "[" + String.join(",", applies.stream().map(Type::toString).toList()) + "]";
        }

        @Override
        public String toVarianceString(boolean printSign) {
            String withoutSign;
            if (applies.isEmpty()) {
                withoutSign = base.toString();
            } else {
                var applyString = new LinkedList<String>();
                for(Type appl:applies){
                    applyString.add(appl.toVarianceString(true));
                }
                withoutSign = base.toString() + "[" + String.join(",", applyString) + "]";
            }

            if(printSign){
                return varianceSign+withoutSign;
            } else {
                return withoutSign;
            }
        }
    }
    public record GenType(String varianceSign, int index) implements Type{
        @Override
        public String toString() {
            return "?"+index;
        }

        @Override
        public String toVarianceString(boolean printSign) {
            if(printSign) return varianceSign + this.toString();
            return this.toString();
        }
    }

    private int counter;
    private final Map<String, TypeBase> dataToType = new HashMap<>();
    private final Map<Integer, Data> catToRoot = new HashMap<>();

    private int processFromRoot(Data data, int fam, int start){
       var end = start;
       for(Data child:data.getChildren()){
           end = processFromRoot(child, fam, end);
       }
       dataToType.put(data.getFullyQualifiedName(), new TypeBase(fam,start,end));
       return end+1;
    }

    public TypeBase resolveType(Data data){
        var cur = dataToType.get(data.getFullyQualifiedName());
        if(cur != null) return cur;
        var root = data;
        while (root.getParent() != null) root=root.getParent();
        int typeCat;
        if(root.getTypId() != null) {
            typeCat = root.getTypId();
        } else {
            typeCat = counter++;
        }
        processFromRoot(root, typeCat, 0);
        catToRoot.put(typeCat, root);
        return dataToType.get(data.getFullyQualifiedName());
    }

    private Type resolveInstance(Instance inst, String sign){
        if(inst.isData() && inst instanceof DefInstance di){
            var dataBase = di.getBase().asData();
            var base = resolveType(dataBase);
            var variances = dataBase.getGenerics().stream().map(Generic::variance).toList();
            if(TypeCheckContext.isSpecialVarargDef(dataBase)){
                variances = TypeCheckContext.getVarargVariance(dataBase,di.getArguments());
            }
            var args = new LinkedList<Type>();
            int i = 0;
            for(Instance argI: di.getArguments()){
                var var = variances.get(i++);
                args.add(resolveInstance(argI, var.generateVarianceSign()));
            }
            return new ImplType(sign, base, args);
        } else if(inst instanceof GenericInstance gi) {
            return new GenType(sign, gi.index);
        }else {
            throw new RuntimeException("Not a type");
        }
    }

    public Type resolveInstance(Instance inst){
        return resolveInstance(inst, "");
    }

    public String resolveTypeString(Instance inst, boolean printVariance){
        var typ = resolveInstance(inst);
        if(!printVariance) return typ.toString();
        return typ.toVarianceString(false);
    }

    public String resolveTypeString(Instance inst){
        return resolveTypeString(inst, false);
    }

    public String resolveEntityString(Instance inst){
        if(inst instanceof DefInstance di){
            StringBuilder builder = new StringBuilder();
            builder.append(di.getBase().getFullyQualifiedName());
            builder.append("[");
            for (Iterator<Instance> it = di.getArguments().iterator(); it.hasNext();) {
                var typ = TypeCollector.get().resolveTypeString(it.next(), true);
                builder.append(typ);
                if(it.hasNext()) builder.append(",");
            }
            builder.append("]");
            return builder.toString();
        } else if(inst instanceof GenericInstance gi){
            //We can use upper type bounds in field expressions
            if(gi.getBound() != null && gi.getBound().isData()){
                return resolveEntityString(gi.getBound());
            }
        }
        throw new RuntimeException("Wrong instance: "+inst);

    }

    private void generateDataStructureDefName(OutputStreamWriter out, Data data) throws IOException {
        for(Data child: data.getChildren()) generateDataStructureDefName(out, child);
        out.append(data.getFullyQualifiedName()).append("\n");
    }

    private int generateDataStructureEntry(OutputStreamWriter out, Data data) throws IOException {
        var kids = 0;
        for(Data child: data.getChildren()) {
            kids += generateDataStructureEntry(out, child);
        }
        out.append(String.valueOf(kids)).append("\n");
        return kids+1;
    }

    public void generateCode(Path base) throws IOException {
        var typesDir = base.resolve(Path.of(".types"));
        Files.createDirectory(typesDir);
        for(Map.Entry<Integer,Data> e:catToRoot.entrySet()){
            var file = typesDir.resolve(Path.of(e.getKey()+".nora"));
            var size = e.getValue().countChildren();
            try(OutputStreamWriter out = new OutputStreamWriter(Files.newOutputStream(file, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
                out.append("<family>: ").append(String.valueOf(e.getKey())).append("\n");
                out.append("<length>: ").append(String.valueOf(size)).append("\n");
                out.append("<definitions>: ").append("\n");
                generateDataStructureDefName(out, e.getValue());
                out.append("<structure>: ").append("\n");
                generateDataStructureEntry(out, e.getValue());
            }
        }
    }
}
