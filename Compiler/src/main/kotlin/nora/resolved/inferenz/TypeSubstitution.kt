package nora.resolved.inferenz

import nora.*
import nora.resolved.ResolvedState
import nora.typesys.*

fun Expr<ResolvedState>.type():Type = (extra[Extra.TYPE] as TypeValue).typ
fun Elem<Expr<ResolvedState>>.type():Type = data.type()

fun Map<Extra,Value>.withFinalType(t:Type):Map<Extra,Value> = minus(Extra.TYPE_VAR) + Pair(Extra.TYPE, TypeValue(t))


fun Elem<Expr<ResolvedState>>.substituteTypeVars(ctx:TypeResolutionContext, binds:Map<Binding,Type>):Elem<Expr<ResolvedState>> {
    return map{ it.accept(ExprInferenceVisitor, Pair(ctx, binds)) }
}

//Just for Dispatching
object ExprInferenceVisitor: ExprVisitor<ResolvedState, Expr<ResolvedState>, Pair<TypeResolutionContext,Map<Binding,Type>>> {
    override fun visitInstanceOfExpr(expr: InstanceOfExpr<ResolvedState>, ctx:Pair<TypeResolutionContext, Map<Binding,Type>>) = expr.substituteTypeVars(ctx.first, ctx.second)
    override fun visitTypeHintExpr(expr: TypeHintExpr<ResolvedState>, ctx:Pair<TypeResolutionContext, Map<Binding,Type>>) = expr.substituteTypeVars(ctx.first, ctx.second)
    override fun visitCallExpr(expr: CallExpr<ResolvedState>, ctx:Pair<TypeResolutionContext, Map<Binding,Type>>) = expr.substituteTypeVars(ctx.first, ctx.second)
    override fun visitPrimitiveExpr(expr: PrimitiveExpr<ResolvedState>, ctx:Pair<TypeResolutionContext, Map<Binding,Type>>) = expr.substituteTypeVars(ctx.first, ctx.second)
    override fun visitCreateExpr(expr: CreateExpr<ResolvedState>, ctx:Pair<TypeResolutionContext, Map<Binding,Type>>) = expr.substituteTypeVars(ctx.first, ctx.second)
    override fun visitLitExpr(expr: LitExpr<ResolvedState>, ctx:Pair<TypeResolutionContext, Map<Binding,Type>>) = expr.substituteTypeVars(ctx.first, ctx.second)
    override fun visitValExpr(expr: ValExpr<ResolvedState>, ctx:Pair<TypeResolutionContext, Map<Binding,Type>>) = expr.substituteTypeVars(ctx.first, ctx.second)
    override fun visitFunLinkExpr(expr: FunLinkExpr<ResolvedState>, ctx:Pair<TypeResolutionContext, Map<Binding,Type>>) = expr.substituteTypeVars(ctx.first, ctx.second)
    override fun visitTypeLinkExpr(expr: TypeLinkExpr<ResolvedState>, ctx:Pair<TypeResolutionContext, Map<Binding,Type>>) = expr.substituteTypeVars(ctx.first, ctx.second)
    override fun visitFieldExpr(expr: FieldExpr<ResolvedState>, ctx:Pair<TypeResolutionContext, Map<Binding,Type>>) = expr.substituteTypeVars(ctx.first, ctx.second)
    override fun visitFunctionExpr(expr: FunctionExpr<ResolvedState>, ctx:Pair<TypeResolutionContext, Map<Binding,Type>>) = expr.substituteTypeVars(ctx.first, ctx.second)
    override fun visitIfExpr(expr: IfExpr<ResolvedState>, ctx:Pair<TypeResolutionContext, Map<Binding,Type>>) = expr.substituteTypeVars(ctx.first, ctx.second)
    override fun visitLetExpr(expr: LetExpr<ResolvedState>, ctx:Pair<TypeResolutionContext, Map<Binding,Type>>) = expr.substituteTypeVars(ctx.first, ctx.second)
}

fun InstanceOfExpr<ResolvedState>.substituteTypeVars(ctx: TypeResolutionContext, binds:Map<Binding,Type>):Expr<ResolvedState> {
    val nTarget = source.substituteTypeVars(ctx, binds)
    val nHint = type.substituteTypeVars(ctx, binds)
    return InstanceOfExpr(
        extra.withFinalType(BoolType),
        nTarget,
        nHint
    )
}


fun TypeHintExpr<ResolvedState>.substituteTypeVars(ctx: TypeResolutionContext, binds:Map<Binding,Type>):Expr<ResolvedState> {
    val nTarget = target.substituteTypeVars(ctx, binds)
    val nHint = type.substituteTypeVars(ctx, binds)
    return TypeHintExpr(
        extra.withFinalType(nHint.type()),
        nTarget,
        nHint
    )
}

fun CallExpr<ResolvedState>.substituteTypeVars(ctx: TypeResolutionContext, binds:Map<Binding,Type>):Expr<ResolvedState> {
    val nTarget = target.substituteTypeVars(ctx,binds)
    val nArgs = args.map { it.substituteTypeVars(ctx, binds) }
    val retType = when(val typ = nTarget.type()){
        is DataType -> if(typ.parameters.isEmpty()) UnknownType else typ.parameters.last().param.data
        else -> UnknownType
    }
    return CallExpr(
        extra.withFinalType(retType),
        nTarget,
        nArgs
    )
}


fun PrimitiveExpr<ResolvedState>.substituteTypeVars(ctx: TypeResolutionContext, binds:Map<Binding,Type>):Expr<ResolvedState> {
    val tVar = (extra[Extra.TYPE_VAR] as TypeValue).typ
    return PrimitiveExpr(
        extra.withFinalType(ctx.resolveType(tVar)),
        target
    )
}


fun AppliedReference<ResolvedState>.inferenceTypeReference(ctx: TypeResolutionContext):Pair<AppliedReference<ResolvedState>, Type>{
    val params = (parameters as Provided<ResolvedState>).parameters

    fun buildDataType(genDefs:List<Elem<Generic<ResolvedState>>>):Pair<AppliedReference<ResolvedState>, Type> {
        val args = genDefs.zip(params).map { it.second.map { a -> coerceToSatisfy(ctx.resolveType(a as Type),it.first.data) }}
        val tArgs = genDefs.zip(args).map { TypeParam(it.first.data.variance.data, it.second) }
        return Pair(AppliedReference(main, Provided(args)), DataType(main.map { it as Ref }, tArgs))
    }

    return when(val def = resolvedDefinition(main.data.name).toNullable()){
        is Callable<ResolvedState> -> {
            val args = def.generics.zip(params).map { it.second.map { a -> coerceToSatisfy(ctx.resolveType(a as Type),it.first.data) }}
            val subst = args.map { it.data }.toTypedArray()
            val retT = (def.ret.data as Type).substitute(subst)
            val argTs = def.args.map { (it.data.type.data as Type).substitute(subst) }
            Pair(AppliedReference<ResolvedState>(main, Provided(args)), FunctionType(retT, *argTs.toTypedArray()))
        }
        is VarArgParameterized<ResolvedState> -> buildDataType(def.genGenerics(params.size))
        is Parameterized<ResolvedState> -> buildDataType(def.generics)
        //should never happen as it should be checked by previous passes
        else -> buildDataType(listOf())
    }

}

fun CreateExpr<ResolvedState>.substituteTypeVars(ctx: TypeResolutionContext, binds:Map<Binding,Type>):Expr<ResolvedState> {
    val (nArgRefs, nTarget) = target.data.inferenceTypeReference(ctx)
    val nArgs = args.map{ it.substituteTypeVars(ctx, binds) }
    return CreateExpr(
        extra.withFinalType(nTarget),
        Elem(nArgRefs,target.src),
        nArgs
    )
}

fun LitExpr<ResolvedState>.substituteTypeVars(ctx: TypeResolutionContext, binds:Map<Binding,Type>):Expr<ResolvedState> {
    val litT = (extra[Extra.TYPE_VAR] as TypeValue).typ
    return LitExpr(
        extra.withFinalType(ctx.resolveType(litT)),
        value
    )
}

fun ValExpr<ResolvedState>.substituteTypeVars(ctx: TypeResolutionContext, binds:Map<Binding,Type>):Expr<ResolvedState> {
    return ValExpr(
        extra.withFinalType(binds[source.data]!!),
        source
    )
}

fun FunLinkExpr<ResolvedState>.substituteTypeVars(ctx: TypeResolutionContext, binds:Map<Binding,Type>):Expr<ResolvedState> {
    val (nArgRefs, nTarget) = source.data.inferenceTypeReference(ctx)
    return FunLinkExpr(
        extra.withFinalType(nTarget),
        Elem(nArgRefs,source.src)
    )
}

fun TypeLinkExpr<ResolvedState>.substituteTypeVars(ctx: TypeResolutionContext, binds:Map<Binding,Type>):Expr<ResolvedState> {
    val (nArgRefs, nTarget) = source.data.inferenceTypeReference(ctx)
    return TypeLinkExpr(
        extra.withFinalType(nTarget),
        Elem(nArgRefs,source.src)
    )
}

fun FieldExpr<ResolvedState>.substituteTypeVars(ctx: TypeResolutionContext, binds:Map<Binding,Type>):Expr<ResolvedState> {
    val nTarget = target.substituteTypeVars(ctx, binds)
    val fieldT = (extra[Extra.TYPE_VAR] as TypeValue).typ
    return FieldExpr(
        extra.withFinalType(ctx.resolveType(fieldT)),
        nTarget,
        name
    )
}

fun FunctionExpr<ResolvedState>.substituteTypeVars(ctx: TypeResolutionContext, binds:Map<Binding,Type>):Expr<ResolvedState> {
    val nArgs = args.map { it.map { ca -> ca.copy(type = ca.type?.map { t -> ctx.resolveType(t as Type) }) } }
    val argTs = nArgs.map { it.data.type!!.data as Type }
    val nBinds = argTs.zip(args).fold(binds) {b, (t,a) -> b + Pair(a.data.name.data, t)}
    val nBody = body.substituteTypeVars(ctx, nBinds)
    val funT = FunctionType(nBody.type(),*argTs.toTypedArray())
    return FunctionExpr(
        extra.withFinalType(funT),
        nArgs,
        nBody
    )
}


fun IfExpr<ResolvedState>.substituteTypeVars(ctx: TypeResolutionContext, binds:Map<Binding,Type>):Expr<ResolvedState> {
    //Flow typing opportunity detection
    val condExp = cond.substituteTypeVars(ctx, binds)
    val nBinds = if(condExp.data is InstanceOfExpr && condExp.data.source.data is ValExpr){
        //We can flow type
        val bind = condExp.data.source.data.source.data
        val typeCheck = condExp.data.type
        binds + Pair(bind, typeCheck.type())
    } else {
        binds
    }
    //restore expectations
    val thenExp = then.substituteTypeVars(ctx,nBinds)
    val otherExp = other.substituteTypeVars(ctx,binds)
    val retType = commonSuperType(thenExp.type(), otherExp.type())
    return IfExpr(
        extra.withFinalType(retType?:thenExp.type()),
        condExp,
        thenExp,
        otherExp
    )
}

fun LetExpr<ResolvedState>.substituteTypeVars(ctx: TypeResolutionContext, binds:Map<Binding,Type>):Expr<ResolvedState> {
    val nBind = bind.substituteTypeVars(ctx,binds)
    val nBinds = binds + Pair(name.data, nBind.type())
    val nBody = body.substituteTypeVars(ctx, nBinds)
    //Restore return value
    return LetExpr(
        extra.withFinalType(nBody.type()),
        name,
        nBind,
        nBody
    )
}


