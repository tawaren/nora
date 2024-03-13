package nora.vm.nodes.utils;

//import com.oracle.truffle.api.dsl.GenerateInline;

import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;
import nora.vm.method.Method;
import nora.vm.method.MultiMethod;
import nora.vm.runtime.NoraVmContext;
import nora.vm.types.TypeUtil;
import nora.vm.runtime.data.ClosureData;
import nora.vm.runtime.data.RuntimeData;
import nora.vm.types.TypeInfo;

import java.math.BigInteger;

//@GenerateInline(false)
@ReportPolymorphism
public abstract class ConcreteTypeOfUtilNode extends Node {
    TypeUtil util = NoraVmContext.getTypeUtil(this);

    public abstract int executeIndexTypeOf(Object o);
    public abstract int executeIndexTypeOf(byte i);
    public abstract int executeIndexTypeOf(int i);
    public abstract int executeIndexTypeOf(long l);
    public abstract int executeIndexTypeOf(boolean b);


    @Specialization
    public int getBooleanType(boolean bool){
        return util.BooleanTypeInfo.id();
    }

    @Specialization
    public int getByteType(byte value){
        return util.ByteTypeInfo.id();
    }

    @Specialization
    public int getIntType(int value){
        return util.IntTypeInfo.id();
    }

    //Todo: can we have one that is after long & BigInteger but before generic?
    @Specialization
    public int getLongIntegerType(long value){
        return util.NumTypeInfo.id();
    }

    @Specialization
    public int getBigIntegerType(BigInteger value){
        return util.NumTypeInfo.id();
    }

    @Specialization(guards = "isIntegerType(value)")
    public int getCombIntegerType(Object value){
        return util.NumTypeInfo.id();
    }

    protected static boolean isIntegerType(Object value){
        return value instanceof Long || value instanceof BigInteger;
    }

    @Specialization
    public int getStringType(TruffleString str){
        return util.StringTypeInfo.id();
    }

    @Specialization
    public int getRuntimeDataType(RuntimeData rd){
        return rd.typeIndex;
    }

    //Todo: Can we shortcut???
    @Specialization
    public int getMethodType(Method md){
        return md.getType().info.id();
    }

    //Todo: Can we shortcut???
    @Specialization
    public int getMultiMethodType(MultiMethod md){
        return md.getType().info.id();
    }

    //Todo: Can we shortcut???
    @Specialization
    public int getClosureType(ClosureData cd){
        return cd.funType.info.id();
    }

    //Todo: Can we shortcut???
    @Specialization
    public int getTypeType(TypeInfo t){
        return util.TypeTypeInfo.id();
    }

    @Specialization
    public int getGenericType(Object o){
        return util.extractTypeIndex(o);
    }

}
