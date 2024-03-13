package nora.vm.types;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.strings.TruffleString;
import java.util.Arrays;
import java.util.Objects;

public class Type {

    public enum Variance {
        Co, Contra, Inv
    }

    public record TypeParameter(Variance variance, Type type){}
    public static final TypeParameter[] NO_APPLIES = new TypeParameter[0];
    public static final Type[] NO_GENERICS = new Type[0];

    public TruffleString toTruffleString() {
        return TruffleString.fromJavaStringUncached(toString(),TruffleString.Encoding.UTF_16);
    }

    public final TypeInfo info;
    @CompilationFinal(dimensions = 1)
    public final TypeParameter[] applies;

    public Type(TypeInfo info, TypeParameter[] applies) {
        this.info = info;
        this.applies = applies;
    }

    public Type(TypeInfo info) {
        this(info,NO_APPLIES);
    }

    //stable versions allow to use == to check for equals
    @TruffleBoundary
    public Type stabilized(){
        if(info.stabilized() == info) return this;
        return new Type(info.stabilized(), applies);
    }

    //Assumes that abstract types without concrete subtypes are pruned
    public boolean isConcrete(){
        return info.start == info.end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Type type = (Type) o;
        return info.equals(type.info) && Arrays.equals(applies, type.applies);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(info);
        result = 31 * result + Arrays.hashCode(applies);
        return result;
    }

    @Override
    public String toString() {
        return info+ Arrays.toString(applies);
    }

    public static boolean appliesSubType(TypeParameter[] applies, TypeParameter[] otherApplies){
        assert applies.length == otherApplies.length;
        for(int i = 0; i < applies.length; i++){
            assert applies[i].variance == otherApplies[i].variance;
            var res = switch (applies[i].variance){
                case Co -> applies[i].type.subTypeOf(otherApplies[i].type);
                case Contra -> otherApplies[i].type.subTypeOf(applies[i].type);
                case Inv -> applies[i].type.sameType(otherApplies[i].type);
            };
            if(!res) return false;
        }
        return true;
    }


    public boolean subTypeOf(Type typ) {
        if(!info.subTypeOf(typ.info)) return false;
        return appliesSubType(applies, typ.applies);
    }

    private static boolean sameApplies(TypeParameter[] applies, TypeParameter[] otherApplies){
        if(applies.length != otherApplies.length) return false;
        for(int i = 0; i < applies.length; i++){
            assert applies[i].variance == otherApplies[i].variance;
           if(!applies[i].type.sameType(otherApplies[i].type)) return false;
        }
        return true;
    }
    public boolean sameType(Type other) {
        if(!info.equals(other.info)) return false;
        return sameApplies(applies, other.applies);
    }

    public boolean isRelatedTo(Type other) {
        if(info.cat() != other.info.cat()) return false;
        if(applies.length != other.applies.length) return false;
        for(int i = 0; i < applies.length; i++){
            if(!applies[i].type.isRelatedTo(other.applies[i].type)) return false;
        }
        return true;
    }
}
