package nora.compiler.entries.resolved;

import nora.compiler.entries.Instance;
import nora.compiler.entries.interfaces.Callable;
import nora.compiler.entries.interfaces.MultiMethod;
import nora.compiler.entries.interfaces.Parametric;
import nora.compiler.entries.interfaces.WithArguments;
import nora.compiler.processing.MethodCaseSorter;
import nora.compiler.processing.TypeCollector;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class ResolvedMultiMethod extends ResolvedDefinition implements MultiMethod {
    private final boolean sealed;
    private final boolean partial;

    private final Instance returnType;
    private final List<Generic> generics;
    private final List<Argument> dynamicArgs;
    private final List<Argument> staticArgs;
    private final List<ResolvedCaseMethod> caseMethods;


    public ResolvedMultiMethod(String fullyQualifiedName, boolean sealed, boolean partial, Instance returnType, List<Generic> generics, List<Argument> dynamicArgs, List<Argument> staticArgs, List<ResolvedCaseMethod> caseMethods) {
        super(fullyQualifiedName);
        this.sealed = sealed;
        this.partial = partial;
        this.returnType = returnType;
        this.generics = generics;
        this.dynamicArgs = dynamicArgs;
        this.staticArgs = staticArgs;
        this.caseMethods = caseMethods;
    }

    //Todo: I do not like but we still need as this may be resolved before the case Methods
    public void addCaseMethod(ResolvedCaseMethod caseM){
        caseMethods.add(caseM);
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
        valid = false; //Case method may call back
        if(!returnType.validateAndInfer(Variance.Covariance)) return cacheValidationRes(false);
        for(Generic g:generics) {
            var bound = g.bound();
            //Todo: See similar other places for the distinction
            //      We need better compiler, this comment is at way to many places
            if(bound != null && !bound.validateAndInfer(Variance.Covariance)) return cacheValidationRes(false);
        }
        for(Argument stat:staticArgs) if(!stat.typ().validateAndInfer(Variance.Contravariance)) return cacheValidationRes(false);
        for(Argument dyn:dynamicArgs){
            if(!dyn.typ().validateAndInfer(Variance.Bivariance)) return cacheValidationRes(false);
        }
        valid = true; //Case method may call back
        var modulePath = fullyQualifiedName.split("::")[0]+"::";
        for(ResolvedCaseMethod cm:caseMethods){
            if(!cm.validateAndInfer()) return cacheValidationRes(false);
            if(sealed) {
                if(!cm.getFullyQualifiedName().startsWith(modulePath)){
                    return cacheValidationRes(false);
                }
            }
        }
        caseMethods.sort(new MethodCaseSorter());
        //Todo: check for ambiguity - later allow for sensitivity config
        var ambiguityChecker = new MethodCaseSorter(3);
        //Todo: ensure before conditions hold
        Set<String> seen = new HashSet<>();
        ResolvedCaseMethod prev = null;
        for(ResolvedCaseMethod cur: caseMethods){
            if(prev != null){
                if(ambiguityChecker.compare(prev, cur) == 0){
                    System.out.println("Resolution order of Case Methods: "+prev.fullyQualifiedName+" and "+cur.fullyQualifiedName+" is ambigous");
                }
            }
            seen.add(cur.fullyQualifiedName);
            if(cur.getBefore() != null && seen.contains(cur.getBefore().getFullyQualifiedName())){
                System.out.println("Could not enforce @Before annotation on "+cur+" as "+cur.getBefore().getFullyQualifiedName()+" has a more specific signature");
            }
            if(cur.getAfter() != null && !seen.contains(cur.getAfter().getFullyQualifiedName())){
                System.out.println("Could not enforce @After annotation on "+cur+" as "+cur.getAfter().getFullyQualifiedName()+" has a less specific signature");
            }
            prev = cur;
        }


        //Todo: check completness? how is hard - especially if markers are involfed
        //      see if: https://doc.rust-lang.org/nightly/nightly-rustc/rustc_mir_build/thir/pattern/usefulness/index.html
        //  helps
        return cacheValidationRes(true);
    }

    @Override
    public boolean validateApplication(List<Instance> arguments, Variance position) {
        if(!validateAndInfer()) return false;
        if(generics.size() != arguments.size()) {
            return false;
        }
        int i = 0;
        for(Instance arg:arguments) {
            var gen = generics.get(i++);
            if(gen.variance() != null) throw new RuntimeException("Function generics are not supposed to have a variance");
            var bound = gen.bound();
            if(!arg.validateAndInfer(null)) return false;
            if(bound == null) continue;
            var genTyp = bound.substitute(arguments);
            if(!arg.fulfills(genTyp)) return false;
        }
        return true;
    }

    @Override
    public Kind getKind() {
        return Kind.MultiMethod;
    }

    @Override
    public MultiMethod asMultiMethod() {
        return this;
    }

    @Override
    public Callable asCallable() {
        return this;
    }

    @Override
    public Parametric asParametric() {
        return this;
    }

    @Override
    public WithArguments asWithArguments() {
        return this;
    }

    public boolean isSealed() {
        return sealed;
    }

    public boolean isPartial() {
        return partial;
    }

    public Instance getReturnType() {
        return returnType;
    }

    public List<Generic> getGenerics() {
        return generics;
    }

    public int numGenerics() {
        return generics.size();
    }

    @Override
    public List<Argument> getArgs() {
        var allArgs = new LinkedList<Argument>();
        allArgs.addAll(dynamicArgs);
        allArgs.addAll(staticArgs);
        return allArgs;
    }

    public List<Argument> getDynamicArgs() {
        return dynamicArgs;
    }

    public List<Argument> getStaticArgs() {
        return staticArgs;
    }

    @Override
    public String toString() {
        return fullyQualifiedName;
    }

    @Override
    public void generateCode(Path buildRoot) {
        var path = targetFile(buildRoot);
        try {
            Files.createDirectories(path.getParent());
            try (OutputStreamWriter out = new OutputStreamWriter(Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
                out.append("<sig>: (");
                for (Iterator<Argument> it = dynamicArgs.iterator(); it.hasNext(); ) {
                    var argT = TypeCollector.get().resolveTypeString(it.next().typ(), true);
                    out.append(argT);
                    if (it.hasNext()) out.append(", ");
                }
                out.append(") | (");
                for (Iterator<Argument> it = staticArgs.iterator(); it.hasNext(); ) {
                    var argT = TypeCollector.get().resolveTypeString(it.next().typ(), true);
                    out.append(argT);
                    if (it.hasNext()) out.append(", ");
                }
                out.append(") -> ");
                var retT = TypeCollector.get().resolveTypeString(returnType);
                out.append(retT).append("\n");
                out.append("<methods>:\n");
                for (ResolvedCaseMethod m : caseMethods) {
                    m.generateDispatchInfo(out);
                    out.append("\n");
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}
