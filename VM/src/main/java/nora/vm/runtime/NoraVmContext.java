package nora.vm.runtime;

import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.nodes.Node;
import nora.vm.loading.Loader;
import meta.MetaLanguageProtocol;
import nora.vm.specTime.SpecialMethodRegistry;
import nora.vm.truffle.NoraLanguage;
import nora.vm.types.DispatchCoordinator;
import nora.vm.types.TypeUtil;
import nora.vm.types.schemas.SchemaManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

//Todo: Make this the context class and (Call it NoraVMContext)
public class NoraVmContext {
    private final Loader loaderInstance;
    private final SchemaManager manager;
    private final DispatchCoordinator dispatchCoordinator;
    private final SpecialMethodRegistry specialMethodRegistry;
    private final TypeUtil typeUtil;
    private final MethodCreator methodFactory;
    private final ValueCache valueCache;
    private final Path base;
    private final Map<String, MetaLanguageProtocol> metaProtocolByName = new HashMap<>();
    private final Map<Class<?>, MetaLanguageProtocol> metaProtocolByClass = new HashMap<>();
    public final boolean runSave;

    public NoraVmContext(String base, boolean runSave) {
        //Todo: Shall we move this into context init?
        this.loaderInstance = new Loader(base);
        this.runSave = runSave;
        this.manager = new SchemaManager();
        this.specialMethodRegistry = new SpecialMethodRegistry();
        this.dispatchCoordinator = new DispatchCoordinator();
        this.typeUtil = new TypeUtil();
        this.methodFactory = new MethodCreator();
        this.valueCache = new ValueCache();
        this.base = Path.of(base);
    }

    public void init() throws Exception{
        var meta = Files.readAllLines(base.resolve(".meta.nora"));
        for(String m:meta) {
            var res = (MetaLanguageProtocol)Class.forName(m).getConstructor().newInstance();
            metaProtocolByName.put(res.getName(), res);
            metaProtocolByClass.put(res.getClass(), res);
            res.initialize(this);
        }
        specialMethodRegistry.init();
    }

    public Loader getLoader() {
        return loaderInstance;
    }

    public static Loader getLoader(Node node){
        return get(node).getLoader();
    }

    public SchemaManager getSchemaManager() {
        return manager;
    }

    public static SchemaManager getSchemaManager(Node node){
        return get(node).getSchemaManager();
    }

    public SpecialMethodRegistry getSpecialMethodRegistry() {
        return specialMethodRegistry;
    }

    public static SpecialMethodRegistry getSpecialMethodRegistry(Node node){
        return get(node).getSpecialMethodRegistry();
    }

    public DispatchCoordinator getDispatchCoordinator() {
        return dispatchCoordinator;
    }

    public static DispatchCoordinator getDispatchCoordinator(Node node){
        return get(node).getDispatchCoordinator();
    }

    public TypeUtil getTypeUtil() {
        return typeUtil;
    }

    public static TypeUtil getTypeUtil(Node node){
        return get(node).getTypeUtil();
    }

    public MethodCreator getMethodFactory() {
        return methodFactory;
    }

    public ValueCache getValueCache() {
        return valueCache;
    }

    public static ValueCache getValueCache(Node node) {
        return get(node).getValueCache();
    }

    public static MethodCreator getMethodFactory(Node node){
        return get(node).getMethodFactory();
    }

    public Path getBase() {
        return base;
    }

    @SuppressWarnings("unchecked")
    public <T> T getMetaProtocol(Class<T> cls){
        return (T)metaProtocolByClass.get(cls);
    }
    public MetaLanguageProtocol getMetaProtocol(String name){
        return metaProtocolByName.get(name);
    }

    private static final ContextReference<NoraVmContext> REFERENCE =
            ContextReference.create(NoraLanguage.class);

    public static NoraVmContext get(Node node) {
        return REFERENCE.get(node);
    }

    public void close() {
        //Todo: do we need to do something
    }
}
