package nora.vm.runtime.data;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
import nora.vm.nodes.property.PropertyManager;
import nora.vm.truffle.NoraLanguage;
import nora.vm.types.Type;
import nora.vm.types.TypeInfo;
import nora.vm.types.schemas.DataSchemaHandler;

import java.util.Arrays;

@ExportLibrary(InteropLibrary.class)
public class RuntimeData extends NoraData {
    public final int typeIndex;
    public final int dispatcherIndex;

    public RuntimeData(DataSchemaHandler handler, int dispatcherIndex) {
        super(handler);
        this.typeIndex = handler.type.info.id();
        this.dispatcherIndex = dispatcherIndex;
    }

    @Override
    public Type getType() {
        //Can we do the Cast Unsave
        return getHandler().type;
    }

    @Override
    public TypeInfo getTypeInfo() {
        return getType().info;
    }

    public final DataSchemaHandler getHandler(){
        return (DataSchemaHandler)handler;
    }

    @ExportMessage
    public boolean hasMembers(){
        return true;
    }

    @ExportMessage
    public String[] getMembers(boolean includeInternal){
        return Arrays.stream(handler.getAllProperties()).map(PropertyManager::getName).toArray(String[]::new);
    }

    @ExportMessage
    public boolean isMemberReadable(String member){
        return getHandler().getMemberIndex().containsKey(member);
    }

    @ExportMessage
    public Object readMember(String member) throws UnknownIdentifierException {
        var prop = getHandler().getMemberIndex().get(member);
        if(prop == null) throw UnknownIdentifierException.create(member);
        return prop.getGetter().executeGenericGet(this);
    }

    @ExportMessage
    public final boolean isMetaObject(){
        return false;
    }

    @ExportMessage
    public final Object getMetaQualifiedName() throws UnsupportedMessageException{
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    final Object getMetaSimpleName() throws UnsupportedMessageException {
        throw UnsupportedMessageException.create();
    }


    @ExportMessage
    public final String toDisplayString(boolean allowSideEffects){
        return toString();
    }

    @ExportMessage
    public boolean hasMetaObject(){
        return true;
    }

    @ExportMessage
    public DataSchemaHandler getMetaObject(){
        return getHandler();
    }


    @ExportMessage
    public final boolean isMetaInstance(Object instance){
        return false;
    }

    @ExportMessage
    public final boolean hasLanguage(){
        return true;
    }

    @ExportMessage
    public final Class<? extends TruffleLanguage<?>> getLanguage(){
        return NoraLanguage.class;
    }
}
