package nora.vm.nodes.string.to_string.data;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import nora.vm.nodes.string.to_string.data.DataToStringComposeNodeGen;
import nora.vm.runtime.data.NoraData;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
import nora.vm.types.schemas.utils.ToStringTemplate;

import java.util.Arrays;

public abstract class DataToStringComposeNode extends DataToStringNode {
    @Children DataToStringNode[] toStrNodes;
    private final ToStringTemplate template;

    public DataToStringComposeNode(DataToStringNode[] toStrNodes, ToStringTemplate template) {
        this.toStrNodes = toStrNodes;
        this.template = template;
    }

    @ExplodeLoop
    @Specialization
    public void evalData(TruffleStringBuilder builder, NoraData data,
                         @Cached TruffleStringBuilder.AppendStringNode appender
    ){
        appender.execute(builder,template.start);
        CompilerAsserts.partialEvaluationConstant(toStrNodes.length);
        for(int i = 0; i < toStrNodes.length; i++){
            if(i != 0) appender.execute(builder,template.seperator);
            toStrNodes[i].executeToString(builder,data);
        }
        appender.execute(builder,template.end);
    }

    public DataToStringNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        DataToStringNode[] nToStrNodes = Arrays.stream(toStrNodes).map(DataToStringNode::cloneUninitialized).toArray(DataPropertyToStringNode[]::new);
        return DataToStringComposeNodeGen.create(nToStrNodes, template);
    }
}
