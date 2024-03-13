package nora

import nora.typesys.DataType
import nora.typesys.Type
import nora.typesys.TypeParam
import nora.typesys.Variance
import java.nio.file.Path

data class PrimInfo(val short:String, val symbol:String?, val full:String, val numArgs:Int)

val primInfoByName = mapOf(
    Pair("add", PrimInfo("add","+", "nora.lang.Arith::add",2)),
    Pair("sub", PrimInfo("sub", "-","nora.lang.Arith::sub",2)),
    Pair("mul", PrimInfo("mul", "*","nora.lang.Arith::mul",2)),
    Pair("div", PrimInfo("div", "/","nora.lang.Arith::div",2)),
    Pair("mod", PrimInfo("mod", "%","nora.lang.Arith::mod",2)),
    Pair("and", PrimInfo("and", "&","nora.lang.Bitwise::and",2)),
    Pair("or", PrimInfo("or","|","nora.lang.Bitwise::or", 2)),
    Pair("xor", PrimInfo("xor","^","nora.lang.Bitwise::xor",2)),
    Pair("not", PrimInfo("not", "!","nora.lang.Bitwise::not", 1)),
    Pair("inv", PrimInfo("inv", "~","nora.lang.Bitwise::inv", 1)),
    Pair("lsh",PrimInfo("lsh","<<","nora.lang.Bitwise::lsh", 2)),
    Pair("rsh",PrimInfo("rsh",">>","nora.lang.Bitwise::rsh", 2)),
    Pair("eq", PrimInfo("eq","==","nora.lang.Compare::eq",2)),
    Pair("cmp", PrimInfo("cmp", "<>","nora.lang.Compare::cmp",2)),
    Pair("lt", PrimInfo("lt", "<","nora.lang.Compare::lt",2)),
    Pair("lte", PrimInfo("lte","<=","nora.lang.Compare::lte",2)),
    Pair("gt", PrimInfo("gt",">","nora.lang.Compare::gt",2)),
    Pair("gte", PrimInfo("gte",">=","nora.lang.Compare::gte",2)),
    Pair("substring", PrimInfo("substring", null, "nora.lang.StringUtils::substring",3)),
    Pair("string_length", PrimInfo("string_length", null,"nora.lang.StringUtils::length",1)),
    Pair("string_concat", PrimInfo("string_concat",null,"nora.lang.StringUtils::concat",2)),
    Pair("to_string", PrimInfo("to_string",null,"nora.lang.StringUtils::toString",1)),
    Pair("hash_code", PrimInfo("hash_code", null, "nora.lang.Hash::hashCode",1))
)

val primInfoBySymbol = primInfoByName.values.filter { it.symbol != null }.associateBy { it.symbol }

private val noSrc = Src(Path.of(""),0,0)


val BoolType = DataType(Elem(Ref("nora.lang.Primitive::Bool"), noSrc), listOf())
val ByteType = DataType(Elem(Ref("nora.lang.Primitive::Byte"), noSrc), listOf())
val IntType = DataType(Elem(Ref("nora.lang.Primitive::Int"), noSrc), listOf())
val NumType = DataType(Elem(Ref("nora.lang.Primitive::Num"), noSrc), listOf())
val StringType = DataType(Elem(Ref("nora.lang.Primitive::String"), noSrc), listOf())

val TupleDef = Ref("nora.lang.Primitive::Tuple")
fun TupleType(vararg args: Type) = DataType(
    Elem(TupleDef, noSrc),
    args.map { TypeParam(Variance.CO,Elem(it, noSrc)) }.toList()
)

val FunDef = Ref("nora.lang.Primitive::Function")
fun FunctionType(ret:Type, vararg args: Type) = DataType(
    Elem(FunDef, noSrc),
    args.map { TypeParam(Variance.CONTRA,Elem(it, noSrc)) }.toList() + TypeParam(Variance.CO,Elem(ret, noSrc))
)