package nora.compiler.entries.proxies;

import nora.compiler.entries.Instance;
import nora.compiler.entries.interfaces.*;
import nora.compiler.entries.resolved.Argument;
import nora.compiler.entries.resolved.Generic;
import nora.compiler.entries.resolved.Variance;
import nora.compiler.entries.unresolved.RawData;

import java.util.List;
import java.util.Set;

public class DataResolverProxy extends BaseProxy<Data, RawData> implements Data {

    public DataResolverProxy(RawData rawData) {
        super(rawData);
    }

    @Override
    public boolean isSealed() {
        return onAny(Data::isSealed, RawData::isSealed);
    }

    @Override
    public boolean isAbstr() {
        return onAny(Data::isAbstr, RawData::isAbstr);
    }

    @Override
    public Data getParent() {
        return onResolved(Data::getParent);
    }

    @Override
    public List<Generic> getGenerics() {
        return onResolved(Data::getGenerics);
    }

    @Override
    public int numGenerics() {
        return onAny(Data::numGenerics, RawData::numGenerics);
    }

    @Override
    public Set<Instance> getTraits() {
        return onResolved(Data::getTraits);
    }

    @Override
    public void addChild(Data child) {
        onAny(d -> {d.addChild(child); return null;}, r -> {r.addChild(child); return null;});
    }

    @Override
    public void addPrimitiveChild(Data child) {
        onAny(d -> {d.addPrimitiveChild(child); return null;}, r -> {r.addPrimitiveChild(child); return null;});
    }

    @Override
    public List<Data> getChildren() {
        return onResolved(Data::getChildren);
    }

    /*@Override
    public void addExtraMarkers(Set<Trait> trait) {
        onAny(d -> {d.addExtraMarkers(trait); return null;}, r -> {r.addExtraMarkers(trait); return null;});
    }*/

    @Override
    public boolean subTypeOf(Data otherData) {
        return onResolved(d -> d.subTypeOf(otherData));
    }

    @Override
    public Argument getFieldByName(String field) {
        return onResolved(d -> d.getFieldByName(field));
    }

    @Override
    public void fillFields(List<Argument> fieldCol) {
        onResolved(d -> {d.fillFields(fieldCol); return null;});
    }

    @Override
    public boolean validateApplication(List<Instance> arguments, Variance position) {
        return onResolved(r -> r.validateApplication(arguments, position));
    }

    @Override
    public List<Argument> getArgs() {
        return onResolved(Data::getArgs);
    }

    @Override
    public Integer getTypId() {
        return onAny(Data::getTypId, RawData::getTypId);
    }

    @Override
    public boolean hasTrait(Trait trait) {
        return onResolved(t -> t.hasTrait(trait));
    }

    @Override
    public Data asData() {
        var res = super.asData();
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
    public WithTraits asWithTraits() {
        var res = super.asWithTraits();
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
