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
import nora.vm.types.Type;
import nora.vm.types.TypeInfo;
import nora.vm.types.TypeUtil;

import java.math.BigInteger;

@ReportPolymorphism
public abstract class FullTypeOfUtilNode extends Node {
    TypeUtil util = NoraVmContext.getTypeUtil(this);
    public abstract Type executeTypeOf(Object o);
    public abstract Type executeTypeOf(int i);
    public abstract Type executeTypeOf(long l);
    public abstract Type executeTypeOf(boolean b);

    @Specialization
    public Type getBooleanType(boolean bool){
        return util.BooleanType;
    }

    @Specialization
    public Type getIntIntegerType(int value){
        return util.IntType;
    }

    @Specialization
    public Type getLongIntegerType(long value){
        return util.NumType;
    }

    @Specialization
    public Type getBigIntegerType(BigInteger value){
        return util.NumType;
    }

    @Specialization
    public Type getStringType(TruffleString str){
        return util.StringType;
    }

    @Specialization
    public Type getRuntimeDataType(RuntimeData rd){
        return rd.getType();
    }

    @Specialization
    public Type getMethodType(Method md){
        return md.getType();
    }

    @Specialization
    public Type getMultiMethodType(MultiMethod md){
        return md.getType();
    }

    @Specialization
    public Type getClosureType(ClosureData cd){
        return cd.funType;
    }

    @Specialization
    public Type getTypeType(TypeInfo t){
        return util.TypeType;
    }

    @Specialization
    public Type getGenericType(Object o){
        return util.extractFullType(o);
    }

}
