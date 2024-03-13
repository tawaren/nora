package nora.parsed.checking

import nora.*
import nora.Annotation
import nora.Array
import nora.Function

import nora.Elem
import nora.ParametricReference
import nora.primInfoByName

typealias RefState = ParametricReference

data class LinkingState(val checked:Set<String>, val cyclic:List<List<Elem<String>>>, val missing:List<Elem<String>>, val hidden:List<Elem<String>>)
data class FullContext(val state:LinkingState, val bindings:Set<Binding>,  val checking:Set<String>, val path:List<Elem<String>>)
data class ExprContext(val bindings:Set<Binding>)

//Just for Dispatching
object CycleAndLinkChecker : DefinitionVisitor<RefState, LinkingState, FullContext> {
    override fun visitDefinition(definition: Definition<RefState>, ctx: FullContext): LinkingState = definition.detectCyclesAndLink(ctx)
    override fun visitParameterized(parameterized: Parameterized<RefState>, ctx: FullContext): LinkingState = parameterized.detectCyclesAndLink(ctx)
    override fun visitCallable(callable: Callable<RefState>, ctx: FullContext): LinkingState = callable.detectCyclesAndLink(ctx)
    override fun visitImplemented(implemented: Implemented<RefState>, ctx: FullContext): LinkingState = implemented.detectCyclesAndLink(ctx)
    override fun visitContainer(container: Container<RefState>, ctx: FullContext): LinkingState = container.detectCyclesAndLink(ctx)
    override fun visitMultiMethod(multiMethod: MultiMethod<RefState>, ctx: FullContext): LinkingState = multiMethod.detectCyclesAndLink(ctx);
    override fun visitMethod(method: Method<RefState>, ctx: FullContext): LinkingState = method.detectCyclesAndLink(ctx)
    override fun visitCaseMethod(caseMethod: CaseMethod<RefState>, ctx:FullContext): LinkingState = caseMethod.detectCyclesAndLink(ctx)
    override fun visitObjectMethod(objectMethod: ObjectMethod<RefState>, ctx:FullContext): LinkingState = objectMethod.detectCyclesAndLink(ctx)
    override fun visitData(data: Data<RefState>, ctx:FullContext): LinkingState = data.detectCyclesAndLink(ctx)
    override fun visitTrait(trait: Trait<RefState>, ctx: FullContext): LinkingState = trait.detectCyclesAndLink(ctx)
    override fun visitTuple(tuple: Tuple<RefState>, ctx:FullContext): LinkingState = tuple.detectCyclesAndLink(ctx)
    override fun visitFunction(function: Function<RefState>, ctx:FullContext): LinkingState = function.detectCyclesAndLink(ctx)
    override fun visitArray(array: Array<RefState>,ctx:FullContext): LinkingState = array.detectCyclesAndLink(ctx)
    override fun visitAnnotation(anot: Annotation<RefState>, ctx: FullContext) = anot.detectCyclesAndLink(ctx)
}


private fun FullContext.cycleDetected(def1:Elem<String>):LinkingState {
    return state.copy(cyclic = state.cyclic.plusElement(path + def1))
}

private fun FullContext.missingDefinition(ref:Elem<Binding>):LinkingState {
    return state.copy(missing = state.missing.plusElement(Elem(ref.data.name, ref.src)))
}

private fun Elem<Binding>.detectCyclesAndLink(ctx: FullContext): LinkingState {
    return when(data){
        //A global name
        is Ref -> {
            if(ctx.state.checked.contains(data.name)) {
                ctx.state
            } else{
                val target = parsedDefinition(data.name).toNullable() ?: return ctx.missingDefinition(this)
                //Clear locals as we go global and add to path
                val nCtx = ctx.copy(
                    path = ctx.path + Elem(data.name, src),
                    bindings = setOf()
                )
                target.accept(CycleAndLinkChecker, nCtx)
            }
        }
        else -> {
            if(ctx.bindings.contains(data)){
                ctx.state
            } else {
                ctx.missingDefinition(this)
            }
        }
    }
}


private fun Applies<RefState>.detectCyclesAndLink(ctx: FullContext): LinkingState {
    return when(this){
        is Hinted<RefState> -> hints.fold(ctx){s,b -> s.copy(state = b.value.data.detectCyclesAndLink(s))}.state
        is Partial<RefState> -> parameters.fold(ctx){s,b -> if(b.data==null) s else s.copy(state = b.data.detectCyclesAndLink(s))}.state
        is Provided<RefState> -> parameters.fold(ctx){s,b -> s.copy(state = b.data.detectCyclesAndLink(s))}.state
    }
}

private fun RefState.detectCyclesAndLink(ctx: FullContext): LinkingState {
    val base = ctx.copy(state = main.detectCyclesAndLink(ctx))
    return parameters.fold(base){s,b -> s.copy(state = b.data.detectCyclesAndLink(s))}.state
}

private fun Generic<RefState>.detectCyclesAndLink(ctx: FullContext): LinkingState {
    val nCtx = ctx.copy(state = fallback?.data?.detectCyclesAndLink(ctx)?:ctx.state)
    return bounds.fold(nCtx){s,b -> s.copy(state = b.data.detectCyclesAndLink(s))}.state
}

private fun Argument<RefState>.detectCyclesAndLink(ctx: FullContext): LinkingState {
    return type.data.detectCyclesAndLink(ctx)
}

private fun Definition<RefState>.detectCyclesAndLink(ctx: FullContext): LinkingState {
    return annotations.fold(ctx){s,g -> s.copy(state = g.first.detectCyclesAndLink(s))}.state
}

private fun Parameterized<RefState>.detectCyclesAndLink(ctx: FullContext): LinkingState {
    val nCtx = ctx.copy(state = (this as Definition<RefState>).detectCyclesAndLink(ctx))
    return generics.fold(nCtx){s,g -> s.copy(
        state = g.data.detectCyclesAndLink(s),
        bindings = s.bindings + g.data.name.data
    )}.state
}

private fun Callable<RefState>.detectCyclesAndLink(ctx: FullContext): LinkingState {
    val withGenerics = ctx.copy(
        bindings = generics.map { it.data.name.data }.toSet(),
        state = (this as Parameterized<RefState>).detectCyclesAndLink(ctx)
    )
    val nAfterArgsState = args.fold(withGenerics){s,a -> s.copy(
        state = a.data.detectCyclesAndLink(s)
    )}
    return ret.data.detectCyclesAndLink(nAfterArgsState)
}

private fun Implemented<RefState>.detectCyclesAndLink(ctx: FullContext): LinkingState {
    val nCtx = ctx.copy(state = (this as Callable<RefState>).detectCyclesAndLink(ctx))
    val expCtx = ExprContext((args.map { it.data.name.data } + generics.map { it.data.name.data }).toSet())
    return nCtx.state.copy( missing = nCtx.state.missing + body.data.accept(ExprLinkChecker, expCtx))
}

private fun Container<RefState>.detectCyclesAndLink(ctx: FullContext): LinkingState {
    val nState = ctx.copy(state = (this as Parameterized<RefState>).detectCyclesAndLink(ctx))
    return fields.fold(nState){s,a -> s.copy(state = a.data.detectCyclesAndLink(s))}.state
}

private fun enterNewDef(name:Elem<String>, ctx: FullContext): FullContext{
    assert(parsedDefinition(name.data) !is None)
    if(ctx.state.checked.contains(name.data)) return ctx
    if(ctx.checking.contains(name.data)) return ctx.copy(state = ctx.cycleDetected(name))
    return ctx.copy(checking = ctx.checking + name.data, path = ctx.path + name)
}

private fun Method<RefState>.detectCyclesAndLink(ctx: FullContext): LinkingState {
    return (this as Implemented<RefState>).detectCyclesAndLink(enterNewDef(name, ctx))
}

private fun MultiMethod<RefState>.detectCyclesAndLink(ctx: FullContext): LinkingState {
    return (this as Callable<RefState>).detectCyclesAndLink(enterNewDef(name, ctx))
}

private fun CaseMethod<RefState>.detectCyclesAndLink(ctx: FullContext): LinkingState {
    val nCtx = enterNewDef(name, ctx)
    val nState = nCtx.copy(state = (this as Implemented<RefState>).detectCyclesAndLink(nCtx))
    val nState2 = parentApplies.fold(nState){ s,pa -> s.copy(state = pa.data.detectCyclesAndLink(s))}
    return parent.detectCyclesAndLink(nState2)
}

private fun ObjectMethod<RefState>.detectCyclesAndLink(ctx: FullContext): LinkingState {
    val nCtx = enterNewDef(name, ctx)
    val nState = nCtx.copy(state = (this as Implemented<RefState>).detectCyclesAndLink(nCtx))
    val nState2 = nCtx.copy(state = objectType.data.detectCyclesAndLink(nState))
    return hiddenGenerics.fold(nState2){s,g -> s.copy(state = g.data.detectCyclesAndLink(s))}.state
}

private fun Data<RefState>.detectCyclesAndLink(ctx: FullContext): LinkingState {
    val nCtx = enterNewDef(name, ctx)
    val nState = nCtx.copy(state = (this as Container<RefState>).detectCyclesAndLink(nCtx))
    val nState2 = hiddenGenerics.fold(nState){s,g -> s.copy(state = g.data.detectCyclesAndLink(s))}
    val nState3 = hiddenFields.fold(nState2){s,a -> s.copy(state = a.data.detectCyclesAndLink(s))}
    val res = traits.fold(nState3){s,t -> s.copy(state = t.data.detectCyclesAndLink(s))}.state
    if(parent == null) return res
    return parent.detectCyclesAndLink(nState3.copy(state = res))
}

private fun Trait<RefState>.detectCyclesAndLink(ctx: FullContext): LinkingState {
    val nCtx = enterNewDef(name, ctx)
    val nState = nCtx.copy(state = (this as Definition<RefState>).detectCyclesAndLink(nCtx))
    return traits.fold(nState){s,t -> s.copy(state = t.data.detectCyclesAndLink(s))}.state
}

private fun Tuple<RefState>.detectCyclesAndLink(ctx: FullContext): LinkingState {
    val nCtx = enterNewDef(name, ctx)
    val nState = nCtx.copy(state = (this as Definition<RefState>).detectCyclesAndLink(nCtx))
    return traits.fold(nState){s,t -> s.copy(state = t.data.detectCyclesAndLink(s))}.state
}

private fun Function<RefState>.detectCyclesAndLink(ctx: FullContext): LinkingState {
    val nCtx = enterNewDef(name, ctx)
    val nState = nCtx.copy(state = (this as Definition<RefState>).detectCyclesAndLink(nCtx))
    return traits.fold(nState){s,t -> s.copy(state = t.data.detectCyclesAndLink(s))}.state
}

private fun Array<RefState>.detectCyclesAndLink(ctx: FullContext): LinkingState {
    val nCtx = enterNewDef(name, ctx)
    val nState = nCtx.copy(state = (this as Parameterized<RefState>).detectCyclesAndLink(nCtx))
    val nState2 = traits.fold(nState){s,t -> s.copy(state = t.data.detectCyclesAndLink(s))}
    val res = elemType.data.detectCyclesAndLink(nState2);
    if(parent == null) return res;
    return parent.detectCyclesAndLink(nState2.copy(state = res))
}


//Just for Dispatching
object ExprLinkChecker: ExprVisitor<RefState,List<Elem<String>>, ExprContext> {
    override fun merge(res: List<List<Elem<String>>>): List<Elem<String>> = res.flatten()
    override fun visitPrimitiveExpr(expr: PrimitiveExpr<RefState>, ctx: ExprContext): List<Elem<String>> = expr.checkLinks(ctx)
    override fun visitCreateExpr(expr: CreateExpr<RefState>, ctx: ExprContext): List<Elem<String>> = expr.checkLinks(ctx)
    override fun visitLetExpr(expr: LetExpr<RefState>, ctx: ExprContext): List<Elem<String>>  = expr.checkLinks(ctx)
    override fun visitValExpr(expr: ValExpr<RefState>, ctx: ExprContext): List<Elem<String>> = expr.checkLinks(ctx)
    override fun visitFunLinkExpr(expr: FunLinkExpr<RefState>, ctx: ExprContext): List<Elem<String>> = expr.checkLinks(ctx)
    override fun visitTypeLinkExpr(expr: TypeLinkExpr<RefState>, ctx: ExprContext): List<Elem<String>> = expr.checkLinks(ctx)
    override fun visitFunctionExpr(expr: FunctionExpr<RefState>, ctx: ExprContext): List<Elem<String>> = expr.checkLinks(ctx)
}


fun PrimitiveExpr<RefState>.checkLinks(ctx: ExprContext):List<Elem<String>> {
    val nState = ExprLinkChecker.visitExpr(this, ctx)
    if(primInfoByName.contains(target.data)) return nState
    return nState + target
}

fun Elem<Binding>.checkLinks(ctx: ExprContext):List<Elem<String>> {
    return when(data){
        //A global name
        is Ref -> {
            if (parsedDefinition(data.name) !is None) {
                return listOf()
            } else {
                listOf(Elem(data.name, src))
            }
        }
        else -> {
            if(ctx.bindings.contains(data)){
                listOf()
            } else {
                listOf(Elem(data.name, src))
            }
        }
    }
}

fun ParametricReference.checkLinks(ctx: ExprContext):List<Elem<String>> {
    val errors = main.checkLinks(ctx)
    return errors + parameters.flatMap{ it.data.checkLinks(ctx)}
}


private fun Applies<RefState>.checkLinks(ctx: ExprContext): List<Elem<String>> {
    return when(this){
        is Hinted<RefState> -> hints.flatMap{ it.value.data.checkLinks(ctx)}
        is Partial<RefState> -> parameters.flatMap{ it.data?.checkLinks(ctx) ?: listOf() }
        is Provided<RefState> -> parameters.flatMap{ it.data.checkLinks(ctx)}
    }
}

fun AppliedReference<RefState>.checkLinks(ctx: ExprContext):List<Elem<String>> {
    val errors = main.checkLinks(ctx)
    return errors + parameters.checkLinks(ctx)
}

fun CreateExpr<RefState>.checkLinks(ctx: ExprContext):List<Elem<String>> {
    return ExprLinkChecker.visitExpr(this, ctx) + target.data.checkLinks(ctx)
}

fun LetExpr<RefState>.checkLinks(ctx: ExprContext):List<Elem<String>> {
    val nState = bind.data.accept(ExprLinkChecker, ctx)
    val nCtx = ctx.copy(bindings = ctx.bindings + name.data)
    return body.data.accept(ExprLinkChecker, nCtx) + nState
}

fun ValExpr<RefState>.checkLinks(ctx: ExprContext):List<Elem<String>> {
    val nState = ExprLinkChecker.visitExpr(this, ctx)
    return if(!ctx.bindings.contains(source.data)) {
        nState + source.map { it.name }
    } else {
        nState
    }
}

fun FunLinkExpr<RefState>.checkLinks(ctx: ExprContext):List<Elem<String>> {
    return ExprLinkChecker.visitExpr(this, ctx) + source.data.checkLinks(ctx)
}

fun TypeLinkExpr<RefState>.checkLinks(ctx: ExprContext):List<Elem<String>> {
    return ExprLinkChecker.visitExpr(this, ctx) + source.data.checkLinks(ctx)
}

fun FunctionExpr<RefState>.checkLinks(ctx: ExprContext):List<Elem<String>> {
    val nState = args.fold(listOf<Elem<String>>()){s,a -> s + (a.data.type?.data?.checkLinks(ctx)?:listOf()) }
    val nCtx = ctx.copy(bindings = ctx.bindings + args.map { it.data.name.data }.toSet())
    return nState + ExprLinkChecker.visitExpr(this, nCtx)
}
