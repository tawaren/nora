package nora.vm.nodes.hash_code;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.strings.TruffleString;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.nodes.equality.EqPlainNode;
import nora.vm.nodes.equality.EqPlainNodeGen;
import nora.vm.nodes.hash_code.data.DataHashCodeNode;
import nora.vm.nodes.hash_code.data.DataHashCodeRecursionNodeGen;
import nora.vm.runtime.NoraVmContext;
import nora.vm.runtime.data.IdentityObject;
import nora.vm.runtime.data.NoraData;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

import java.math.BigInteger;

public class HashCodeNode extends NoraNode {
    public final static int MAX_RECURSION_DEPTH = 4;
    @Child private NoraNode val;
    //MAX_RECURSION_DEPTH ensures that we always start with a call lets truffle decide to do inline or not
    @Child private HashCodePlainNode hash = HashCodePlainNodeGen.create(MAX_RECURSION_DEPTH);

    public HashCodeNode(NoraNode val) {
        this.val = val;
    }

    @Override
    public int executeInt(VirtualFrame virtualFrame) throws UnexpectedResultException {
        return hash.execute(val.execute(virtualFrame));
    }

    @Override
    public Object execute(VirtualFrame virtualFrame) {
        try {
            return executeInt(virtualFrame);
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
            return ConstNode.create(hash.execute(lVal));
        } else if(leftRes instanceof CachedNode cn1) {
            return new CacheNode(new HashCodeNode(cn1.liftCache()));
        } else {
            var util = NoraVmContext.getTypeUtil(null);
            if(!util.isSimpleType(leftRes.getType(frame).info)){
                //Sadly we do not know how expensive this will be
                //Todo: we could ask schema
                frame.markNonTrivial();
            }
            return new HashCodeNode(leftRes);
        }
    }

    @Override
    public String toString() {
        return "HashCode(" +val+")";
    }

    @Override
    public HashCodeNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return new HashCodeNode(val.cloneUninitialized());
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return NoraVmContext.getTypeUtil(null).IntType;
    }

    @Override
    public int complexity() {
        return 1+val.complexity();
    }

}
