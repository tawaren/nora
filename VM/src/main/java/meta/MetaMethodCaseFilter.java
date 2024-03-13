package meta;

import nora.vm.types.Type;

public interface MetaMethodCaseFilter extends MetaLanguageObject{
   boolean preCheck(String methodIdentifier, Type[] multiMethodApplies);
   boolean postCheck(String methodIdentifier, Type[] multiMethodApplies, Type[] methodApplies);
}
