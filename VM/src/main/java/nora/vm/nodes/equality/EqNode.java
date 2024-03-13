package nora.vm.nodes.equality;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.strings.TruffleString;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.nodes.NoraNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

@NodeInfo(shortName = "==")
public class EqNode extends NoraNode {
    public final static int MAX_RECURSION_DEPTH = 4;
    @Child private NoraNode left;
    @Child private NoraNode right;
    //MAX_RECURSION_DEPTH ensures that we always start with a call lets truffle decide to do inline or not
    @Child private EqPlainNode eq = EqPlainNodeGen.create(MAX_RECURSION_DEPTH);

    public EqNode(NoraNode left, NoraNode right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean executeBoolean(VirtualFrame virtualFrame) throws UnexpectedResultException {
        return eq.execute(left.execute(virtualFrame), right.execute(virtualFrame));
    }

    @Override
    public Object execute(VirtualFrame virtualFrame) {
        try {
            return executeBoolean(virtualFrame);
        } catch (UnexpectedResultException e) {
            throw new RuntimeException("Type Missmatch");
        }
    }


    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        var leftSpec = left.specialise(frame);
        var rightSpec = right.specialise(frame);
        if(leftSpec instanceof ConstNode && rightSpec instanceof ConstNode){
            var leftRes = leftSpec.execute(null);
            var rightRes = leftSpec.execute(null);
            return ConstNode.create(eq.execute(leftRes,rightRes));
        } else if(leftSpec instanceof CachedNode cn1 && rightSpec instanceof CachedNode cn2) {
            //will be trivial after first execution
            // we rely on truffle not optimizing before executing at least once
            return new CacheNode(new EqNode(cn1.liftCache(), cn2.liftCache()));
        }
        var util = NoraVmContext.getTypeUtil(null);
        var type = leftSpec.getType(frame).info;
        if(!util.isSimpleType(type)){
            //Sadly we do not know how expensive this will be
            //Todo: we could ask schema
            frame.markNonTrivial();
        }
        return new EqNode(leftSpec, rightSpec);
    }

    @Override
    public String toString() {
        return "Eq(" +left+"," +right+")";
    }

    @Override
    public EqNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return new EqNode(left.cloneUninitialized(),right.cloneUninitialized());
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return NoraVmContext.getTypeUtil(null).BooleanType;
    }

    @Override
    public int complexity() {
        return 1+left.complexity()+right.complexity();
    }
}
