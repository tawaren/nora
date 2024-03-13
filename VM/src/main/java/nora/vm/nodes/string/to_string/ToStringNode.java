package nora.vm.nodes.string.to_string;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

public class ToStringNode extends NoraNode {
    public final static int MAX_RECURSION_DEPTH = 4;
    @Child private NoraNode val;
    //MAX_RECURSION_DEPTH ensures that we always start with a call lets truffle decide to do inline or not
    @Child private ToStringPlainNode toString = ToStringPlainNodeGen.create(MAX_RECURSION_DEPTH);
    @Child private TruffleStringBuilder.ToStringNode finish = TruffleStringBuilder.ToStringNode.create();

    public ToStringNode(NoraNode val) {
        this.val = val;
    }

    @Override
    public TruffleString executeString(VirtualFrame virtualFrame) throws UnexpectedResultException {
        var builder = TruffleStringBuilder.createUTF16();
        toString.execute(builder,val.execute(virtualFrame));
        return finish.execute(builder);
    }

    @Override
    public Object execute(VirtualFrame virtualFrame) {
        try {
            return executeString(virtualFrame);
        } catch (UnexpectedResultException e) {
            throw new RuntimeException("Type Missmatch");
        }
    }


    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        var leftRes = val.specialise(frame);
        if(leftRes instanceof ConstNode){
            var lVal = leftRes.execute(null);
            var builder = TruffleStringBuilder.createUTF16();
            toString.execute(builder,lVal);
            return ConstNode.create(finish.execute(builder));
        } else if(leftRes instanceof CachedNode cn1) {
            return new CacheNode(new ToStringNode(cn1.liftCache()));
        } else {
            var util = NoraVmContext.getTypeUtil(null);
            if(!util.isSimpleType(leftRes.getType(frame).info)){
                //Sadly we do not know how expensive this will be
                //Todo: we could ask schema
                frame.markNonTrivial();
            }
            return new ToStringNode(leftRes);
        }
    }

    @Override
    public String toString() {
        return "ToString(" +val+")";
    }

    @Override
    public ToStringNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return new ToStringNode(val.cloneUninitialized());
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return NoraVmContext.getTypeUtil(null).StringType;
    }

}
