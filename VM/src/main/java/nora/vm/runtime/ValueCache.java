package nora.vm.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.strings.TruffleString;
import nora.vm.nodes.EnsureOrRetNode;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.cache.EarlyRetCacheNode;
import nora.vm.nodes.cache.ManagedCacheNode;
import nora.vm.nodes.cache.ReferenceCacheNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.nodes.consts.ConstRetNode;
import nora.vm.runtime.data.ClosureData;
import nora.vm.runtime.data.RuntimeData;
import nora.vm.types.schemas.ClosureSchemaHandler;
import nora.vm.types.schemas.DataSchemaHandler;

import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate;

public class ValueCache {

    private int accessEventTime = 0;

    //Todo: Maybe have primitive subtypes?
    public class CacheEntry {
        private int accessTime = accessEventTime++;
        private int accessCount = 1;

        private Object object;

        public CacheEntry(Object object) {
            this.object = object;
        }

        public Object access() {
            accessTime = accessEventTime++;
            accessCount++;
            return object;
        }
    }


    private static int objectArraySize(Object[] arr, int budget, int depthLimit){
        budget -= 1;
        for(int i = 1; i < arr.length; i++){
            if(budget < 0) return budget;
            budget = objectSizeCheck(arr[i], budget, depthLimit-1);
        }
        return budget;
    }

    public static int objectSizeCheck(Object obj, int budget, int depthLimit){
        return switch (obj) {
            case RuntimeData rd -> rd.handler.size(rd, budget, depthLimit);
            case ClosureData rd -> rd.handler.size(rd, budget, depthLimit);
            case TruffleString ts -> budget - (1 + ((ts.codePointLengthUncached(TruffleString.Encoding.UTF_16) >> 1)));
            case BigInteger bi -> budget - ((bi.bitLength() >> 5) + 1);
            case Object[] oArr -> objectArraySize(oArr, budget, depthLimit);
            case boolean[] bArr -> budget - (bArr.length/4) - 1;
            case byte[] bArr -> budget - (bArr.length/4) - 1;
            case int[] iArr -> budget - iArr.length - 1;
            case long[] lArr -> budget - lArr.length - 1;
            default -> budget - 1; //is something that counts as small
        };
    }

    //Todo: Replace with a LRU ShortTerm and FrequencyBased LongTerm cache
    private List<CacheEntry> hardCache = new LinkedList<>();
    @TruffleBoundary
    public <T> NoraNode createCached(NoraNode origin, T value) {
        //let cache manager do this -- todo: make configurable
        var remSize = objectSizeCheck(value, 30,15);
        if(remSize <= 0) {
            return createManagedCached(new ManagedCacheNode(origin), value);
        } else {
            return ConstNode.create(value);
        }
    }

    @TruffleBoundary
    public NoraNode createEarlyRetCached(NoraNode origin, EnsureOrRetNode.EarlyReturnException retExp) {
        //let cache manager do this -- todo: make configurable
        var remSize = objectSizeCheck(retExp.getValue(), 30,15);
        if(remSize <= 0) {
            return createManagedCached(new ManagedCacheNode(origin), retExp);
        } else {
            return new ConstRetNode(retExp);
        }
    }

    public <T> NoraNode createManagedCached(ManagedCacheNode cacheNode, T value) {
        var entry = new CacheEntry(value);
        hardCache.add(entry);
        return new ReferenceCacheNode(cacheNode, new WeakReference<>(entry));
    }

    public NoraNode createEarlyRetManagedCached(ManagedCacheNode cacheNode, EnsureOrRetNode.EarlyReturnException retExp) {
        var entry = new CacheEntry(retExp);
        hardCache.add(entry);
        return new EarlyRetCacheNode(cacheNode, new WeakReference<>(entry));
    }

}
