package nora.vm.loading;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import meta.MetaObjectLayoutHandler;
import nora.vm.method.EntryPoint;
import nora.vm.method.GenericMultiMethod;
import nora.vm.runtime.NoraVmContext;
import nora.vm.types.TypeUtil;
import nora.vm.types.Type;
import nora.vm.types.pattern.TypePattern;
import org.antlr.v4.runtime.misc.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class Loader {
    private final String base;
    public Loader(String base) {
        this.base = base;
    }

    private final MultiMethodLoader multi = new MultiMethodLoader();
    private final TypeFamilyLoader family = new TypeFamilyLoader();
    private final MethodLoader method = new MethodLoader();
    private final DataLoader data = new DataLoader();

    private Path pathFromId(String id){
        var pathString = base+File.separator+id.replace(".", File.separator).replace("::",File.separator)+".nora";
        return Path.of(pathString);
    }

    @TruffleBoundary
    public GenericMultiMethod loadGenericMultiMethod(String id) throws IOException {
        return multi.parse(pathFromId(id));
    }

    public final Map<Integer, TypeFamilyLoader.TypeData[]> families = new HashMap<>();

    @TruffleBoundary
    public TypeFamilyLoader.TypeData[] loadFullTypeFamily(int c) {
        return families.computeIfAbsent(c, k -> family.parse(Path.of(base,".types", c+".nora")));
    }


    /*
    public Type loadType(String id) {
        var type = TypeLoader.parse(pathFromId(id), this);
        //Todo: Better Exception
        if(!Objects.equals(type.name, id)) throw new IllegalStateException("??");
        return TypeLoader.parse(pathFromId(id), this);
    }
    */



    //We need to overwrite as standard record do not handle arrays well
    private record ApplyKey(String id, Type[] generics){
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ApplyKey applyKey = (ApplyKey) o;
            return id.equals(applyKey.id) && Arrays.equals(generics, applyKey.generics);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(id);
            result = 31 * result + Arrays.hashCode(generics);
            return result;
        }
    }

    private final Map<String, Pair<TypePattern[], EntryPoint>> methodLoadCache = new HashMap<>();
    private final Map<ApplyKey, EntryPoint> entryCache = new HashMap<>();
    private final Map<ApplyKey, Type> sigCache = new HashMap<>();

    private Pair<TypePattern[], EntryPoint> loadMethod(String id){
        return methodLoadCache.computeIfAbsent(id, k -> method.parse(pathFromId(id)));
    }

    @TruffleBoundary
    public EntryPoint loadSpecializedMethod(String id, Type[] generics){
        var key = new ApplyKey(id,generics);
        return entryCache.computeIfAbsent(key, k -> {
            var ep = loadMethod(id);
            try {
                var argSig = Arrays.stream(ep.a).map(t -> t.buildTypeParam(generics)).toArray(Type.TypeParameter[]::new);
                return ep.b.specialise(id, argSig, generics);
            } catch (Exception e) {
                //Todo: Better error Handling
                e.printStackTrace();
                return null;
            }
        });
    }

    @TruffleBoundary
    public Type loadMethodSig(String id, Type[] generics){
        TypeUtil util = NoraVmContext.getTypeUtil(null);
        var key = new ApplyKey(id,generics);
        return sigCache.computeIfAbsent(key, k -> {
            var ep = loadMethod(id);
            var sig = new Type.TypeParameter[ep.a.length];
            for(int i = 0; i < sig.length; i++){
                sig[i] = ep.a[i].buildTypeParam(generics);
            }
            try {
                return new Type(util.FunctionTypeInfo, sig);
            } catch (Exception e){
                e.printStackTrace();
                return null;
            }
        });
    }

    public record TypedField(String name, Type typ){};
    public record DataType(String name, MetaObjectLayoutHandler handler, String superData, boolean isConcrete, Type typ, List<TypedField> fieldTypes){
        //Todo: Can we cache?
        public Type[] plainApplies() {
            return Arrays.stream(typ.applies).map(Type.TypeParameter::type).toArray(Type[]::new);
        }

    }
    private final Map<String, DataLoader.DataInfo> dataLoadCache = new HashMap<>();
    //Todo: is this cache needed or superseeded by the SchemaManagers Cache?
    private final Map<ApplyKey, DataType> dataTypeCache = new HashMap<>();

    private DataLoader.DataInfo loadData(String id){
        return dataLoadCache.computeIfAbsent(id, k -> data.parse(pathFromId(id)));
    }

    @TruffleBoundary
    public DataType loadSpecializedData(String id, Type[] generics){
        var key = new ApplyKey(id,generics);
        return dataTypeCache.computeIfAbsent(key, k -> {
            var dp = loadData(id);
            if(dp.handler() != null) {
                dp = dp.handler().extendLoad(dp, generics);
            }
            var applies = new Type.TypeParameter[generics.length];
            var variances = dp.variances();
            //Todo: We have a vararg problem here with tuple
            assert applies.length == variances.length;
            for(int i = 0; i < applies.length; i++) applies[i] = new Type.TypeParameter(variances[i], generics[i]);
            var fieldTypes = dp.fieldTypes().stream()
                    .map(ft -> new TypedField(ft.name(),ft.type().buildType(generics))).toList();
            return new DataType(dp.name(),dp.handler(),dp.superData(), dp.isConcrete(), new Type(dp.info().stabilized(), applies), fieldTypes);
        });
    }
}
