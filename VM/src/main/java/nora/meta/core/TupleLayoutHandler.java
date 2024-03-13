package nora.meta.core;

import com.oracle.truffle.api.staticobject.StaticShape;
import nora.vm.loading.DataLoader;
import nora.vm.loading.Loader;
import meta.MetaObjectLayoutHandler;
import nora.vm.types.schemas.DataSchemaHandler;
import nora.vm.nodes.property.PropertyManager;
import nora.vm.types.schemas.SchemaManager;
import nora.vm.runtime.data.RuntimeData;
import nora.vm.runtime.data.RuntimeDataFactory;
import nora.vm.truffle.NoraLanguage;
import nora.vm.types.Type;
import nora.vm.types.TypeInfo;

import java.util.Arrays;
import java.util.List;

public class TupleLayoutHandler implements MetaObjectLayoutHandler {
    static final String tupleIdentifier = "nora.lang.Primitives::Tuple";
    //We need a way to coordinate those between Metalanguage features

    @Override
    public DataSchemaHandler handleLayout(SchemaManager manager, Loader.DataType data) {
        StaticShape.Builder schema = StaticShape.newBuilder(NoraLanguage.get(null));
        var applies = data.typ().applies;
        PropertyManager[] props = new PropertyManager[applies.length];
        for(int i = 0; i < props.length; i++){
            props[i] = manager.addFieldProperty(schema,"_"+i,applies[i].type());
        }
        var shape = schema.build(RuntimeData.class, RuntimeDataFactory.class);
        return new TupleLayout(shape.getFactory(),props,data.typ());
    }

    @Override
    public DataLoader.DataInfo extendLoad(DataLoader.DataInfo info, Type[] generics) {
        var newVariance = Arrays.stream(generics).map( v -> Type.Variance.Co).toArray(Type.Variance[]::new);
        return new DataLoader.DataInfo(
                info.name(),
                this,
                info.superData(),
                info.isConcrete(),
                info.info(),
                newVariance,
                info.fieldTypes()
        );
    }
}
