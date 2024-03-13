package nora

interface Expr<L>{
    val extra:Map<Extra, Value>

    //for visitor pattern
    fun <T,C> accept(vis: ExprVisitor<L,T,C>, ctx:C): T
    fun children():List<Expr<L>>
}

data class InstanceOfExpr<L>(
    override val extra: Map<Extra, Value>,
    val source: Elem<Expr<L>>,
    val type: Elem<Expr<L>>
) : Expr<L> {
    override fun <T, C> accept(vis: ExprVisitor<L,T,C>, ctx: C): T = vis.visitInstanceOfExpr(this, ctx)
    override fun children(): List<Expr<L>> = listOf(source.data, type.data)
}

data class TypeHintExpr<L>(
    override val extra: Map<Extra, Value>,
    val target: Elem<Expr<L>>,
    val type:Elem<Expr<L>>
) : Expr<L> {
    override fun <T, C> accept(vis: ExprVisitor<L,T,C>, ctx: C): T = vis.visitTypeHintExpr(this, ctx)
    override fun children(): List<Expr<L>> = listOf(type.data)
}

data class CallExpr<L>(
    override val extra: Map<Extra, Value>,
    val target: Elem<Expr<L>>,
    val args:List<Elem<Expr<L>>>
) : Expr<L> {
    override fun <T, C> accept(vis: ExprVisitor<L,T,C>, ctx: C): T = vis.visitCallExpr(this, ctx)
    override fun children(): List<Expr<L>> = args.map { it.data } + target.data
}

//Just a special marker expression for empty bodies of prmitive functions
// conceptually takes on the signature of its parent function
data class PrimitiveExpr<L>(
    override val extra: Map<Extra, Value>,
    val target: Elem<String>,
) : Expr<L> {
    override fun <T, C> accept(vis: ExprVisitor<L,T,C>, ctx: C): T = vis.visitPrimitiveExpr(this, ctx)
    override fun children(): List<Expr<L>> = listOf()
}

data class CreateExpr<L>(
    override val extra: Map<Extra, Value>,
    val target: Elem<AppliedReference<L>>,
    val args:List<Elem<Expr<L>>>
) : Expr<L> {
    override fun <T, C> accept(vis: ExprVisitor<L,T,C>, ctx: C): T = vis.visitCreateExpr(this, ctx)
    override fun children(): List<Expr<L>> = args.map { it.data }
}

data class LitExpr<L>(
    override val extra: Map<Extra, Value>,
    val value: Elem<Any>,
) : Expr<L> {
    override fun <T, C> accept(vis: ExprVisitor<L,T,C>, ctx: C): T = vis.visitLitExpr(this, ctx)
    override fun children(): List<Expr<L>> = listOf()
}


//Todo: Change the parser to differentiate between a ref to args & lets
//      Track them in Context
//     vs: Some imported Definition
data class ValExpr<L>(
    override val extra: Map<Extra, Value>,
    val source: Elem<Loc>,
) : Expr<L> {
    override fun <T, C> accept(vis: ExprVisitor<L,T,C>, ctx: C): T = vis.visitValExpr(this, ctx)
    override fun children(): List<Expr<L>> = listOf()
}

data class FunLinkExpr<L>(
    override val extra: Map<Extra, Value>,
    val source: Elem<AppliedReference<L>>,
) : Expr<L> {
    override fun <T, C> accept(vis: ExprVisitor<L,T,C>, ctx: C): T = vis.visitFunLinkExpr(this, ctx)
    override fun children(): List<Expr<L>> = listOf()
}

data class TypeLinkExpr<L>(
    override val extra: Map<Extra, Value>,
    val source: Elem<AppliedReference<L>>,
) : Expr<L> {
    override fun <T, C> accept(vis: ExprVisitor<L,T,C>, ctx: C): T = vis.visitTypeLinkExpr(this, ctx)
    override fun children(): List<Expr<L>> = listOf()
}

data class FieldExpr<L>(
    override val extra: Map<Extra, Value>,
    val target: Elem<Expr<L>>,
    val name: Elem<String>,
) : Expr<L> {
    override fun <T, C> accept(vis: ExprVisitor<L,T,C>, ctx: C): T = vis.visitFieldExpr(this, ctx)
    override fun children(): List<Expr<L>> = listOf(target.data)
}

data class FunctionExpr<L>(
    override val extra: Map<Extra, Value>,
    val args: List<Elem<ClosureArgument<L>>>,
    val body: Elem<Expr<L>>
) : Expr<L> {
    override fun <T, C> accept(vis: ExprVisitor<L,T,C>, ctx: C): T = vis.visitFunctionExpr(this, ctx)
    override fun children(): List<Expr<L>> = listOf(body.data)
}

data class IfExpr<L>(
    override val extra: Map<Extra, Value>,
    val cond: Elem<Expr<L>>,
    val then: Elem<Expr<L>>,
    val other: Elem<Expr<L>>
) : Expr<L> {
    override fun <T, C> accept(vis: ExprVisitor<L,T,C>, ctx: C): T = vis.visitIfExpr(this, ctx)
    override fun children(): List<Expr<L>> = listOf(cond.data, then.data, other.data)
}

data class LetExpr<L>(
    override val extra: Map<Extra, Value>,
    val name: Elem<Loc>,
    val bind: Elem<Expr<L>>,
    val body: Elem<Expr<L>>
) : Expr<L> {
    override fun <T, C> accept(vis: ExprVisitor<L,T,C>, ctx: C): T = vis.visitLetExpr(this, ctx)
    override fun children(): List<Expr<L>> = listOf(bind.data, body.data)
}

