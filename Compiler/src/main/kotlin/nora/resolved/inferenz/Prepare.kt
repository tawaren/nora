package nora.resolved.inferenz

import nora.*
import nora.resolved.ResolvedState
import nora.typesys.TypeVar
import java.math.BigInteger

//-1 as default
data class PrepareContext(val next:Int, val varargs:Int = -1)

//Just for Dispatching
object ExprInferencePrepareVisitor: ExprVisitor<ResolvedState, Pair<Expr<ResolvedState>,Int>, PrepareContext> {
    override fun visitInstanceOfExpr(expr: InstanceOfExpr<ResolvedState>, ctx:PrepareContext) = expr.prepareInference(ctx)
    override fun visitTypeHintExpr(expr: TypeHintExpr<ResolvedState>, ctx:PrepareContext) = expr.prepareInference(ctx)
    override fun visitCallExpr(expr: CallExpr<ResolvedState>, ctx:PrepareContext) = expr.prepareInference(ctx)
    override fun visitPrimitiveExpr(expr: PrimitiveExpr<ResolvedState>, ctx:PrepareContext) = expr.prepareInference(ctx)
    override fun visitCreateExpr(expr: CreateExpr<ResolvedState>, ctx:PrepareContext) = expr.prepareInference(ctx)
    override fun visitLitExpr(expr: LitExpr<ResolvedState>, ctx:PrepareContext) = expr.prepareInference(ctx)
    override fun visitValExpr(expr: ValExpr<ResolvedState>, ctx:PrepareContext) = expr.prepareInference(ctx)
    override fun visitFunLinkExpr(expr: FunLinkExpr<ResolvedState>, ctx:PrepareContext) = expr.prepareInference(ctx)
    override fun visitTypeLinkExpr(expr: TypeLinkExpr<ResolvedState>, ctx:PrepareContext) = expr.prepareInference(ctx)
    override fun visitFieldExpr(expr: FieldExpr<ResolvedState>, ctx:PrepareContext) = expr.prepareInference(ctx)
    override fun visitFunctionExpr(expr: FunctionExpr<ResolvedState>, ctx:PrepareContext) = expr.prepareInference(ctx)
    override fun visitIfExpr(expr: IfExpr<ResolvedState>, ctx:PrepareContext) = expr.prepareInference(ctx)
    override fun visitLetExpr(expr: LetExpr<ResolvedState>, ctx:PrepareContext) = expr.prepareInference(ctx)
}

fun Elem<Expr<ResolvedState>>.prepareInference(ctx:PrepareContext): Pair<Elem<Expr<ResolvedState>>,Int> {
    val (nExp, next) = data.accept(ExprInferencePrepareVisitor, ctx)
    return Pair(Elem(nExp, src),next)
}

fun List<Elem<Expr<ResolvedState>>>.prepareInference(ctx:PrepareContext): Pair<List<Elem<Expr<ResolvedState>>>,Int> {
    return fold(Pair(listOf(),ctx.next)) { (l,n), a ->
        val (e,next) = a.prepareInference(PrepareContext(n))
        Pair(l+e,next)
    }
}

fun ClosureArgument<ResolvedState>.prepareInference(ctx:PrepareContext): Pair<ClosureArgument<ResolvedState>,Int> {
    val (nType, next) = if(type == null){
        Pair(Elem(TypeVar(ctx.next, name.src), name.src), ctx.next+1)
    } else {
        Pair(type, ctx.next)
    }
    return Pair(ClosureArgument(
        name,
        nType
    ), next)
}

fun List<Elem<ClosureArgument<ResolvedState>>>.prepareCloArgInference(ctx:PrepareContext): Pair<List<Elem<ClosureArgument<ResolvedState>>>,Int> {
    return fold(Pair(listOf(),ctx.next)) { (l,n), a ->
        val (e,next) = a.data.prepareInference(PrepareContext(n))
        Pair(l+Elem(e,a.src),next)
    }
}

fun Partial<ResolvedState>.prepareInference(ctx:PrepareContext): Pair<Provided<ResolvedState>,Int> {
    val (nParams, next) = parameters.fold(Pair(listOf<Elem<ResolvedState>>(),ctx.next)){ (l,n), p ->
        if(p.data == null) {
            Pair(l+Elem(TypeVar(n, p.src),p.src), n+1)
        } else Pair(l+Elem(p.data,p.src), n)
    }
    return Pair(Provided(nParams),next)
}

fun Hinted<ResolvedState>.prepareInference(base:Elem<String>, ctx:PrepareContext): Pair<Provided<ResolvedState>,Int> {
    val hintMap = hints.associate { Pair(it.name.data, it.value) }
    fun computeProvided(gens:List<Elem<Generic<ResolvedState>>>): Pair<Provided<ResolvedState>,Int> {
        val (params, next) = gens.fold(Pair(listOf<Elem<ResolvedState>>(),ctx.next)){ (l,n), g ->
            val assoc = hintMap[g.data.name.data.name]
            if(assoc != null){
                Pair(l+assoc,n)
            } else {
                Pair(l+Elem(TypeVar(n,base.src),base.src),n+1)
            }
        }
        return Pair(Provided(params),next)
    }

    return when(val def = resolvedDefinition(base.data).toNullable()!!){
        is VarArgParameterized<ResolvedState> -> {
            //Error would be better but a found 0 expected n error is ok as well
            if(ctx.varargs == -1) Pair(Provided(listOf()),ctx.next)
            else computeProvided(def.genGenerics(ctx.varargs))
        }
        is Parameterized<ResolvedState> -> computeProvided(def.generics)
        else -> Pair(Provided(listOf()),ctx.next)
    }
}

fun Applies<ResolvedState>.prepareInference(base:Elem<String>, ctx:PrepareContext): Pair<Applies<ResolvedState>,Int> {
   return when(this){
       is Hinted -> prepareInference(base, ctx)
       is Partial -> prepareInference(ctx)
       is Provided -> Pair(this, ctx.next)
   }

}

fun AppliedReference<ResolvedState>.prepareInference(ctx:PrepareContext): Pair<AppliedReference<ResolvedState>,Int> {
    val (nParam, next) = parameters.prepareInference(main.map { it.name }, ctx)
    return Pair(AppliedReference(
        main,
        nParam
    ), next)
}

fun InstanceOfExpr<ResolvedState>.prepareInference(ctx: PrepareContext):Pair<InstanceOfExpr<ResolvedState>,Int> {
    val (nSource, next1) = source.prepareInference(PrepareContext(ctx.next))
    val numApplies = nSource.findVarArgArity()
    val (nType, next2) = type.prepareInference(PrepareContext(next1, numApplies))
    return Pair(InstanceOfExpr(
        extra,
        nSource,
        nType
    ),next2)
}

fun TypeHintExpr<ResolvedState>.prepareInference(ctx: PrepareContext):Pair<TypeHintExpr<ResolvedState>,Int> {
    val (nTarget, next1) = target.prepareInference(ctx)
    val numApplies = if(ctx.varargs != -1) ctx.varargs else nTarget.findVarArgArity()
    val (nType, next2) = type.prepareInference(PrepareContext(next1, numApplies))
    return Pair(TypeHintExpr(
        extra,
        nTarget,
        nType
    ), next2)
}

fun CallExpr<ResolvedState>.prepareInference(ctx: PrepareContext):Pair<CallExpr<ResolvedState>,Int> {
    val (nTarget, next1) = target.prepareInference(ctx.copy(varargs = args.size+1))
    val (nArgs, next2) = args.prepareInference(PrepareContext(next1))
    return Pair(CallExpr(
        extra,
        nTarget,
        nArgs
    ),next2)
}

fun PrimitiveExpr<ResolvedState>.prepareInference(ctx: PrepareContext):Pair<PrimitiveExpr<ResolvedState>,Int> {
    return Pair(copy(extra = extra + Pair(Extra.TYPE_VAR, TypeValue(TypeVar(ctx.next, target.src)))), ctx.next+1)
}

fun CreateExpr<ResolvedState>.prepareInference(ctx: PrepareContext):Pair<CreateExpr<ResolvedState>,Int> {
    val (nTarget, next1) = target.data.prepareInference(ctx.copy(varargs = args.size))
    val (nArgs, next2) = args.prepareInference(PrepareContext(next1))
    return Pair(CreateExpr(
        extra,
        Elem(nTarget, target.src),
        nArgs
    ), next2)
}

fun LitExpr<ResolvedState>.prepareInference(ctx: PrepareContext):Pair<LitExpr<ResolvedState>,Int> {
    return when(value.data){
        is Byte -> Pair(copy(extra = extra + Pair(Extra.TYPE_VAR, TypeValue(TypeVar(ctx.next, value.src)))), ctx.next+1)
        is Int -> Pair(copy(extra = extra + Pair(Extra.TYPE_VAR, TypeValue(TypeVar(ctx.next, value.src)))), ctx.next+1)
        is Long -> Pair(copy(extra = extra + Pair(Extra.TYPE_VAR, TypeValue(NumType))), ctx.next)
        is BigInteger -> Pair(copy(extra = extra + Pair(Extra.TYPE_VAR, TypeValue(NumType))), ctx.next)
        is Boolean -> Pair(copy(extra = extra + Pair(Extra.TYPE_VAR, TypeValue(BoolType))), ctx.next)
        is String -> Pair(copy(extra = extra + Pair(Extra.TYPE_VAR, TypeValue(StringType))), ctx.next)
        else -> Pair(this, ctx.next)
    }
}

fun ValExpr<ResolvedState>.prepareInference(ctx: PrepareContext):Pair<ValExpr<ResolvedState>,Int> {
    return Pair(this, ctx.next)
}

fun FunLinkExpr<ResolvedState>.prepareInference(ctx: PrepareContext):Pair<FunLinkExpr<ResolvedState>,Int> {
    val (nSource, next) = source.data.prepareInference(ctx)
    return Pair(FunLinkExpr(
        extra,
        Elem(nSource, source.src)
    ), next)
}

fun TypeLinkExpr<ResolvedState>.prepareInference(ctx: PrepareContext):Pair<TypeLinkExpr<ResolvedState>,Int> {
    val (nSource, next) = source.data.prepareInference(ctx)
    return Pair(TypeLinkExpr(
        extra,
        Elem(nSource, source.src)
    ), next)
}

fun FieldExpr<ResolvedState>.prepareInference(ctx: PrepareContext):Pair<FieldExpr<ResolvedState>,Int> {
    val nExtra = extra + Pair(Extra.TYPE_VAR, TypeValue(TypeVar(ctx.next, name.src)))
    val (nTarget, next) = target.prepareInference(PrepareContext(ctx.next+1))
    return Pair(FieldExpr(
        nExtra,
        nTarget,
        name
    ), next)
}

fun FunctionExpr<ResolvedState>.prepareInference(ctx: PrepareContext):Pair<FunctionExpr<ResolvedState>,Int> {
    val nExtra = extra + Pair(Extra.TYPE_VAR, TypeValue(TypeVar(ctx.next,body.src)))
    val (nArgs, next1) = args.prepareCloArgInference(PrepareContext(ctx.next+1))
    val (nBody, next2) = body.prepareInference(PrepareContext(next1))
    return Pair(FunctionExpr(
        nExtra,
        nArgs,
        nBody
    ), next2)
}

fun IfExpr<ResolvedState>.prepareInference(ctx: PrepareContext):Pair<IfExpr<ResolvedState>,Int> {
    val (nCond, next1) = cond.prepareInference(PrepareContext(ctx.next))
    val (nThen, next2) = then.prepareInference(ctx.copy(next = next1))
    val (nOther, next3) = other.prepareInference(ctx.copy(next = next2))
    return Pair(IfExpr(
        extra,
        nCond,
        nThen,
        nOther
    ),next3)
}

fun LetExpr<ResolvedState>.prepareInference(ctx: PrepareContext):Pair<LetExpr<ResolvedState>,Int> {
    val nExtra = extra + Pair(Extra.TYPE_VAR, TypeValue(TypeVar(ctx.next,body.src)))
    val (nBind, next1) = bind.prepareInference(PrepareContext(ctx.next+1))
    val (nBody, next2) = body.prepareInference(ctx.copy(next = next1))
    return Pair(LetExpr(
        nExtra,
        name,
        nBind,
        nBody
    ), next2)
}

//Note: findVarArgArity is just a help for obvious cases
//      other cases we need the developer to provide
//      alla: (?,?) or Tuple<?,?> insteadOf Tuple
//            (?,?) => ? or Function[?,?,?] insteadOf Function

//Just for Dispatching
object ExprArityDetectorVisitor: ExprVisitor<ResolvedState, Int, Unit> {
    override fun visitExpr(expr: Expr<ResolvedState>, ctx: Unit): Int = -1
    override fun visitTypeHintExpr(expr: TypeHintExpr<ResolvedState>, ctx:Unit) = expr.findVarArgArity()
    override fun visitCreateExpr(expr: CreateExpr<ResolvedState>, ctx:Unit) = expr.findVarArgArity()
    override fun visitFunLinkExpr(expr: FunLinkExpr<ResolvedState>, ctx:Unit) = expr.findVarArgArity()
    override fun visitTypeLinkExpr(expr: TypeLinkExpr<ResolvedState>, ctx:Unit) = expr.findVarArgArity()
    override fun visitFunctionExpr(expr: FunctionExpr<ResolvedState>, ctx:Unit) = expr.findVarArgArity()
    override fun visitIfExpr(expr: IfExpr<ResolvedState>, ctx:Unit) = expr.findVarArgArity()
    override fun visitLetExpr(expr: LetExpr<ResolvedState>, ctx:Unit) = expr.findVarArgArity()
}

fun Elem<Expr<ResolvedState>>.findVarArgArity(): Int = data.accept(ExprArityDetectorVisitor, Unit)

fun TypeHintExpr<ResolvedState>.findVarArgArity():Int {
    val arity = target.findVarArgArity()
    if(arity != -1) return arity
    return type.findVarArgArity()
}

fun CreateExpr<ResolvedState>.findVarArgArity():Int = args.size

fun FunLinkExpr<ResolvedState>.findVarArgArity():Int {
    return when(val appls = source.data.parameters){
        is Hinted -> {
            when (val def = resolvedDefinition(source.data.main.data.name)) {
                is Callable<*> -> def.args.size+1
                //It should be a function as defined by FunLinkExpr
                else -> -1
            }
        }
        is Partial -> appls.parameters.size
        is Provided -> appls.parameters.size
    }
}

fun TypeLinkExpr<ResolvedState>.findVarArgArity():Int {
    return when(val appls = source.data.parameters){
        is Hinted -> {
            when (val def = resolvedDefinition(source.data.main.data.name)) {
                is VarArgParameterized<*> -> appls.hints.size //we can not save this one (make best effort)
                is Parameterized<*> -> def.totalArgs()
                //It should be a data type as defined by TypeLinkExpr
                else -> -1
            }
        }
        is Partial -> appls.parameters.size
        is Provided -> appls.parameters.size
    }
}

fun FunctionExpr<ResolvedState>.findVarArgArity():Int = args.size+1

fun IfExpr<ResolvedState>.findVarArgArity():Int {
    val thenVarArg = then.findVarArgArity()
    if(thenVarArg != -1) return thenVarArg
    return other.findVarArgArity()
}

fun LetExpr<ResolvedState>.findVarArgArity():Int = body.findVarArgArity()
