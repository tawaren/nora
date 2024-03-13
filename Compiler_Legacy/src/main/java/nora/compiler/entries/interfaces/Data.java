package nora.compiler.entries.interfaces;

import nora.compiler.entries.Definition;
import nora.compiler.entries.resolved.Argument;

import java.util.LinkedList;
import java.util.List;

public interface Data extends Definition, WithArguments, WithTraits {

    boolean isSealed();
    boolean isAbstr();
    Data getParent();
    Integer getTypId();

    void addChild(Data child);
    void addPrimitiveChild(Data child);
    List<Data> getChildren();

    default int countChildren() {
        int sum = 1;
        for(Data child:getChildren() ) sum+=child.countChildren();
        return sum;
    }

    //void addExtraMarkers(Set<Trait> trait);

    //Todo: these may need more support when markers support generics
    boolean subTypeOf(Data otherData);

    Argument getFieldByName(String field);
    void fillFields(List<Argument> fieldCol);

    default List<Argument> getFields() {
        List<Argument> col = new LinkedList<>();
        fillFields(col);
        return col;
    }


}
