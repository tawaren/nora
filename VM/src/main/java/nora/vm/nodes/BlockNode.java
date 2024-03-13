package nora.vm.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

//Todo: use the blocknode api implements BlockNode.ElementExecutor<LanguageNode>
public class BlockNode extends ForwardNode {
    @Children private final StmNode[] stms;
    @Child private NoraNode expr;

    public BlockNode(StmNode[] stms, NoraNode expr) {
        this.stms = stms;
        this.expr = expr;
    }

    @ExplodeLoop
    private void executeStms(VirtualFrame frame) {
        CompilerAsserts.partialEvaluationConstant(stms.length);
        for(StmNode n:stms) n.executeVoid(frame);
    }

    @Override
    public Object execute(VirtualFrame virtualFrame) {
        executeStms(virtualFrame);
        return expr.execute(virtualFrame);
    }

    @Override
    public boolean executeBoolean(VirtualFrame virtualFrame) throws UnexpectedResultException {
        executeStms(virtualFrame);
        return expr.executeBoolean(virtualFrame);
    }

    @Override
    public byte executeByte(VirtualFrame virtualFrame) throws UnexpectedResultException {
        executeStms(virtualFrame);
        return expr.executeByte(virtualFrame);
    }

    @Override
    public int executeInt(VirtualFrame virtualFrame) throws UnexpectedResultException {
        executeStms(virtualFrame);
        return expr.executeInt(virtualFrame);
    }

    @Override
    public long executeLong(VirtualFrame virtualFrame) throws UnexpectedResultException {
        executeStms(virtualFrame);
        return expr.executeLong(virtualFrame);
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        StmNode[] newStms = new StmNode[stms.length];
        var kept = 0;
        for (StmNode stm : stms) {
            var newStm = stm.specialise(frame);
            if (newStm != null) newStms[kept++] = newStm;
        }

        var newExpr = expr.specialise(frame);
        var compStm = newStms;
        if(kept == 0){
            return newExpr;
        } else if(kept != newStms.length){
            compStm = new StmNode[kept];
            System.arraycopy(newStms,0,compStm,0, kept);
        }
        if(newExpr instanceof ConstNode) return newExpr;
        if(newExpr instanceof CachedNode cn1){
            //This could be done more elegantly over an interface instead of asking LetNode explicitly
            for(int i = 0; i < compStm.length; i++){
                if(compStm[i] instanceof LetNode ln && ln.getExpr() instanceof CachedNode cn){
                    compStm[i] = LetNodeGen.create(ln.slot, cn.liftCache());
                }
            }
            return new CacheNode(new BlockNode(compStm, cn1.liftCache()));
        } else {
            return new BlockNode(compStm, newExpr);
        }
    }

    @Override
    public String toString() {
        var sts = Stream.concat(Arrays.stream(stms),Stream.of(expr)).map(Objects::toString).toList();
        return "{"+String.join(", ", sts)+"}";
    }

    @Override
    public BlockNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        var nStms = new StmNode[stms.length];
        for(int i = 0;i < nStms.length; i++) nStms[i] = stms[i].cloneUninitialized();
        return new BlockNode(nStms, expr.cloneUninitialized());
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return frame.withTypeSnapshot(f -> {
            for (NoraNode stm : stms) {
                stm.getType(frame);
            }
            return expr.getType(frame);
        });
    }

    @Override
    public boolean isUnboxed() {
        return expr.isUnboxed();
    }
}
