package nora.compiler.entries.resolved;

import nora.compiler.entries.DefInstance;
import nora.compiler.entries.GenericInstance;
import nora.compiler.entries.Instance;
import nora.compiler.entries.interfaces.*;
import nora.compiler.processing.TypeCheckContext;
import nora.compiler.processing.TypeCollector;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ResolvedData extends ResolvedDefinition implements Data {
    private final boolean sealed;
    private final boolean abstr;
    private Integer typId;
    private final String handler;
    private final Instance parent;
    private final List<Generic> generics;
    private final Set<Instance> traits;
    private final List<Argument> fields;
    private final List<Data> children;

    public ResolvedData(String fullyQualifeidName, String handler, boolean sealed, boolean abstr, Integer typId, Instance parent, List<Generic> generics, Set<Instance> traits, List<Argument> fields, List<Data> children) {
        super(fullyQualifeidName);
        this.handler = handler;
        this.sealed = sealed;
        this.abstr = abstr;
        this.typId = typId;
        this.parent = parent;
        this.generics = generics;
        this.traits = traits;
        this.fields = fields;
        this.children = children;
    }

    @Override
    public void addChild(Data child){
        children.add(child);
    }

    @Override
    public void addPrimitiveChild(Data child) {
        if(typId != null) throw new RuntimeException("Primitives can not have subtypes");
        typId = child.getTypId();
        var nChildren = new LinkedList<Data>();
        nChildren.add(child);
        for(Data kid:children){
            if(kid != child) nChildren.add(kid);
        }
        children.clear();
        children.addAll(nChildren);
        if(parent instanceof DefInstance di && di.isData()){
            di.getBase().asData().addPrimitiveChild(this);
        }
    }

    @Override
    public List<Data> getChildren() {
        return children;
    }

    @Override
    public Kind getKind() {
        return Kind.Data;
    }

    @Override
    public Data asData() {
        return this;
    }

    @Override
    public Parametric asParametric() {
        return this;
    }

    @Override
    public WithTraits asWithTraits() {
        return this;
    }

    @Override
    public WithArguments asWithArguments() {
        return this;
    }

    @Override
    public boolean isSealed() { return sealed; }
    @Override
    public boolean isAbstr() { return abstr; }
    @Override
    public Data getParent() {
        if(parent != null && parent.isData() && parent instanceof DefInstance di){
            return di.getBase().asData();
        }
        return null;
    }


    @Override
    public List<Generic> getGenerics() {
        return generics;
    }

    public int numGenerics() {
        return generics.size();
    }

    @Override
    public Set<Instance> getTraits() { return traits; }

    /*@Override
    public void addExtraMarkers(Set<Trait> trait) {
        traits.addAll(trait);
    }*/

    @Override
    public boolean hasTrait(Trait trait) {
        return traits.stream().anyMatch(t -> {
            if(t.isTrait() && t instanceof DefInstance di){
                return di.getBase().asTrait().hasTrait(trait);
            }
            return false;
        });
    }

    @Override
    public boolean subTypeOf(Data otherData) {
        if(fullyQualifiedName.equals(otherData.getFullyQualifiedName())) return true;
        if(parent == null) return false;
        return getParent().subTypeOf(otherData);
    }

    @Override
    public Argument getFieldByName(String field) {
        //Todo: I hate this we need a clean compiler in the future, this is so much patchworlk
        for(Argument a: fields) {
            if(a.name().equals(field)) return a;
        }
        if(parent != null) return getParent().getFieldByName(field);
        throw new RuntimeException("No field named "+field);
    }

    @Override
    public void fillFields(List<Argument> fieldCol) {
        if(parent != null) getParent().fillFields(fieldCol);
        fieldCol.addAll(fields);
    }

    @Override
    public List<Argument> getFields() {
        List<Argument> col = new LinkedList<>();
        fillFields(col);
        return col;
    }

    @Override
    public List<Argument> getArgs() {
        return getFields();
    }

    public Integer getTypId() {
        return typId;
    }

    private Boolean valid = null;
    private boolean cacheValidationRes(boolean res){
        if(!res) throw new RuntimeException("Validation failed"); //Better for now
        valid = res;
        return res;
    }

    @Override
    public boolean validateAndInfer() {
        if(valid != null) return valid;
        //This will give error if a cyclic dep exist
        //Todo: problem is parent will call back
        // But if we set true we no longer detect cycles
        valid = false;
        if(parent != null && !parent.validateAndInfer(Variance.Covariance)) return cacheValidationRes(false);
        //Ensure same generic arguments wit same or stricter restrictions
        if(parent != null && parent instanceof DefInstance di){
           var dummyInstance = new LinkedList<Instance>();
           for(int i = 0; i < generics.size(); i++){
               var gen = generics.get(i);
               var bound = gen.bound();
               if(bound != null) bound = bound.substitute(dummyInstance);
               var nGen = new GenericInstance(gen.variance(), i, bound);
               dummyInstance.add(nGen);
               var other = di.getArguments().get(i).substitute(dummyInstance);
               if(!nGen.fulfills(other)) return cacheValidationRes(false);
           }

           var res = new DefInstance(this, dummyInstance).fulfills(parent.substitute(dummyInstance));
           if(!res) return cacheValidationRes(false);
        }
        for(Generic g: generics) {
            var bound = g.bound();
            //Note: if we had supertype bounds this could be contra as well
            if(bound != null && !bound.validateAndInfer(Variance.Covariance)) return cacheValidationRes(false);
        }
        for(Argument a: fields) if(!a.typ().validateAndInfer(Variance.Covariance)) return cacheValidationRes(false);
        for(Instance m: traits) if(!m.validateAndInfer(Variance.Covariance)) return cacheValidationRes(false);
        //this ensures that the primitive one is included
        var kids = getChildren();
        if(!abstr && !kids.isEmpty()) return cacheValidationRes(false);

        var modulePath = fullyQualifiedName.split("::")[0]+"::";
        for(Data c: kids) {
            // will trigger a cycle: luckily abstract and sealed is still valid
            //if(!c.validate()) return cacheValidationRes(false);
            if(sealed) {
                if(c.isAbstr() && !c.isSealed()) return cacheValidationRes(false);
                if(!c.getFullyQualifiedName().startsWith(modulePath)){
                    return cacheValidationRes(false);
                }
            }
        }
        return cacheValidationRes(true);
    }

    @Override
    public boolean validateApplication(List<Instance> arguments, Variance position) {
        if(!validateAndInfer()) return false;
        if(generics.size() != arguments.size()) {
            //Are we a special unrestricted VarArg Def
            // Like tuples & function
            return generics.size() == 0 && TypeCheckContext.isSpecialVarargDef(this);
        }
        int i = 0;
        var finalGenerics = generics;
        if(TypeCheckContext.isSpecialVarargDef(this)) {
            finalGenerics = TypeCheckContext.getVarargVariance(this, arguments).stream()
                    .map(v -> new Generic("", v, null, null)).toList();
        }
        for(Instance arg:arguments) {
            var gen = finalGenerics.get(i++);
            if(position == null){
                if(!arg.validateAndInfer(null)) return false;
            } else {
                if(!arg.validateAndInfer(position.flipBy(gen.variance()))) return false;
            }
            var bound = gen.bound();
            if(bound == null) continue;
            var genTyp = bound.substitute(arguments);
            if(!arg.fulfills(genTyp)) return false;
        }
        return true;
    }

    @Override
    public void generateCode(Path buildRoot) {
        var path = targetFile(buildRoot);
        try {
            Files.createDirectories(path.getParent());
            try (OutputStreamWriter out = new OutputStreamWriter(Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
                out.append("<name>: ").append(fullyQualifiedName).append("\n");
                if(handler != null){
                    out.append("<handler>: ");
                    if(!handler.contains("#")) out.append("nora_core#");
                    out.append(handler).append("()").append("\n");
                }
                if (parent instanceof DefInstance di) {
                    out.append("<super>: ");
                    out.append(di.getBase().getFullyQualifiedName());
                    out.append("\n");
                }
                out.append("<kind>: ");
                if (abstr) {
                    out.append("<abstract>").append("\n");
                } else {
                    out.append("<concrete>").append("\n");
                }

                var type = TypeCollector.get().resolveType(this);
                out.append("<info>: ").append(type.toString()).append("\n");

                out.append("<generics>: ");
                for (Iterator<Generic> it = generics.iterator(); it.hasNext(); ) {
                    out.append(it.next().variance().generateVarianceSign(true));
                    if (it.hasNext()) out.append(", ");
                }

                out.append("\n").append("<fields>: ").append("\n");
                for(Argument arg: fields){
                    var argT = TypeCollector.get().resolveTypeString(arg.typ());
                    out.append(arg.name()).append(":").append(argT).append("\n");
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}
