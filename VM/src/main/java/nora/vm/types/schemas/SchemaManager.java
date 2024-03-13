package nora.vm.types.schemas;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.staticobject.DefaultStaticProperty;
import com.oracle.truffle.api.staticobject.StaticProperty;
import com.oracle.truffle.api.staticobject.StaticShape;
import com.oracle.truffle.api.strings.TruffleString;
import nora.vm.loading.Loader;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.property.PropertyManager;
import nora.vm.nodes.property.reader.*;
import nora.vm.nodes.property.setter.*;
import nora.vm.runtime.NoraVmContext;
import nora.vm.runtime.data.ClosureData;
import nora.vm.runtime.data.ClosureDataFactory;
import nora.vm.runtime.data.RuntimeData;
import nora.vm.runtime.data.RuntimeDataFactory;
import nora.vm.truffle.NoraLanguage;
import nora.vm.types.Type;
import nora.vm.types.TypeInfo;
import nora.vm.types.TypeUtil;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SchemaManager {

    public record BooleanProperty(String name, StaticProperty prop) implements PropertyManager {
        @Override public String getName() { return name; }
        @Override public NoraPropertySetNode getSetter(NoraNode value) { return new BooleanPropertySetNode(prop, value);}
        @Override public NoraPropertyGetNode getGetter() { return new BooleanPropertyGetNode(prop); }
    }

    public record ObjectProperty(String name, StaticProperty prop, Type type) implements PropertyManager {
        @Override public String getName() { return name; }
        @Override public NoraPropertySetNode getSetter(NoraNode value) { return new ObjectPropertySetNode(prop, value);}
        @Override public NoraPropertyGetNode getGetter() { return new ObjectPropertyGetNode(prop, type); }
        public StaticProperty getProperty() { return prop;}
    }

    public record ByteProperty(String name, StaticProperty byteProp) implements PropertyManager {
        @Override public String getName() { return name; }
        @Override public NoraPropertySetNode getSetter(NoraNode value) { return new BytePropertySetNode(byteProp, value);}
        @Override public NoraPropertyGetNode getGetter() { return new BytePropertyGetNode(byteProp); }
    }


    public record IntProperty(String name, StaticProperty intProp) implements PropertyManager {
        @Override public String getName() { return name; }
        @Override public NoraPropertySetNode getSetter(NoraNode value) { return new IntPropertySetNode(intProp, value);}
        @Override public NoraPropertyGetNode getGetter() { return new IntegerPropertyGetNode(intProp); }
    }

    public record NumericProperty(String name, StaticProperty longProp, StaticProperty bigProp) implements PropertyManager {
        @Override public String getName() { return name; }
        @Override public NoraPropertySetNode getSetter(NoraNode value) { return new NumericPropertySetNode(longProp, bigProp, value);}
        @Override public NoraPropertyGetNode getGetter() { return new NumericPropertyGetNode(longProp, bigProp); }
    }

    @TruffleBoundary
    public PropertyManager addFieldProperty(StaticShape.Builder schema, String name, Type field){
        TypeUtil util = NoraVmContext.getTypeUtil(null);
        if(field.info == util.BooleanTypeInfo) {
            StaticProperty prop = new DefaultStaticProperty(name);
            schema.property(prop, boolean.class, true);
            return new BooleanProperty(name, prop);
        } else if(field.info == util.ByteTypeInfo) {
            assert !field.info.equals(util.BooleanTypeInfo);
            StaticProperty prop = new DefaultStaticProperty(name);
            schema.property(prop, byte.class, true);
            return new ByteProperty(name, prop);
        } else if(field.info == util.IntTypeInfo) {
            assert !field.info.equals(util.BooleanTypeInfo);
            assert !field.info.equals(util.ByteTypeInfo);
            StaticProperty prop = new DefaultStaticProperty(name);
            schema.property(prop, int.class, true);
            return new IntProperty(name, prop);
        } else if(field.info == util.NumTypeInfo) {
            assert !field.info.equals(util.BooleanTypeInfo);
            assert !field.info.equals(util.ByteTypeInfo);
            assert !field.info.equals(util.IntTypeInfo);
            StaticProperty prop1 = new DefaultStaticProperty(name);
            StaticProperty prop2 = new DefaultStaticProperty(name+"$Big");
            schema.property(prop1, long.class, true);
            schema.property(prop2, BigInteger.class, true);
            return new NumericProperty(name,prop1,prop2);
        } else {
            assert !field.info.equals(util.BooleanTypeInfo);
            assert !field.info.equals(util.ByteTypeInfo);
            assert !field.info.equals(util.IntTypeInfo);
            assert !field.info.equals(util.NumTypeInfo);
            StaticProperty prop = new DefaultStaticProperty(name);
            Class clazz = Object.class;
            if(field.info == util.StringTypeInfo) clazz = TruffleString.class;
            else if(field.info == util.TypeTypeInfo) clazz = TypeInfo.class;
            schema.property(prop, clazz, true);
            return new ObjectProperty(name,prop,field);
        }
    }

    private final Map<Type, DataSchemaHandler> schemas = new HashMap<>();
    private DataSchemaHandler buildDataSchema(Loader.DataType dataT){
        var parentName = dataT.superData();
        StaticShape.Builder schema = StaticShape.newBuilder(NoraLanguage.get(null));
        var fields = dataT.fieldTypes();
        PropertyManager[] props = new PropertyManager[fields.size()];
        int i = 0;
        for(Loader.TypedField field: fields){
            props[i] = addFieldProperty(schema,field.name(),field.typ());
            i++;
        }
        DataSchemaHandler handler;
        if(parentName != null) {
            var parentType = NoraVmContext.getLoader(null).loadSpecializedData(parentName, dataT.plainApplies());
            DataSchemaHandler parentSchema = schemas.get(parentType.typ());
            if(parentSchema == null) parentSchema = buildDataSchema(parentType);
            if(parentSchema instanceof DefaultDataSchemaHandler ddh){
                StaticShape<RuntimeDataFactory> shape = schema.build(ddh.schema);
                handler = new DefaultDataSchemaHandler(dataT.name(), dataT.typ(), ddh, shape, props);
            } else {
                throw new RuntimeException("Can not extend custom schema");
            }
        } else {
            StaticShape<RuntimeDataFactory> shape = schema.build(RuntimeData.class, RuntimeDataFactory.class);
            handler = new DefaultDataSchemaHandler(dataT.name(), dataT.typ(), null, shape, props);
        }
        return handler;
    }

    @TruffleBoundary
    private DataSchemaHandler getDataHandlerFor(Loader.DataType dataT){
        var handler = schemas.get(dataT.typ());
        if(handler != null) return handler;
        if(dataT.handler() != null) {
            handler = dataT.handler().handleLayout(this,dataT);
        } else {
            handler = buildDataSchema(dataT);
        }
        assert schemas.get(dataT.typ()) == null;
        schemas.put(dataT.typ(), handler);
        return handler;
    }

    @TruffleBoundary
    public DataSchemaHandler getDataHandlerFor(String id, Type[] generics){
        var dataT = NoraVmContext.getLoader(null).loadSpecializedData(id, generics);
        return getDataHandlerFor(dataT);
    }

    private record CaptKey(Type[] captures){
        //We use it in a away where it is guaranteed that other is a CaptKey
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return Arrays.equals(captures, ((CaptKey) o).captures);
        }
        @Override
        public int hashCode() {
            return Arrays.hashCode(captures);
        }
    }

    private final Map<CaptKey, ClosureSchemaHandler> cloSchemas = new HashMap<>();
    private ClosureSchemaHandler buildClosureSchema(Type[] captures){
        StaticShape.Builder schema = StaticShape.newBuilder(NoraLanguage.get(null));
        var capts = new PropertyManager[captures.length];
        for(int i = 0; i < capts.length; i++){
            capts[i] = addFieldProperty(schema,"_"+i, captures[i]);
        }
        StaticShape<ClosureDataFactory> shape = schema.build(ClosureData.class, ClosureDataFactory.class);
        return new DefaultClosureSchemaHandler(shape.getFactory(), capts);
    }

    @TruffleBoundary
    public ClosureSchemaHandler getClosureHandlerFor(Type[] captures){
        var key = new CaptKey(captures);
        var handler = cloSchemas.get(key);
        if(handler != null) return handler;
        handler = buildClosureSchema(captures);
        cloSchemas.put(key, handler);
        return handler;
    }
}
