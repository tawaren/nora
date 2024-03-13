package nora.vm.types.schemas;

import com.oracle.truffle.api.strings.TruffleStringBuilder;
import nora.vm.nodes.string.to_string.data.DataToStringNode;
import nora.vm.runtime.data.ClosureData;
import nora.vm.runtime.data.NoraData;

public class ClosureToStringNode extends DataToStringNode {
    @Child private TruffleStringBuilder.AppendJavaStringUTF16Node appendNode = TruffleStringBuilder.AppendJavaStringUTF16Node.create();

    @Override
    public void executeToString(TruffleStringBuilder builder, NoraData data) {
        appendNode.execute(builder, "<Closure@");
        appendNode.execute(builder, ((ClosureData)data).identity.toString());
        appendNode.execute(builder, ">");
    }

    @Override
    public DataToStringNode cloneUninitialized() {
        return this;
    }
}
