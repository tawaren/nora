package nora.vm.nodes.cache;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import nora.vm.nodes.EnsureOrRetNode;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.TypedRetNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.runtime.ValueCache;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;
import nora.vm.types.schemas.DataSchemaHandler;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate;

//Todo: what if this is to big and should not be cached because of GarbageCollection?
//      add a no cache annotation to method
public class CacheNode extends CachedNode {
    @Child private NoraNode comp;

    public CacheNode(NoraNode comp) {
        this.comp = comp;
    }

    @Override
    public boolean executeBoolean(VirtualFrame virtualFrame) throws UnexpectedResultException {
        try {
            var res = comp.executeBoolean(virtualFrame);
            replace(ConstNode.create(res));
            return res;
        } catch (UnexpectedResultException e) {
            replace(ConstNode.create(e.getResult()));
            throw e;
        } catch (EnsureOrRetNode.EarlyReturnException retExp){
            //Note: type should no longer be needed, should be save
            replace(new TypedRetNode(ConstNode.create(retExp.getValue()), null));
            throw retExp;
        }
    }

    @Override
    public byte executeByte(VirtualFrame virtualFrame) throws UnexpectedResultException {
        try {
            var res = comp.executeByte(virtualFrame);
            replace(ConstNode.create(res));
            return res;
        } catch (UnexpectedResultException e) {
            replace(ConstNode.create(e.getResult()));
            throw e;
        } catch (EnsureOrRetNode.EarlyReturnException retExp){
            //Note: type should no longer be needed, should be save
            replace(new TypedRetNode(ConstNode.create(retExp.getValue()), null));
            throw retExp;
        }
    }


    @Override
    public int executeInt(VirtualFrame virtualFrame) throws UnexpectedResultException {
        try {
            var res = comp.executeInt(virtualFrame);
            replace(ConstNode.create(res));
            return res;
        } catch (UnexpectedResultException e) {
            replace(ConstNode.create(e.getResult()));
            throw e;
        } catch (EnsureOrRetNode.EarlyReturnException retExp){
            //Note: type should no longer be needed, should be save
            replace(new TypedRetNode(ConstNode.create(retExp.getValue()), null));
            throw retExp;
        }
    }

    @Override
    public long executeLong(VirtualFrame virtualFrame) throws UnexpectedResultException {
        try {
            var res = comp.executeLong(virtualFrame);
            replace(ConstNode.create(res));
            return res;
        } catch (UnexpectedResultException e) {
            replace(ConstNode.create(e.getResult()));
            throw e;
        } catch (EnsureOrRetNode.EarlyReturnException retExp){
            //Note: type should no longer be needed, should be save
            replace(new TypedRetNode(ConstNode.create(retExp.getValue()), null));
            throw retExp;
        }
    }

    @Override
    public Object execute(VirtualFrame virtualFrame) {
        //Todo: Handle EarlyReturnException - needs special managed cache
        try {
            var res = comp.execute(virtualFrame);
            transferToInterpreterAndInvalidate();
            replace(NoraVmContext.getValueCache(null).createCached(comp, res));
            return res;
        } catch (EnsureOrRetNode.EarlyReturnException retExp){
            transferToInterpreterAndInvalidate();
            replace(NoraVmContext.getValueCache(null).createEarlyRetCached(this, retExp));
            throw retExp;
        }
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        var res = comp.specialise(frame);
        if(comp instanceof CachedNode cn) return cn.liftCache();
        return new CacheNode(res);
    }

    @Override
    public NoraNode liftCache() {
        return comp;
    }

    @Override
    public NoraNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return new CacheNode(comp);
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return comp.getType(frame);
    }
}
