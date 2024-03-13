package nora.codegen

import nora.*
import nora.resolved.ResolvedState
import nora.resolved.inferenz.type
import nora.resolved.inferenz.unifyInstanceAndKeepType
import nora.typesys.Type

data class State(val locals:Map<Loc,Int> = mapOf(), val captures:Map<Loc,Int> = mapOf())
data class BindingContext(val state:State = State(), val lets:Set<Loc> = setOf(), val args:Map<Loc, Int> = mapOf(), val indirect:Set<Loc> = setOf(), val types:Map<Loc, Type> = mapOf())
data class ExprInfo(val res:Expr<ResolvedState>, val state:State)

fun BindingContext.produceRes(res:Expr<ResolvedState>) = ExprInfo(res,state)
fun ExprInfo.produceRes(res:Expr<ResolvedState>) = ExprInfo(res,state)

fun BindingContext.bindLocal(loc:Loc):Pair<BindingContext,Int> {
    return if(state.locals.containsKey(loc)) {
        Pair(this, state.locals[loc]!!)
    } else {
        Pair(copy(state = state.copy(captures = state.locals + Pair(loc, state.locals.size))), state.locals.size)
    }
}

fun BindingContext.bindCapture(loc:Loc):Pair<BindingContext,Int> {
    return if(state.captures.containsKey(loc)) {
        Pair(this, state.captures[loc]!!)
    } else {
        Pair(copy(state = state.copy(captures = state.captures + Pair(loc, state.captures.size))), state.captures.size)
    }
}

fun BindingContext.integrate(res:ExprInfo) = copy(state = res.state)

//Todo: we need dispatch for reconstruction
fun Definition<ResolvedState>.rebindNames(): Definition<ResolvedState> {
    return accept(DefNameBinder, Unit)
}

//Just for Dispatching
object DefNameBinder : DefinitionVisitor<ResolvedState, Definition<ResolvedState>, Unit> {
    override fun visitDefinition(definition: Definition<ResolvedState>, ctx: Unit) = definition
    override fun visitMethod(method: Method<ResolvedState>, ctx: Unit) = method.rebindNames()
    override fun visitCaseMethod(caseMethod: CaseMethod<ResolvedState>, ctx: Unit) = caseMethod.rebindNames()
    override fun visitObjectMethod(objectMethod: ObjectMethod<ResolvedState>, ctx: Unit) = objectMethod.rebindNames()
}

fun Implemented<ResolvedState>.rebindBodyNames(): ExprInfo {
    val (_,argBindings) = args.fold(Pair(0, mapOf<Loc,Int>())) { (c, m), a -> Pair(c+1, m + Pair(a.data.name.data, c)) }
    val argTypes = args.fold(mapOf<Loc,Type>()) { m, a -> m + Pair(a.data.name.data, a.data.type.data as Type) }
    return body.data.rebindNames(BindingContext(args = argBindings, types = argTypes))
}

fun Method<ResolvedState>.rebindNames(): Method<ResolvedState> {
    val nBody = rebindBodyNames()
    return copy(
        extra = extra + Pair(Extra.LOCALS, IntValue(nBody.state.locals.size)),
        body = body.map { nBody.res }
    )
}

fun CaseMethod<ResolvedState>.rebindNames(): CaseMethod<ResolvedState> {
    val nBody = rebindBodyNames()
    return copy(
        extra = extra + Pair(Extra.LOCALS, IntValue(nBody.state.locals.size)),
        body = body.map { nBody.res }
    )
}

fun ObjectMethod<ResolvedState>.rebindNames(): ObjectMethod<ResolvedState> {
    val nBody = rebindBodyNames()
    return copy(
        extra = extra + Pair(Extra.LOCALS, IntValue(nBody.state.locals.size)),
        body = body.map { nBody.res }
    )
}

fun Expr<ResolvedState>.rebindNames(ctx: BindingContext): ExprInfo {
    return accept(NameBinder, ctx)
}

//Just for Dispatching
object NameBinder: ExprVisitor<ResolvedState, ExprInfo, BindingContext> {
    override fun visitInstanceOfExpr(expr: InstanceOfExpr<ResolvedState>, ctx: BindingContext) = expr.rebindNames(ctx)
    override fun visitTypeHintExpr(expr: TypeHintExpr<ResolvedState>, ctx: BindingContext) = expr.rebindNames(ctx)
    override fun visitCallExpr(expr: CallExpr<ResolvedState>, ctx: BindingContext) = expr.rebindNames(ctx)
    override fun visitPrimitiveExpr(expr: PrimitiveExpr<ResolvedState>, ctx: BindingContext) = expr.rebindNames(ctx)
    override fun visitCreateExpr(expr: CreateExpr<ResolvedState>, ctx: BindingContext) = expr.rebindNames(ctx)
    override fun visitLitExpr(expr: LitExpr<ResolvedState>, ctx: BindingContext) = expr.rebindNames(ctx)
    override fun visitValExpr(expr: ValExpr<ResolvedState>, ctx: BindingContext) = expr.rebindNames(ctx)
    override fun visitFunLinkExpr(expr: FunLinkExpr<ResolvedState>, ctx: BindingContext) = expr.rebindNames(ctx)
    override fun visitTypeLinkExpr(expr: TypeLinkExpr<ResolvedState>, ctx: BindingContext) = expr.rebindNames(ctx)
    override fun visitFieldExpr(expr: FieldExpr<ResolvedState>, ctx: BindingContext) = expr.rebindNames(ctx)
    override fun visitFunctionExpr(expr: FunctionExpr<ResolvedState>, ctx: BindingContext) = expr.rebindNames(ctx)
    override fun visitIfExpr(expr: IfExpr<ResolvedState>, ctx: BindingContext) = expr.rebindNames(ctx)
    override fun visitLetExpr(expr: LetExpr<ResolvedState>, ctx: BindingContext) = expr.rebindNames(ctx)
}

fun InstanceOfExpr<ResolvedState>.rebindNames(ctx: BindingContext):ExprInfo {
    val nSource = source.data.rebindNames(ctx)
    val nType = type.data.rebindNames(ctx.integrate(nSource))
    return nType.produceRes(copy(
        source = source.map { nSource.res },
        type = type.map { nType.res }
    ))
}

fun TypeHintExpr<ResolvedState>.rebindNames(ctx: BindingContext):ExprInfo {
    val nTarget = target.data.rebindNames(ctx)
    val nType = type.data.rebindNames(ctx.integrate(nTarget))
    return nType.produceRes(copy(
        target = target.map { nTarget.res },
        type = type.map { nType.res }
    ))
}

fun CallExpr<ResolvedState>.rebindNames(ctx: BindingContext):ExprInfo {
    val nTarget = target.data.rebindNames(ctx)
    val (nCtx, nArgs) = args.fold(Pair(ctx.integrate(nTarget), listOf<Elem<Expr<ResolvedState>>>())) { (c,l), a ->
        val r = a.data.rebindNames(c)
        Pair(c.integrate(r), l + a.map { r.res })
    }
    return nCtx.produceRes(copy(
        target = target.map { nTarget.res },
        args = nArgs
    ))
}

fun PrimitiveExpr<ResolvedState>.rebindNames(ctx: BindingContext):ExprInfo {
    return ctx.produceRes(this)
}

fun CreateExpr<ResolvedState>.rebindNames(ctx: BindingContext):ExprInfo {
    val (nCtx, nArgs) = args.fold(Pair(ctx, listOf<Elem<Expr<ResolvedState>>>())) { (c,l), a ->
        val r = a.data.rebindNames(c)
        Pair(c.integrate(r), l + a.map { r.res })
    }
    return nCtx.produceRes(copy(
        args = nArgs
    ))
}

fun LitExpr<ResolvedState>.rebindNames(ctx: BindingContext):ExprInfo {
    return ctx.produceRes(this)
}

fun ValExpr<ResolvedState>.rebindNames(ctx: BindingContext):ExprInfo {
    return if(ctx.lets.contains(source.data)){
        val (c, slot) = ctx.bindLocal(source.data)
        c.produceRes(copy( extra = extra + Pair(Extra.SLOT, SlotValue(SlotKind.LOCAL, slot))))
    } else if(ctx.args.containsKey(source.data)) {
        val slot = ctx.args[source.data]!!
        ctx.produceRes(copy( extra = extra + Pair(Extra.SLOT, SlotValue(SlotKind.ARG,slot))))
    } else if(ctx.indirect.contains(source.data)) {
        val (c, slot) = ctx.bindCapture(source.data)
        c.produceRes(copy( extra = extra + Pair(Extra.SLOT, SlotValue(SlotKind.CAPTURE,slot))))
    } else {
        assert(false)//Should not happen
        ctx.produceRes(this)
    }
}

fun FunLinkExpr<ResolvedState>.rebindNames(ctx: BindingContext):ExprInfo {
    return ctx.produceRes(this)
}

fun TypeLinkExpr<ResolvedState>.rebindNames(ctx: BindingContext):ExprInfo {
  return ctx.produceRes(this)
}

fun FieldExpr<ResolvedState>.rebindNames(ctx: BindingContext):ExprInfo {
    val nTarget = target.data.rebindNames(ctx)
    return nTarget.produceRes(copy(
        target = target.map { nTarget.res },
    ))
}

fun FunctionExpr<ResolvedState>.rebindNames(ctx: BindingContext):ExprInfo {
    val indirect = ctx.args.keys + ctx.lets + ctx.indirect
    val nArgBinds = args.fold(mapOf<Loc,Int>()){ m, a -> m + Pair(a.data.name.data, m.size) }
    val nArgTypes = args.fold(mapOf<Loc,Type>()){ m, a -> m + Pair(a.data.name.data, a.data.type?.data as Type) }
    val bodyCtx = BindingContext(args = nArgBinds, types = nArgTypes, indirect = indirect)
    val nBody = body.data.rebindNames(bodyCtx)
    val (nCtx, captures) = nBody.state.captures.keys.fold(Pair(ctx, listOf<Pair<SlotValue,Type>>())) { (c,l), capt ->
        val typ = ctx.types[capt]!!
        if(ctx.lets.contains(capt)){
            val (nc, slot) = ctx.bindLocal(capt)
            Pair(nc, l + Pair(SlotValue(SlotKind.LOCAL, slot), typ))
        } else if(ctx.args.containsKey(capt)) {
            val slot = ctx.args[capt]!!
            Pair(c, l + Pair(SlotValue(SlotKind.ARG, slot),typ))
        } else if(ctx.indirect.contains(capt)) {
            val (nc, slot) = ctx.bindCapture(capt)
            Pair(nc, l + Pair(SlotValue(SlotKind.CAPTURE, slot),typ))
        } else {
            assert(false)//Should not happen
            Pair(c,l)
        }
    }
    return nCtx.produceRes(copy(
        extra = extra + Pair(Extra.CAPTURES, CaptureValue(captures)) + Pair(Extra.LOCALS, IntValue(bodyCtx.state.locals.size)),
        body = body.map { nBody.res }
    ))
}

fun IfExpr<ResolvedState>.rebindNames(ctx: BindingContext):ExprInfo {
    val nCond = cond.data.rebindNames(ctx)
    val condCtx = ctx.integrate(nCond)
    //Todo make reusable is used in more than one place
    val bindCtx = if(cond.data is InstanceOfExpr && cond.data.source.data is ValExpr){
        //We can flow type
        val bind = cond.data.source.data.source.data
        condCtx.copy(types = condCtx.types +  Pair(bind, cond.data.type.type()))
    } else {
        //We can not fow type
        condCtx
    }
    val nThen = then.data.rebindNames(bindCtx)
    val nOther = then.data.rebindNames(condCtx.integrate(nThen))

    return nOther.produceRes(copy(
        cond = cond.map { nCond.res },
        then = then.map { nThen.res },
        other = other.map { nOther.res },
    ))
}

fun LetExpr<ResolvedState>.rebindNames(ctx: BindingContext):ExprInfo {
    val (nCtx, slot) = ctx.bindLocal(name.data)
    val nBind = bind.data.rebindNames(nCtx)
    val bodyCtx = nCtx.copy(types = nCtx.types + Pair(name.data, bind.type())).integrate(nBind)
    val nBody = body.data.rebindNames(bodyCtx)
    return nBody.produceRes(copy(
        extra = extra + Pair(Extra.SLOT, SlotValue(SlotKind.LOCAL,slot)),
        bind = bind.map { nBind.res },
        body = body.map { nBody.res }
    ))
}