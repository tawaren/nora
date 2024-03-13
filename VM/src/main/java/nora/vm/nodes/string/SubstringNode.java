package nora.vm.nodes.string;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.strings.TruffleString;
import nora.vm.nodes.IntIndexNodeGen;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

public class SubstringNode extends NoraNode {
    @Child NoraNode stringNode;
    @Child NoraNode startIndex;
    @Child NoraNode lengthNode;
    @Child TruffleString.SubstringNode substring = TruffleString.SubstringNode.create();
    public SubstringNode(NoraNode stringNode, NoraNode startIndex, NoraNode lengthNode) {
        this.stringNode = stringNode;
        this.startIndex = startIndex;
        this.lengthNode = lengthNode;
    }

    @Override
    public TruffleString executeString(VirtualFrame frame) throws UnexpectedResultException {
        return substring.execute(
                stringNode.executeString(frame),
                startIndex.executeInt(frame),
                lengthNode.executeInt(frame),
                TruffleString.Encoding.UTF_16, false
        );
    }

    @Override
    public Object execute(VirtualFrame virtualFrame) {
        try {
            return executeString(virtualFrame);
        } catch (UnexpectedResultException e) {
            throw new RuntimeException("Typ Mismatch");
        }
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        var stringRes = stringNode.specialise(frame);
        var startRes = IntIndexNodeGen.create(startIndex).specialise(frame);
        var lengthRes = IntIndexNodeGen.create(lengthNode).specialise(frame);
        if(stringRes instanceof ConstNode && startRes instanceof ConstNode && lengthRes instanceof ConstNode){
            var strVal = stringRes.executeString(null);
            var startVal = startRes.executeInt(null);
            var lenVal = startRes.executeInt(null);
            var substring = TruffleString.SubstringNode.getUncached();
            return ConstNode.create(substring.execute(strVal,startVal,lenVal,TruffleString.Encoding.UTF_16, false));
        } else if(stringRes instanceof CachedNode cn1 && startRes instanceof CachedNode cn2 && lengthRes instanceof CachedNode cn3) {
            return new CacheNode(new SubstringNode(cn1.liftCache(), cn2.liftCache(), cn3.liftCache()));
        } else {
            frame.markNonTrivial();
            return new SubstringNode(stringRes, startRes,lengthRes);
        }
    }

    @Override
    public String toString() {
        return "Substring(" +stringNode+", " + startIndex +", "+lengthNode+")";
    }

    @Override
    public SubstringNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return new SubstringNode(stringNode.cloneUninitialized(),startIndex.cloneUninitialized(), lengthNode.cloneUninitialized());
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return NoraVmContext.getTypeUtil(null).StringType;
    }

}
