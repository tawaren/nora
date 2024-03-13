package nora.meta.core;

import com.oracle.truffle.api.staticobject.StaticShape;
import meta.MetaObjectLayoutHandler;
import nora.vm.loading.Loader;
import nora.vm.nodes.property.PropertyManager;
import nora.vm.runtime.data.RuntimeData;
import nora.vm.runtime.data.RuntimeDataFactory;
import nora.vm.truffle.NoraLanguage;
import nora.vm.types.Type;
import nora.vm.types.TypeInfo;
import nora.vm.types.schemas.DataSchemaHandler;
import nora.vm.types.schemas.DefaultDataSchemaHandler;
import nora.vm.types.schemas.InheritableDataSchemaHandler;
import nora.vm.types.schemas.SchemaManager;

import java.util.Arrays;
import java.util.List;

public class ObjectLayoutHandler implements MetaObjectLayoutHandler {

    private final int publicTypes;
    private final int publicFields;

    //Todo: do we need list of Method names?
    public ObjectLayoutHandler(int publicTypes, int publicFields) {
        this.publicTypes = publicTypes;
        this.publicFields = publicFields;
    }


    private PropertyManager[] addAllProps(SchemaManager manager, Loader.DataType data, int start, int end, StaticShape.Builder schema) {
        var fields = data.fieldTypes().stream().limit(publicFields).toArray(Loader.TypedField[]::new);
        PropertyManager[] props = new PropertyManager[fields.length - start];
        var fieldTypes = data.fieldTypes();
        assert fieldTypes.size() >= end;
        for(int i = start; i < end ; i++){
            var field = fieldTypes.get(i);
            props[i-start] = manager.addFieldProperty(schema,field.name(),field.typ());
        }
        return props;
    }

    //Todo: What if: data X[A|B] extends Y[A|B]
    //      I think:  data X[A|B] extends X[A]
    //              & abstract X[A] extends Y[A]
    //      Reason: field accessor use X[A], as they do not know B, thus X[A|B] must be an X[A]
    //      Consequence: X[A|B] must include private properties of all parents as those are not inherited
    //      Problem: if Y[A|B] has methods, they access properties over Y[A|B] which is not parent of X[A|B]
    //      Solution: only concrete types can have hidden methods, abstracts ones can only provide signatures no impls
    //                However, this is akward with external Methods, thus instead limit hidden params to concrete data types
    //
    @Override
    public DataSchemaHandler handleLayout(SchemaManager manager, Loader.DataType data) {
        var applies = data.plainApplies();
        if(applies.length == publicTypes){
            DataSchemaHandler parent = null;
            if(data.superData() != null) parent = manager.getDataHandlerFor(data.superData(), applies);
            if(parent instanceof InheritableDataSchemaHandler idh){
                StaticShape.Builder schema = StaticShape.newBuilder(NoraLanguage.get(null));
                var props = addAllProps(manager, data, 0, publicFields, schema);
                var shape = schema.build(idh.schema);
                return new PublicObjectLayout(data.name(), idh,shape,props, data.typ());
            } else if(parent == null){
                StaticShape.Builder schema = StaticShape.newBuilder(NoraLanguage.get(null));
                var props = addAllProps(manager, data, 0, publicFields, schema);
                var shape = schema.build(RuntimeData.class, RuntimeDataFactory.class);
                return new PublicObjectLayout(data.name(), null,shape,props, data.typ());
            }
            throw new RuntimeException("Parent is not a valid super type");
        } else {
            //we are the child schema with hidden properties
            var publicGenerics = Arrays.stream(applies).limit(publicTypes).toArray(Type[]::new);
            var parent = (PublicObjectLayout)manager.getDataHandlerFor(data.name(), publicGenerics);
            StaticShape.Builder schema = StaticShape.newBuilder(NoraLanguage.get(null));
            var props = addAllProps(manager, data, publicFields, data.fieldTypes().size() , schema);
            // Todo: note methods names?
            var shape = schema.build(parent.schema);
            return new PrivateObjectLayout(data.name(),parent, shape.getFactory(), applies, props);
        }
    }
}
