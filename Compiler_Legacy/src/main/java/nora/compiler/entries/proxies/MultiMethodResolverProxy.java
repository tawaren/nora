package nora.compiler.entries.proxies;

import nora.compiler.entries.Instance;
import nora.compiler.entries.interfaces.*;
import nora.compiler.entries.resolved.Argument;
import nora.compiler.entries.resolved.ResolvedCaseMethod;
import nora.compiler.entries.resolved.Generic;
import nora.compiler.entries.resolved.Variance;
import nora.compiler.entries.unresolved.RawMultiMethod;

import java.util.List;

public class MultiMethodResolverProxy extends BaseProxy<MultiMethod, RawMultiMethod> implements MultiMethod {

    public MultiMethodResolverProxy(RawMultiMethod rawMultiMethod) {
        super(rawMultiMethod);
    }

    @Override
    public Instance getReturnType() {
        return onResolved(MultiMethod::getReturnType);
    }

    @Override
    public List<Generic> getGenerics() {
        return onResolved(MultiMethod::getGenerics);
    }

    @Override
    public int numGenerics() {
        return onAny(MultiMethod::numGenerics, RawMultiMethod::numGenerics);
    }

    @Override
    public List<Argument> getArgs() {
        return onResolved(MultiMethod::getArgs);
    }

    @Override
    public void addCaseMethod(ResolvedCaseMethod cm) {
        onAny(m -> {m.addCaseMethod(cm);return null;},r -> {r.addCaseMethod(cm);return null;});
    }

    @Override
    public boolean isSealed() {
        return onAny(MultiMethod::isSealed, RawMultiMethod::isSealed);
    }

    @Override
    public boolean isPartial() {
        return onAny(MultiMethod::isPartial, RawMultiMethod::isPartial);
    }

    @Override
    public List<Argument> getDynamicArgs() {
        return onResolved(MultiMethod::getDynamicArgs);
    }

    @Override
    public List<Argument> getStaticArgs() {
        return onResolved(MultiMethod::getStaticArgs);
    }

    @Override
    public boolean validateApplication(List<Instance> arguments, Variance position) {
        return onResolved(r -> r.validateApplication(arguments, position));
    }

    @Override
    public MultiMethod asMultiMethod() {
        var res = super.asMultiMethod();
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
