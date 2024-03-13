package nora.vm.types;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Idempotent;
import com.oracle.truffle.api.strings.TruffleString;
import nora.vm.method.Method;
import nora.vm.method.MultiMethod;
import nora.vm.runtime.data.ClosureData;
import nora.vm.runtime.data.RuntimeData;

import java.math.BigInteger;

public class TypeUtil {

    public enum TypeKind {
        BOOL,
        BYTE,
        INT,
        NUM,
        STRING,
        FUNCTION,
        ARRAY,
        DATA,
        TYPE
    }

    public final TypeInfo FunctionTypeInfo = new TypeInfo(-1, 0,0).stabilized();
    public final TypeInfo TypeTypeInfo = new TypeInfo(-2, 0,0).stabilized();
    public final TypeInfo BooleanTypeInfo = new TypeInfo(-3, 0,0).stabilized();
    public final TypeInfo ByteTypeInfo = new TypeInfo(-4, 0,0).stabilized();
    public final TypeInfo IntTypeInfo = new TypeInfo(-5, 0,0).stabilized();
    public final TypeInfo NumTypeInfo = new TypeInfo(-6, 0,0).stabilized();

    public  final TypeInfo StringTypeInfo = new TypeInfo(-7, 0,0).stabilized();
    public  final TypeInfo ArrayTypeInfo = new TypeInfo(-8, 0,0).stabilized();


    //Dispatchable (as soon as they are in a hierarchy adapt the indexes)
    public final Type TypeType = new Type(TypeTypeInfo).stabilized();
    public final Type BooleanType = new Type(BooleanTypeInfo).stabilized();
    public final Type ByteType = new Type(ByteTypeInfo).stabilized();
    public final Type IntType = new Type(IntTypeInfo).stabilized();
    public final Type NumType = new Type(NumTypeInfo).stabilized();
    public final Type StringType = new Type(StringTypeInfo).stabilized();
    public final Type BooleanArrayType = new Type(ArrayTypeInfo, new Type.TypeParameter[]{new Type.TypeParameter(Type.Variance.Co,BooleanType)}).stabilized();
    public final Type ByteArrayType = new Type(ArrayTypeInfo, new Type.TypeParameter[]{new Type.TypeParameter(Type.Variance.Co,ByteType)}).stabilized();
    public final Type IntArrayType = new Type(ArrayTypeInfo, new Type.TypeParameter[]{new Type.TypeParameter(Type.Variance.Co,IntType)}).stabilized();
    public final Type NumArrayType = new Type(ArrayTypeInfo, new Type.TypeParameter[]{new Type.TypeParameter(Type.Variance.Co,NumType)}).stabilized();


    public TypeInfo extractTypeInfo(Object arg) {
        if(arg instanceof RuntimeData rd) return rd.getTypeInfo();
        if(arg instanceof TruffleString) return StringTypeInfo;
        if(arg instanceof Byte) return ByteTypeInfo;
        if(arg instanceof Integer) return IntTypeInfo;
        if(arg instanceof Long) return NumTypeInfo;
        if(arg instanceof BigInteger) return NumTypeInfo;
        if(arg instanceof Boolean) return BooleanTypeInfo;
        if(arg instanceof Type) return TypeTypeInfo;
        if(arg instanceof byte[]) return ArrayTypeInfo;
        if(arg instanceof int[]) return ArrayTypeInfo;
        if(arg instanceof long[]) return ArrayTypeInfo;
        if(arg instanceof Object[]) return ArrayTypeInfo;
        if(arg instanceof ClosureData) return FunctionTypeInfo;
        if(arg instanceof Method) return FunctionTypeInfo;
        if(arg instanceof MultiMethod) return FunctionTypeInfo;
        return null;
    }

    public int extractTypeIndex(Object arg) {
        if(arg instanceof RuntimeData rd) return rd.typeIndex;
        var res = extractTypeInfo(arg);
        if(res == null)return 0;
        return res.id();
    }

    public Type extractFullType(Object arg) {
        CompilerAsserts.neverPartOfCompilation();
        assert arg != null;
        if(arg instanceof RuntimeData rd) return rd.getType();
        if(arg instanceof TruffleString) return StringType;
        if(arg instanceof Byte) return ByteType;
        if(arg instanceof Integer) return IntType;
        if(arg instanceof Long) return NumType;
        if(arg instanceof BigInteger) return NumType;
        if(arg instanceof Boolean) return BooleanType;
        if(arg instanceof Type) return TypeType;
        if(arg instanceof byte[]) return ByteArrayType;
        if(arg instanceof int[]) return IntArrayType;
        if(arg instanceof long[]) return NumArrayType;
        if(arg instanceof Object[] arr) return (Type) arr[0];
        if(arg instanceof MultiMethod md) return md.getType();
        if(arg instanceof ClosureData cd) return cd.getType();
        if(arg instanceof Method m) return m.getType();
        assert false;
        return null;
    }

    public TypeKind getKind(TypeInfo typ){
        if(typ == FunctionTypeInfo) return TypeKind.FUNCTION;
        if(typ == ArrayTypeInfo) return TypeKind.ARRAY;
        if(isSchemaType(typ)) return TypeKind.DATA;
        if(typ == BooleanTypeInfo) return TypeKind.BOOL;
        if(typ == ByteTypeInfo) return TypeKind.BYTE;
        if(typ == IntTypeInfo) return TypeKind.INT;
        if(typ == NumTypeInfo) return TypeKind.NUM;
        if(typ == StringTypeInfo) return TypeKind.STRING;
        if(typ == TypeTypeInfo) return TypeKind.TYPE;

        return null;
    }

    @Idempotent
    public static boolean isObject(TypeKind kind){
        return kind == TypeKind.DATA
                || kind == TypeKind.FUNCTION
                || kind == TypeKind.ARRAY
                || kind == TypeKind.STRING
                || kind == TypeKind.TYPE;
    }

    @Idempotent
    public boolean isSchemaType(Type typ) {
        var info = typ.info;
        return isSchemaType(info);
    }

    @Idempotent
    public boolean isSchemaType(TypeInfo info){
        return info.id() != 0 || info.cat() > TypeTypeInfo.cat() || info.cat() < ArrayTypeInfo.cat();
    }

    @Idempotent
    public boolean isNativeType(Type typ){
        return !isSchemaType(typ);
    }
    @Idempotent
    public boolean isSimpleType(TypeInfo typ){
        return typ == BooleanTypeInfo
                || typ == ByteTypeInfo
                || typ == IntTypeInfo
                || typ == NumTypeInfo;
    }


    //Todo: use more often
    public static final int TypeSwitchOffset = 0;
    public static final int BooleanSwitchOffset = 1;
    public static final int ByteSwitchOffset = 2;
    public static final int IntSwitchOffset = 3;
    public static final int NumSwitchOffset = 4;
    public static final int StringSwitchOffset = 5;
    public static final int ArraySwitchOffset = 6;

    //Must be guarded by is primitive
    public int nativeSwitchOffset(Type typ){
        return -(typ.info.cat()+2);
    }



}
