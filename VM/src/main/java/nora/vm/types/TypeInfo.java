package nora.vm.types;

import com.oracle.truffle.api.CompilerDirectives;
import nora.vm.runtime.data.IdentityObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TypeInfo extends IdentityObject {
    final int cat;
    final int start;
    final int end;
    //Todo: later we need these as instance but this needs plenty of change
    private static final Map<TypeInfo,TypeInfo> stableInfos = new HashMap<>();

    public TypeInfo(int cat, int start, int end) {
        this.cat = cat;
        this.start = start;
        this.end = end;
    }

    @CompilerDirectives.TruffleBoundary
    public TypeInfo stabilized(){
        return stableInfos.computeIfAbsent(this, t -> this);
    }


    public boolean subTypeOf(TypeInfo other){
        if(cat != other.cat) return false;
        return start >= other.start && end <= other.end;
    }

    public boolean isAssignableFromConcrete(int id){
        return id >= start && id <= end;
    }

    public boolean isConcrete() {
        return start == end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeInfo typeInfo = (TypeInfo) o;
        return cat == typeInfo.cat && start == typeInfo.start && end == typeInfo.end;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cat, start, end);
    }

    public int id() {
        return end;
    }

    public int cat(){
        return cat;
    }

    @Override
    public String toString() {
        return "TypeInfo{" +
                "cat=" + cat +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}
