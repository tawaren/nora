package nora.vm.nodes.string;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.strings.TruffleString;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.compare.CmpNodeGen;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.nodes.consts.ObjectNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate;

public class ConcatNode extends NoraNode {
    @Child NoraNode left;
    @Child NoraNode right;
    @Child TruffleString.ConcatNode concat = TruffleString.ConcatNode.create();

    public ConcatNode(NoraNode left, NoraNode right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public TruffleString executeString(VirtualFrame virtualFrame) throws UnexpectedResultException {
        return concat.execute(
                left.executeString(virtualFrame),
                right.executeString(virtualFrame),
                TruffleString.Encoding.UTF_16,
                false
        );
    }


    @Override
    public Object execute(VirtualFrame virtualFrame) {
        try {
            return executeString(virtualFrame);
        } catch (UnexpectedResultException e) {
            transferToInterpreterAndInvalidate();
            throw new RuntimeException("Not a String");
        }
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        var leftRes = left.specialise(frame);
        var rightRes = right.specialise(frame);
        if(leftRes instanceof ConstNode && rightRes instanceof ConstNode){
            var lVal = leftRes.executeString(null);
            var rVal = leftRes.executeString(null);
            return ConstNode.create(concat.execute(lVal,rVal,TruffleString.Encoding.UTF_16, false));
        } else if(leftRes instanceof CachedNode cn1 && rightRes instanceof CachedNode cn2) {
            return new CacheNode(new ConcatNode(cn1.liftCache(), cn2.liftCache()));
        } else {
            frame.markNonTrivial();
            return new ConcatNode(leftRes, rightRes);
        }
    }

    @Override
    public String toString() {
        return "Concat(" +left+"," + right +")";
    }

    @Override
    public ConcatNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return new ConcatNode(left.cloneUninitialized(),right.cloneUninitialized());
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return NoraVmContext.getTypeUtil(null).StringType;
    }

}
