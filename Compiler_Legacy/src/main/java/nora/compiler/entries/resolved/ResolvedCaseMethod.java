package nora.compiler.entries.resolved;

import nora.compiler.entries.*;
import nora.compiler.entries.exprs.Expr;
import nora.compiler.entries.interfaces.Function;
import nora.compiler.processing.TypeCollector;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ResolvedCaseMethod extends ResolvedAbstractFunction implements Function {
    private final Definition before;
    private final Definition after;

    private final Instance multiMethod;
    private final List<Argument> dynamicArgs;
    private final List<Argument> staticArgs;

    public ResolvedCaseMethod(String fullyQualifiedName, Function.RecursionMode mode, Definition before, Definition after, Instance multiMethod, List<Argument> dynamicArgs, List<Argument> staticArgs, Instance returnType, Expr body, List<Generic> generics) {
        super(fullyQualifiedName, mode, returnType, body, generics);
        this.before = before;
        this.after = after;
        this.multiMethod = multiMethod;
        this.dynamicArgs = dynamicArgs;
        this.staticArgs = staticArgs;
    }

    public boolean validateAndInfer() {
        if(valid != null) return valid;
        if(!super.validateAndInfer()) return cacheValidationRes(false);
        if(multiMethod instanceof DefInstance di){
            if(!di.isMultiMethod()) return cacheValidationRes(false);
            //It is kinda in a supertype position, so variance must match
            if(!multiMethod.validateAndInfer(Variance.Covariance)) return cacheValidationRes(false);
            //Helper for constraint and generic seen check
            // As this is not the simplest algo most methods are in a seperate file
            var generics = getGenerics();
            var algoRes = CaseGenericInference.checkWitchState(generics.size(), state -> {
                var parentFun = di.getBase().asMultiMethod();
                var applies = di.getArguments();
                var parentGens = parentFun.getGenerics();
                if(applies.size() != parentGens.size()) return cacheValidationRes(false);
                int i = 0;
                for(Instance appl:applies){
                    var gen = parentGens.get(i);
                    var placeHolder = new GenericInstance(gen.variance(), i, gen.bound());
                    CaseGenericInference.inferenceFromValType(appl,placeHolder,state);
                    i++;
                }
                //check applied parent dynamic args are in same hierarchy as case dyn args
                var parDyn = parentFun.getDynamicArgs();
                if (parDyn.size() != dynamicArgs.size()) return cacheValidationRes(false);
                i = 0;
                for (Argument arg : dynamicArgs) {
                    var parArgT = parDyn.get(i++).typ().substitute(applies);
                    var argT = arg.typ();
                    //Note: While this technically is a Bivariant position
                    //      The arguments of it are checked covariantly
                    //      The DefInstance takes care of it
                    if(!argT.validateAndInfer(Variance.Bivariance)) return cacheValidationRes(false);
                    //MixedSubCheck
                    if (!parArgT.subType(argT) && !argT.subType(parArgT)) {
                        return cacheValidationRes(false);
                    }
                    //Generic seen check (result is only available at end)
                    CaseGenericInference.inferenceFromValType(argT,parArgT,state);
                }
                //check applied parent static args are subtypes of case static args
                var statDyn = parentFun.getStaticArgs();
                if (statDyn.size() != staticArgs.size()) return cacheValidationRes(false);
                i = 0;
                for (Argument arg : staticArgs) {
                    var parArgT = statDyn.get(i++).typ().substitute(applies);
                    var argT = arg.typ();
                    if(!argT.validateAndInfer(Variance.Contravariance)) return cacheValidationRes(false);
                    //NormalSubCheck
                    if (!parArgT.subType(argT)) return cacheValidationRes(false);
                    //Generic seen check (result is only available at end)
                    CaseGenericInference.inferenceFromValType(argT,parArgT,state);
                }

                //check case return type is subtypes of applied parent return type
                var parRet = parentFun.getReturnType().substitute(applies);
                if(!getReturnType().subType(parRet)) return cacheValidationRes(false);

                //Generic seen check (result is only available at end)
                CaseGenericInference.inferenceFromValType(getReturnType(),parRet,state);
                return true;
            });
            if(!algoRes) return cacheValidationRes(false);
            return cacheValidationRes(true);
        } else {
            return cacheValidationRes(false);
        }
    }

    public Instance getMultiMethod() {
        return multiMethod;
    }

    @Override
    public Kind getKind() {
        return Kind.Function;
    }

    @Override
    public List<Argument> getArgs() {
        var args = new LinkedList<>(dynamicArgs);
        args.addAll(staticArgs);
        return args;
    }

    @Override
    protected Instance extraReturnHint() {
        if(multiMethod.isCallable() && multiMethod instanceof DefInstance di){
            return di.getBase().asCallable().getReturnType().substitute(di.getArguments());
        } else {
            return null;
        }
    }

    public List<Argument> getDynamicArgs() {
        return dynamicArgs;
    }

    public List<Argument> getStaticArgs() {
        return staticArgs;
    }

    public Definition getBefore() {
        return before;
    }

    public Definition getAfter() {
        return after;
    }

    public void generateDispatchInfo(OutputStreamWriter out) throws IOException {
        out.append(fullyQualifiedName);
        var generics = getGenerics().size();
        if(multiMethod instanceof DefInstance di && di.getArguments() != null && !di.getArguments().isEmpty()){
            out.append("[");
            if(generics != 0) {
                out.append(String.valueOf(generics)).append("|");
            }
            for (Iterator<Instance> it = di.getArguments().iterator(); it.hasNext();) {
                out.append(TypeCollector.get().resolveTypeString(it.next()));
                if (it.hasNext()) out.append(", ");
            }
            out.append("]");
        } else if(generics != 0){
            out.append("[").append(String.valueOf(generics)).append("|]");
        }
        out.append("(");
        for (Iterator<Argument> it = dynamicArgs.iterator(); it.hasNext();) {
            var dynArg = TypeCollector.get().resolveTypeString(it.next().typ(), true);
            out.append(dynArg);
            if (it.hasNext() || !staticArgs.isEmpty()) out.append(", ");
        }
        for (Iterator<Argument> it = staticArgs.iterator(); it.hasNext();) {
            var statArg = TypeCollector.get().resolveTypeString(it.next().typ(), true);
            out.append(statArg);
            if (it.hasNext()) out.append(", ");
        }

        out.append(") -> ");
        var ret = TypeCollector.get().resolveTypeString(getReturnType(), true);
        out.append(ret);
    }
}
