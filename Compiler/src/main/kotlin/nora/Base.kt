package nora

import nora.typesys.Type
import nora.typesys.Variance
import java.nio.file.Path

data class Src(val file: Path, val start:Int, val end:Int)
data class Elem<out T>(val data:T, val src: Src) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Elem<*>
        return data == other.data
    }

    override fun hashCode(): Int {
        return data?.hashCode() ?: 0
    }
}


enum class SlotKind {
    LOCAL,
    ARG,
    CAPTURE
}

sealed interface Value
data class TypeValue(val typ: Type):Value
data class SlotValue(val kind:SlotKind, val slot: Int):Value
data class CaptureValue(val captures: List<Pair<SlotValue, Type>>):Value
data class IntValue(val value: Int):Value



//Todo: Enforce Sealed in structural (similar as private but only for extends)
enum class Modifier{
    PUBLIC, PRIVATE, SEALED
}
enum class Extra{
    TYPE_VAR, //Temporary Type Var used during inferece
    TYPE, //Final Expression Type After inference
    SLOT,
    CAPTURES,
    LOCALS
}

sealed interface AttrValue
//Todo: shall we allow to resolve to type?
data class RefAttribute(val par: ParametricReference):AttrValue
data class IntAttribute(val int: Int):AttrValue
data class BooAttribute(val bool: Boolean):AttrValue
data class StringAttribute(val string: String):AttrValue


data class Attribute(val name: Elem<String>, val value: Elem<AttrValue>)

fun <T,R> Elem<T>.map(f:(T) -> R) = Elem<R>(f(data),src)

sealed interface Reference
sealed interface Binding {val name: String}
//References something fully qualified or imported
data class Ref(override val name: String):Binding, Reference
//References a Generic of the current function
data class Gen(override val name: String, val pos:Int):Binding
//References a enclosing Local(Let) binding
data class Loc(override val name: String, val id:Int):Binding

data class ParametricReference(val main: Elem<Binding>, val parameters:List<Elem<ParametricReference>>) : Reference
data class AppliedReference<out L>(val main: Elem<Binding>, val parameters: Applies<L>) : Reference
data class Hint<out L>(val name:Elem<String>, val value:Elem<L>)

sealed interface Applies<out L>;
data class Provided<out L>(val parameters:List<Elem<L>>):Applies<L>
data class Hinted<out L>(val hints:List<Hint<L>>):Applies<L>
data class Partial<out L>(val parameters:List<Elem<L?>>):Applies<L>



data class ClosureArgument<out L>(val name: Elem<Loc>, val type: Elem<L>?)
data class Argument<out L>(val name: Elem<Loc>, val type: Elem<L>)
data class Generic<out L>(val name: Elem<Gen>, val variance:Elem<Variance>, val bounds:List<Elem<L>>, val fallback:Elem<L>?)

sealed interface Maybe<out T>
data class Some<out T>(val elem:T):Maybe<T>
data object None :Maybe<Nothing>

fun <T> T?.toMaybe():Maybe<T>{
    if(this == null) return None
    return Some(this)
}

fun <T> Maybe<T>.toNullable():T? {
    return when(this){
        None -> null
        is Some -> elem
    }
}

fun <T,V> Maybe<T>.map(f: (T) -> V): Maybe<V> {
    return when(this){
        None -> None
        is Some -> Some(f(elem))
    }
}
