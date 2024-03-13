package meta;

import nora.vm.loading.DataLoader;
import nora.vm.loading.Loader;
import nora.vm.types.Type;
import nora.vm.types.schemas.DataSchemaHandler;
import nora.vm.types.schemas.SchemaManager;

import java.util.List;

public interface MetaObjectLayoutHandler extends MetaLanguageObject {
    DataSchemaHandler handleLayout(SchemaManager manager, Loader.DataType data);
    default DataLoader.DataInfo extendLoad(DataLoader.DataInfo info, Type[] generics){
        return info;
    }
}
