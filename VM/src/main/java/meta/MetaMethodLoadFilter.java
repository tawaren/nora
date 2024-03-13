package meta;

public interface MetaMethodLoadFilter extends MetaLanguageObject{
    boolean loadCheck(String methodIdentifier);
}
