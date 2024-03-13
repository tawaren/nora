package nora.vm.nodes.utils;

import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;
import nora.vm.method.Method;
import nora.vm.method.MultiMethod;
import nora.vm.runtime.NoraVmContext;
import nora.vm.runtime.data.ClosureData;
import nora.vm.runtime.data.RuntimeData;
import nora.vm.types.TypeUtil;
import nora.vm.types.TypeInfo;

import java.math.BigInteger;

@ReportPolymorphism
public abstract class TypeOfUtilNode extends Node {
    TypeUtil util = NoraVmContext.getTypeUtil(this);
    public abstract TypeInfo executeTypeOf(Object o);
    public abstract TypeInfo executeTypeOf(int i);
    public abstract TypeInfo executeTypeOf(long l);
    public abstract TypeInfo executeTypeOf(boolean b);

    @Specialization
    public TypeInfo getBooleanType(boolean bool){
        return util.BooleanTypeInfo;
    }

    @Specialization
    public TypeInfo getIntIntegerType(int value){
        return util.IntTypeInfo;
    }

    @Specialization
    public TypeInfo getLongIntegerType(long value){
        return util.NumTypeInfo;
    }

    @Specialization
    public TypeInfo getBigIntegerType(BigInteger value){
        return util.NumTypeInfo;
    }

    @Specialization
    public TypeInfo getStringType(TruffleString str){
        return util.StringTypeInfo;
    }

    @Specialization
    public TypeInfo getRuntimeDataType(RuntimeData rd){
        return rd.getTypeInfo();
    }

    @Specialization
    public TypeInfo getMethodType(Method md){
        return md.getType().info;
    }

    @Specialization
    public TypeInfo getMultiMethodType(MultiMethod md){
        return md.getType().info;
    }

    @Specialization
    public TypeInfo getClosureType(ClosureData cd){
        return cd.funType.info;
    }

    @Specialization
    public TypeInfo getTypeType(TypeInfo t){
        return util.TypeTypeInfo;
    }

    @Specialization
    public TypeInfo getGenericType(Object o){
        return util.extractTypeInfo(o);
    }

}
