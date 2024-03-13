package nora.compiler.entries.proxies;

import nora.compiler.entries.Instance;
import nora.compiler.entries.interfaces.Callable;
import nora.compiler.entries.interfaces.Function;
import nora.compiler.entries.interfaces.Parametric;
import nora.compiler.entries.interfaces.WithArguments;
import nora.compiler.entries.resolved.Argument;
import nora.compiler.entries.resolved.Generic;
import nora.compiler.entries.resolved.ResolvedCaseMethod;
import nora.compiler.entries.resolved.Variance;
import nora.compiler.entries.unresolved.RawCaseMethod;

import java.util.List;

public class CaseMethodResolverProxy extends BaseProxy<ResolvedCaseMethod, RawCaseMethod> implements Function {

    public CaseMethodResolverProxy(RawCaseMethod rawFunction) {
        super(rawFunction);
    }

    @Override
    public Instance getReturnType() {
        return onResolved(Function::getReturnType);
    }

    @Override
    public List<Generic> getGenerics() {
        return onResolved(Function::getGenerics);
    }

    @Override
    public int numGenerics() {
        return onAny(Function::numGenerics, RawCaseMethod::numGenerics);
    }

    @Override
    public List<Argument> getArgs() {
        return onResolved(Function::getArgs);
    }

    @Override
    public boolean validateApplication(List<Instance> arguments, Variance position) {
        return onResolved(r -> r.validateApplication(arguments, position));
    }

    @Override
    public Function asFunction() {
        var res = super.asFunction();
        if(res == null) return this;
        return res;
    }

    @Override
    public Callable asCallable() {
        var res = super.asCallable();
        if(res == null) return this;
        return res;
    }

    @Override
    public Parametric asParametric() {
        var res = super.asParametric();
        if(res == null) return this;
        return res;
    }

    @Override
    public WithArguments asWithArguments() {
        var res = super.asWithArguments();
        if(res == null) return this;
        return res;
    }
}
