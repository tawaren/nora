package nora.vm.nodes.type;

import com.oracle.truffle.api.CompilerAsserts;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.types.TypeInfo;
import nora.vm.types.TypeUtil;

public abstract class SwitchNode extends NoraNode {

    public static NoraNode create(TypeInfo typ, NoraNode inner) {
        CompilerAsserts.neverPartOfCompilation();
        TypeUtil util = NoraVmContext.getTypeUtil(null);
        if(typ == util.ByteTypeInfo) return new ByteSwitchNode(inner);
        if(typ == util.IntTypeInfo) return new IntSwitchNode(inner);
        if(typ == util.BooleanTypeInfo) return new BooleanSwitchNode(inner);
        if(typ == util.NumTypeInfo) return NumericSwitchNodeGen.create(inner);
        return inner;
    }

    public static NoraNode safeCreate(TypeInfo typ, NoraNode inner) {
        CompilerAsserts.neverPartOfCompilation();
        if(!inner.isUnboxed()) return inner;
        if(inner instanceof CachedNode cn){
            return new CacheNode(create(typ,cn.liftCache()));
        } else {
            return create(typ, inner);
        }
    }

    @Override
    public int complexity() {
        return super.complexity()-1; //take our self out again
    }


}
