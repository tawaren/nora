package nora.vm.types.pattern;

import nora.vm.runtime.NoraVmContext;
import nora.vm.types.Type;
import nora.vm.types.TypeInfo;
import nora.vm.types.TypeUtil;

import java.util.Arrays;

public class TemplateTypePattern implements TypePattern{
    private final TypeInfo info;
    private final TypePattern[] applies;
    private final Type.Variance variance;

    public TemplateTypePattern(TypeInfo info, TypePattern[] applies) {
        this.variance = null;
        this.info = info;
        this.applies = applies;
    }

    private TemplateTypePattern(TypeInfo info, TypePattern[] applies, Type.Variance variance) {
        this.info = info;
        this.applies = applies;
        this.variance = variance;
    }

    private boolean correctVariance() {
        TypeUtil util = NoraVmContext.getTypeUtil(null);
        if(info.equals(util.FunctionTypeInfo)){
            for(int i = 0; i < applies.length-1; i++){
                var var = applies[i].getVariance();
                if(var != Type.Variance.Contra && var != Type.Variance.Inv) return false;
            }
            var var = applies[applies.length - 1].getVariance();
            return var == Type.Variance.Co || var == Type.Variance.Inv;
        }
        //We do not check it for other data types and trust the compiler
        //  Note asserts are only on in tests so we trust anyway
        return true;
    }

    @Override
    public Type buildType(Type[] matches) {
        var appliedTypes = new Type.TypeParameter[applies.length];
        for(int i = 0; i < applies.length; i++){
            appliedTypes[i] = applies[i].buildTypeParam(matches);
        }
        return new Type(info.stabilized(), appliedTypes);
    }

    @Override
    public Type.TypeParameter buildTypeParam(Type[] matches) {
        assert variance != null;
        return new Type.TypeParameter(variance, buildType(matches));
    }

    enum Mode {
        Sub, Super, Mixed, Fixed
    }

    private boolean appliesMatch(Type typ, Type[] matches, Mode mode){
        assert correctVariance();
        if(typ.applies.length != applies.length) return false;
        for(int i = 0; i < applies.length; i++){
            var res = switch (mode){
                case Sub -> applies[i].subtypeMatch(typ.applies[i].type(), matches);
                case Super -> applies[i].supertypeMatch(typ.applies[i].type(), matches);
                case Mixed -> applies[i].mixedTypeMatch(typ.applies[i].type(), matches);
                case Fixed -> applies[i].fixedMatch(typ.applies[i].type(), matches);
            };
            if(!res) return false;
        }
        return true;
    }

    @Override
    public MixedResult mixedDynamicArgTypeMatch(Type typ, Type[] matches) {
        assert variance == null;
        var res = MixedResult.None;
        if(typ.info.subTypeOf(info)) {
            res = MixedResult.Super;
        } else if(info.subTypeOf(typ.info)){
            res = MixedResult.Sub;
        }
        if(res == MixedResult.None) return res;
        if(appliesMatch(typ, matches, Mode.Super)) return res;
        return MixedResult.None;
    }

    @Override
    public boolean mixedTypeMatch(Type typ, Type[] matches) {
        if(variance == Type.Variance.Inv) return fixedMatch(typ, matches);
        var res = typ.info.subTypeOf(info) || info.subTypeOf(typ.info);
        if(!res) return false;
        return appliesMatch(typ, matches, Mode.Mixed);
    }

    @Override
    public boolean subtypeMatch(Type typ, Type[] matches) {
        if(variance == Type.Variance.Inv) return fixedMatch(typ, matches);
        if(variance == Type.Variance.Co || variance == null) {
            if(!info.subTypeOf(typ.info)) return false;
            return appliesMatch(typ, matches, Mode.Sub);
        } else {
            if(!typ.info.subTypeOf(info)) return false;
            return appliesMatch(typ, matches, Mode.Super);
        }
    }

    @Override
    public boolean supertypeMatch(Type typ, Type[] matches) {
        if(variance == Type.Variance.Inv) return fixedMatch(typ, matches);
        if(variance == Type.Variance.Co || variance == null) {
            if(!typ.info.subTypeOf(info)) return false;
            return appliesMatch(typ, matches, Mode.Super);
        } else {
            if(!info.subTypeOf(typ.info)) return false;
            return appliesMatch(typ, matches, Mode.Sub);
        }
    }

    @Override
    public boolean fixedMatch(Type typ, Type[] matches) {
        if(!typ.info.equals(info)) return false;
        return appliesMatch(typ, matches, Mode.Fixed);
    }
    @Override
    public Type.Variance getVariance() {
        return variance;
    }

    @Override
    public TypePattern withVariance(Type.Variance variance) {
        return new TemplateTypePattern(info, applies,  variance);
    }

    @Override
    public String toString() {
        return info.toString()+"["+ Arrays.stream(applies).map(Object::toString).toList()+"]";
    }
}
