package nora.codegen

import nora.*
import nora.Array
import nora.SlotKind.*
import nora.resolved.ResolvedState
import nora.resolved.getAllFields
import nora.resolved.inferenz.type
import nora.resolved.inferenz.unify
import nora.typesys.DataType
import nora.typesys.Type
import java.nio.file.Files
import java.nio.file.Path

//Todo: Sadly not functional
private fun Definition<ResolvedState>.genCode(buildDir:Path) {
    val res = accept(CodeGen, StringBuilder())
    if(res != null){
        val paths = name.data.split("\\.|::")
        val fileName = paths.last()+".nora"
        val modulePath = paths.dropLast(1).fold(buildDir){ p, e -> p.resolve(e)}
        Files.createDirectories(modulePath)
        val filePath = modulePath.resolve(fileName)
        Files.writeString(filePath, res)
    }
}

//Just for Dispatching
object CodeGen : DefinitionVisitor<ResolvedState, StringBuilder?, StringBuilder> {
    override fun visitDefinition(definition: Definition<ResolvedState>, ctx: StringBuilder) = null
    override fun visitMultiMethod(multiMethod: MultiMethod<ResolvedState>, ctx: StringBuilder) = multiMethod.genCode(ctx)
    override fun visitData(data: Data<ResolvedState>, ctx: StringBuilder) = data.genCode(ctx)
    override fun visitTuple(tuple: Tuple<ResolvedState>, ctx: StringBuilder) = tuple.genCode(ctx)
    override fun visitObjectMethod(objectMethod: ObjectMethod<ResolvedState>, ctx: StringBuilder) = objectMethod.genCode(ctx)
}

fun MultiMethod<ResolvedState>.genCode(builder:StringBuilder): StringBuilder {
    val dynArgs = dynamicArgs.joinToString { (it.data.type.data as Type).genTypePattern()}
    val statArgs = staticArgs.joinToString { (it.data.type.data as Type).genTypePattern()}
    val afterSig = builder.append(
        "<sig>: (", dynArgs, ") | (", statArgs ,") -> ", (ret.data as Type).genTypePattern() , "\n"
    )
    val methods = definitionMembers(name.data).map { boundedDefinition(it.name).toNullable()!! as CaseMethod }
    return methods.fold(afterSig.append("<methods>: \n")){b, m ->
        b.append(m.name.data)
            //Todo: can be left ot under some circumstances -  ut is just code size optim
            .append("[",m.generics.size, "|",
                m.parentApplies.joinToString { (it.data as Type).genTypePattern() } ,"]")
            .append("(", m.args.joinToString { (it.data.type.data as Type).genTypePattern() } ,") -> ",
                (m.ret.data as Type).genTypePattern())
            //Todo: Case filter if we add them as annotations
            .append("\n")
    }
}

fun Data<ResolvedState>.genCode(builder:StringBuilder): StringBuilder {
    //Note: String Builder is mutable, however we treat it as immutable to make potential nora transition easier
    val afterName = builder.append("<name>: ", name.data, "\n")
    //Todo: Handler if we add them as annotations
    val afterParent = if (parent != null) afterName.append("<super>: ", parent.data.name, "\n") else afterName
    val afterKind = afterParent.append("<kind>: ", if(isConcrete) "<concrete>" else "<abstract>", "\n")
    val afterInfo = afterKind.append("<info>: ",resolveTypeId(name.data).toString(),"\n")
    val gens = generics.joinToString { it.data.variance.data.shortString() }
    val afterGeneric = afterInfo.append("<generics>: ", gens)
    return getAllFields(true).fold(afterGeneric.append("<fields>:\n")) { b, f ->
        b.append(f.data.name.data.name, ":", (f.data.type.data as Type).genTypePattern(), "\n")
    }
}

fun Tuple<ResolvedState>.genCode(builder:StringBuilder): StringBuilder {
    //Note: String Builder is mutable, however we treat it as immutable to make potential nora transition easier
    val afterName = builder.append("<name>: ", name.data, "\n")
    val afterHandler = afterName.append("<handler>: nora_core#tuple_handler()\n")
    val afterKind = afterHandler.append("<kind>: <concrete>\n")
    //Todo: get the -9 dynamically from some where (library annotation?)
    val afterInfo = afterKind.append("<info>: {-9,0,0}\n")
    val afterGeneric = afterInfo.append("<generics>:\n")
    return afterGeneric.append("<fields>:\n")
}

fun Implemented<ResolvedState>.genCode(builder:StringBuilder): StringBuilder {
    val locals = (extra[Extra.LOCALS] as IntValue).value
    val afterLocals = builder.append("<locals>: ", locals, "\n")
    //Todo: Figure out tail recursion
    //      Either in Parsing or extend Name rebinding
    //      Even better would be to figure it VM transparently (in Parsing?)
    val argTypes = args.joinToString { (it.data.type.data as Type).genTypePattern()}
    val afterSig = afterLocals.append("<sig>: (", argTypes, ") -> ", (ret.data as Type).genTypePattern() , "\n")
    return body.data.genCode(afterSig)
}

fun ObjectMethod<ResolvedState>.genCode(builder:StringBuilder): StringBuilder {
    TODO("Not yet supported")
}

fun Expr<ResolvedState>.genCode(builder:StringBuilder): StringBuilder {
    return accept(ExprCodeGen, builder)
}

//Just for Dispatching
object ExprCodeGen: ExprVisitor<ResolvedState,  StringBuilder, StringBuilder> {
    override fun visitInstanceOfExpr(expr: InstanceOfExpr<ResolvedState>, ctx: StringBuilder) = expr.genCode(ctx)
    override fun visitTypeHintExpr(expr: TypeHintExpr<ResolvedState>, ctx: StringBuilder) = expr.genCode(ctx)
    override fun visitCallExpr(expr: CallExpr<ResolvedState>, ctx: StringBuilder) = expr.genCode(ctx)
    override fun visitPrimitiveExpr(expr: PrimitiveExpr<ResolvedState>, ctx: StringBuilder) = expr.genCode(ctx)
    override fun visitCreateExpr(expr: CreateExpr<ResolvedState>, ctx: StringBuilder) = expr.genCode(ctx)
    override fun visitLitExpr(expr: LitExpr<ResolvedState>, ctx: StringBuilder) = expr.genCode(ctx)
    override fun visitValExpr(expr: ValExpr<ResolvedState>, ctx: StringBuilder) = expr.genCode(ctx)
    override fun visitFunLinkExpr(expr: FunLinkExpr<ResolvedState>, ctx: StringBuilder) = expr.genCode(ctx)
    override fun visitTypeLinkExpr(expr: TypeLinkExpr<ResolvedState>, ctx: StringBuilder) = expr.genCode(ctx)
    override fun visitFieldExpr(expr: FieldExpr<ResolvedState>, ctx: StringBuilder) = expr.genCode(ctx)
    override fun visitFunctionExpr(expr: FunctionExpr<ResolvedState>, ctx: StringBuilder) = expr.genCode(ctx)
    override fun visitIfExpr(expr: IfExpr<ResolvedState>, ctx: StringBuilder) = expr.genCode(ctx)
    override fun visitLetExpr(expr: LetExpr<ResolvedState>, ctx: StringBuilder) = expr.genCode(ctx)
}

fun InstanceOfExpr<ResolvedState>.genCode(builder: StringBuilder): StringBuilder{
    val afterSource = source.data.genCode(builder.append("<subTypeOf>(")).append(",")
    return type.data.genCode(afterSource).append(")")
}

fun TypeHintExpr<ResolvedState>.genCode(builder: StringBuilder): StringBuilder{
    return target.data.genCode(builder)
}

fun CallExpr<ResolvedState>.genCode(builder: StringBuilder): StringBuilder{
    //Todo: Handle Tail create if applicable
    //      Even Better Figure out during spec
    val afterTarget = target.data.genCode(builder.append("<call>")).append("(")
    val argString = args.joinToString { it.data.genCode(StringBuilder()) }
    return afterTarget.append(argString,")")
}

fun PrimitiveExpr<ResolvedState>.genCode(builder: StringBuilder): StringBuilder{
    val primInfo = primInfoByName[target.data]!!
    val args = (1..primInfo.numArgs).joinToString { "!$it" }
    return builder.append("<primitive>#", primInfo.short, "(", args, ")")
}

fun CreateExpr<ResolvedState>.genCode(builder: StringBuilder): StringBuilder{
    //Todo: Handle Tail create if applicable
    //      Even Better Figure out during spec
    val genArgs = (target.data.parameters as Provided).parameters.joinToString { (it.data as Type).genTypePattern() }
    val args = args.joinToString { it.data.genCode(StringBuilder()) }
    return builder.append("<create>#",target.data.main.data.name,"[",genArgs,"](",args,")")
}

fun LitExpr<ResolvedState>.genCode(builder: StringBuilder): StringBuilder{
    return when(type()){
        ByteType -> builder.append("<byte> ", value.data)
        IntType -> builder.append("<int> ", value.data)
        NumType -> builder.append("<num> ", value.data)
        StringType -> builder.append("\"", value.data, "\"")
        BoolType -> if(value.data as Boolean){
            builder.append("<true>")
        } else {
            builder.append("<false>")
        }
        //Should not happen
        else -> builder.append(value.data)
    }
}

fun SlotValue.genAccessCode():String {
    return when(kind){
        LOCAL -> "\$$slot"
        ARG -> "!${slot+1}"
        CAPTURE -> "#$slot"
    }
}

fun ValExpr<ResolvedState>.genCode(builder: StringBuilder): StringBuilder{
    val slot = extra[Extra.SLOT] as SlotValue
    return builder.append(slot.genAccessCode())
}

fun FunLinkExpr<ResolvedState>.genCode(builder: StringBuilder): StringBuilder{
    return when(resolvedDefinition(source.data.main.data.name).toNullable()){
        is MultiMethod<ResolvedState> -> {
            val genArgs = (source.data.parameters as Provided).parameters.joinToString { (it.data as Type).genTypePattern() }
            builder.append("<multi_method>#", source.data.main.data.name, "[",genArgs ,"]")
        }
        is Method<ResolvedState> -> {
            val genArgs = (source.data.parameters as Provided).parameters.joinToString { (it.data as Type).genTypePattern() }
            builder.append("<method>#", source.data.main.data.name, "[",genArgs ,"]")
        }
        is ObjectMethod -> TODO("Not jet implemented - but ptobably not possible")
        else -> builder //Should not happen
    }
}

fun TypeLinkExpr<ResolvedState>.genCode(builder: StringBuilder): StringBuilder{
    val genArgs = (source.data.parameters as Provided).parameters.joinToString { (it.data as Type).genTypePattern() }
    val typeId = resolveTypeId(source.data.main.data.name)
    return if (genArgs.isEmpty()) {
        builder.append(typeId)
    } else {
        builder.append(typeId, "[", genArgs, "]")
    }
}

fun FieldExpr<ResolvedState>.genCode(builder: StringBuilder): StringBuilder{
    val srcT = type() as DataType
    val srcName = srcT.base.data.name
    val genArgs = srcT.parameters.joinToString { it.param.data.genTypePattern() }
    val field = when(val def =resolvedDefinition(srcName).toNullable()){
        is Tuple<ResolvedState> -> name.data.substring(1).toInt()
        is Data<ResolvedState> -> def.getAllFields().indexOfFirst { it.data.name.data.name == name.data }
        else -> 0 //Should never happen
    }
    val afterGens = builder.append("<field>#", srcName, "[",genArgs ,"](")
    return target.data.genCode(afterGens).append(",",field,")")
}



fun FunctionExpr<ResolvedState>.genCode(builder: StringBuilder): StringBuilder{
    val captures = (extra[Extra.CAPTURES] as CaptureValue).captures
    val captTypes = captures.joinToString { it.second.genTypePattern() }
    val captureExprs = captures.joinToString { it.first.genAccessCode() }
    val numLocals = (extra[Extra.LOCALS] as IntValue).value
    val afterCaptures = builder.append("<closure>", "[", captTypes ,"](", captureExprs ,")")
    val afterType = afterCaptures.append("[", type().genTypePattern(), "]{", numLocals, "} ")
    return body.data.genCode(afterType)
}

fun IfExpr<ResolvedState>.genCode(builder: StringBuilder): StringBuilder{
    val afterType = builder.append("<if>[",type().genTypePattern(),"] ")
    val afterCond = cond.data.genCode(afterType).append("<then> ")
    val afterThen = then.data.genCode(afterCond).append("<else> ")
    return other.data.genCode(afterThen)
}

fun LetExpr<ResolvedState>.genCode(builder: StringBuilder): StringBuilder{
    val slot = (extra[Extra.SLOT] as SlotValue).slot
    val afterBind = builder.append("<let> $", slot, " = ")
    return body.data.genCode(afterBind)
}