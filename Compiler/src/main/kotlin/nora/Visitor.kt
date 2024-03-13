package nora

import nora.Elem
import nora.Value

interface DefinitionVisitor<L, T, C> {
    fun visitDefinition(definition: Definition<L>, ctx:C): T = throw NotImplementedError();
    fun visitParameterized(parameterized: Parameterized<L>, ctx:C): T = visitDefinition(parameterized, ctx)
    fun visitCallable(callable: Callable<L>, ctx:C): T = visitParameterized(callable, ctx)
    fun visitImplemented(implemented: Implemented<L>, ctx:C): T = visitCallable(implemented, ctx)
    fun visitContainer(container: Container<L>, ctx:C): T = visitParameterized(container, ctx)
    fun visitMultiMethod(multiMethod: MultiMethod<L>, ctx:C): T = visitCallable(multiMethod, ctx)
    fun visitMethod(method: Method<L>, ctx:C): T = visitImplemented(method,ctx)
    fun visitCaseMethod(caseMethod: CaseMethod<L>, ctx:C): T = visitImplemented(caseMethod,ctx)
    fun visitObjectMethod(objectMethod: ObjectMethod<L>, ctx:C): T = visitImplemented(objectMethod,ctx)
    fun visitData(data: Data<L>, ctx:C): T = visitContainer(data,ctx)
    fun visitTuple(tuple: Tuple<L>, ctx:C): T = visitDefinition(tuple, ctx)
    fun visitFunction(function: Function<L>, ctx:C): T = visitDefinition(function, ctx)
    fun visitArray(array: Array<L>, ctx: C): T = visitParameterized(array, ctx)
    fun visitTrait(trait: Trait<L>, ctx: C): T = visitParameterized(trait, ctx)
    fun visitAnnotation(anot: Annotation<L>, ctx: C): T = visitDefinition(anot, ctx)

}


interface ExprVisitor<L, T, C> {
    fun merge(res:List<T>): T = if(res.size == 1) res[0] else throw NotImplementedError()
    fun visitExpr(expr: Expr<L>, ctx: C): T = merge(expr.children().map { it.accept(this, ctx) })
    fun visitInstanceOfExpr(expr: InstanceOfExpr<L>, ctx: C): T = visitExpr(expr, ctx)
    fun visitTypeHintExpr(expr: TypeHintExpr<L>, ctx: C): T = visitExpr(expr, ctx)
    fun visitCallExpr(expr: CallExpr<L>, ctx: C): T = visitExpr(expr, ctx)
    fun visitPrimitiveExpr(expr: PrimitiveExpr<L>, ctx: C): T = visitExpr(expr, ctx)
    fun visitCreateExpr(expr: CreateExpr<L>, ctx: C): T = visitExpr(expr, ctx)
    fun visitLitExpr(expr: LitExpr<L>, ctx: C): T = visitExpr(expr, ctx)
    fun visitValExpr(expr: ValExpr<L>, ctx: C): T = visitExpr(expr, ctx)
    fun visitFunLinkExpr(expr: FunLinkExpr<L>, ctx: C): T = visitExpr(expr, ctx)
    fun visitTypeLinkExpr(expr: TypeLinkExpr<L>, ctx: C): T = visitExpr(expr, ctx)
    fun visitFieldExpr(expr: FieldExpr<L>, ctx: C): T = visitExpr(expr, ctx)
    fun visitFunctionExpr(expr: FunctionExpr<L>, ctx: C): T = visitExpr(expr, ctx)
    fun visitIfExpr(expr: IfExpr<L>, ctx: C): T = visitExpr(expr, ctx)
    fun visitLetExpr(expr: LetExpr<L>, ctx: C): T = visitExpr(expr, ctx)
}