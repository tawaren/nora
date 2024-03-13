package nora.vm.types.schemas.utils;
import com.oracle.truffle.api.strings.TruffleString;

//todo: could in the future become more elaborate
public class ToStringTemplate {
    public final TruffleString start;
    public final TruffleString seperator;
    public final TruffleString end;

    public ToStringTemplate(TruffleString start, TruffleString seperator, TruffleString end) {
        this.start = start;
        this.seperator = seperator;
        this.end = end;
    }
}
