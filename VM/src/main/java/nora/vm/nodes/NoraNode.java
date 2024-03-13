package nora.vm.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import nora.vm.method.Function;
import nora.vm.nodes.consts.ConstRetNode;
import nora.vm.nodes.special.DelayedErrorNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.runtime.data.NoraData;
import nora.vm.runtime.data.RuntimeData;
import nora.vm.specTime.SpecFrame;
import nora.vm.truffle.NoraLanguage;
import nora.vm.truffle.NoraTypes;
import nora.vm.truffle.NoraTypesGen;
import nora.vm.types.Type;
import com.oracle.truffle.api.strings.TruffleString;
import nora.vm.types.TypeInfo;

import java.math.BigInteger;

@NodeInfo(language = "Nora Language", description = "The abstract base node for all expressions")
@TypeSystemReference(NoraTypes.class)
public abstract class NoraNode extends Node {

    public abstract Object execute(VirtualFrame virtualFrame);

    public Object executeObject(VirtualFrame virtualFrame) throws UnexpectedResultException {
        return this.execute(virtualFrame);
    }

    public boolean executeBoolean(VirtualFrame virtualFrame) throws UnexpectedResultException {
        return NoraTypesGen.expectBoolean(this.execute(virtualFrame));
    }

    public byte executeByte(VirtualFrame virtualFrame) throws UnexpectedResultException {
        return NoraTypesGen.expectByte(this.execute(virtualFrame));
    }
    public int executeInt(VirtualFrame virtualFrame) throws UnexpectedResultException {
        return NoraTypesGen.expectInteger(this.execute(virtualFrame));
    }

    public long executeLong(VirtualFrame virtualFrame) throws UnexpectedResultException {
        return NoraTypesGen.expectLong(this.execute(virtualFrame));
    }

    public BigInteger executeBigInteger(VirtualFrame virtualFrame) throws UnexpectedResultException {
        return NoraTypesGen.expectBigInteger(this.execute(virtualFrame));
    }

    public TruffleString executeString(VirtualFrame virtualFrame) throws UnexpectedResultException {
        return NoraTypesGen.expectTruffleString(this.executeObject(virtualFrame));
    }

    public NoraData executeNoraData(VirtualFrame virtualFrame) throws UnexpectedResultException {
        return NoraTypesGen.expectNoraData(this.executeObject(virtualFrame));
    }

    public Function executeFunction(VirtualFrame virtualFrame) throws UnexpectedResultException {
        return NoraTypesGen.expectFunction(this.executeObject(virtualFrame));
    }

    @TruffleBoundary
    public abstract NoraNode specialise(SpecFrame frame) throws Exception;

    public NoraNode safeSpecialise(SpecFrame frame) throws Exception {
        try {
            return specialise(frame);
        } catch (EnsureOrRetNode.EarlyReturnException ret){
            return new ConstRetNode(ret);
        } catch (RuntimeException specExp){
            return new DelayedErrorNode(specExp);
        }
    }

    public abstract NoraNode cloneUninitialized();

    @TruffleBoundary
    public Type getType(SpecFrame frame){
        CompilerAsserts.neverPartOfCompilation();
        System.out.println("Ups some typing missing in "+getClass());
        return null;
    }

    public boolean isUnboxed(){
        return true;
    }

    protected NoraLanguage getLanguage(){
        return NoraLanguage.get(this);
    }

    protected NoraVmContext getContext(){
        return NoraVmContext.get(this);
    }

    public int complexity() {
        var count = 1;
        for(Node n: getChildren()){
            //if not complexity needs overwriting
            assert n instanceof NoraNode;
            count += ((NoraNode)n).complexity();
        }
        return count;
    }

}
