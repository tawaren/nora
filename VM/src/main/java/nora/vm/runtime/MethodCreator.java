package nora.vm.runtime;

import nora.vm.method.EntryPoint;
import nora.vm.method.Method;
import nora.vm.nodes.ClosureNode;
import nora.vm.nodes.property.setter.NoraPropertySetNode;
import nora.vm.runtime.data.ClosureData;
import nora.vm.types.Type;
import nora.vm.types.schemas.ClosureSchemaHandler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MethodCreator {


    private record DedupKey(String methodIdentifier, Type[] generics) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DedupKey dedupKey = (DedupKey) o;
            return methodIdentifier.equals(dedupKey.methodIdentifier) && Arrays.equals(generics, dedupKey.generics);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(methodIdentifier);
            result = 31 * result + Arrays.hashCode(generics);
            return result;
        }
    }

    private final Map<DedupKey,Method> methodCache = new HashMap<>();

    public Method create(String methodIdentifier, Type[] generics){
        var key = new DedupKey(methodIdentifier, generics);
        return methodCache.computeIfAbsent(key, m -> new Method(methodIdentifier, generics));
    }

}
