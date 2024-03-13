package meta;


import nora.vm.runtime.NoraVmContext;

public interface MetaLanguageProtocol {
    MetaLanguageObject createObject(String name, Object... args);
    void initialize(NoraVmContext context) throws Exception;
    String getName();
}
