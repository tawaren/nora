package nora.vm.nodes.method.opt;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.staticobject.StaticProperty;
import nora.vm.runtime.data.NoraData;
import nora.vm.truffle.NoraLanguage;

@GenerateInline(false)
public abstract class CtrContextAdaptNode extends Node {

    public abstract void execute(NoraData src, StaticProperty prop, Object trg);

    @Specialization(guards = "cachedProp == prop", limit = "2")
    public void adaptCached(NoraData src, StaticProperty prop, Object trg,
                      @Cached("prop") StaticProperty cachedProp){
        cachedProp.setObject(src,trg);
    }

    @Specialization
    public void adaptGeneric(NoraData src, StaticProperty prop, Object trg){
        prop.setObject(src,trg);
    }

}
