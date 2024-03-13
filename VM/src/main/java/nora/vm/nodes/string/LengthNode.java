package nora.vm.nodes.string;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.strings.TruffleString;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.nodes.consts.IntNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate;

public class LengthNode extends NoraNode {
    @Child NoraNode left;
    @Child TruffleString.CodePointLengthNode codepointLength = TruffleString.CodePointLengthNode.create();

    public LengthNode(NoraNode left) {
        this.left = left;
    }

    @Override
    public int executeInt(VirtualFrame virtualFrame) throws UnexpectedResultException {
        return codepointLength.execute(
                left.executeString(virtualFrame),
                TruffleString.Encoding.UTF_16
        );
    }

    @Override
    public Object execute(VirtualFrame virtualFrame) {
        try {
            return executeInt(virtualFrame);
        } catch (UnexpectedResultException e) {
            transferToInterpreterAndInvalidate();
            throw new RuntimeException("Not a Int");
        }
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        var leftRes = left.specialise(frame);
        if(leftRes instanceof ConstNode){
            var lVal = leftRes.executeString(null);
            return ConstNode.create(codepointLength.execute(lVal,TruffleString.Encoding.UTF_16));
        } else if(leftRes instanceof CachedNode cn1) {
            return new CacheNode(new LengthNode(cn1.liftCache()));
        } else {
            return new LengthNode(leftRes);
        }
    }

    @Override
    public String toString() {
        return "Length(" +left+")";
    }

    @Override
    public LengthNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return new LengthNode(left.cloneUninitialized());
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return NoraVmContext.getTypeUtil(null).IntType;
    }

}
