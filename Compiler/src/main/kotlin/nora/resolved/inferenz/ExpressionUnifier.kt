package nora.resolved.inferenz

import nora.*
import nora.Array
import nora.resolved.ResolvedState
import nora.resolved.getAllFields
import nora.resolved.getField
import nora.typesys.*

//Just for Dispatching
object ExprUnifierVisitor: ExprVisitor<ResolvedState, Pair<Type,InferenceContext>, Pair<InferenceContext, Map<Binding,Type>>> {
    override fun visitInstanceOfExpr(expr: InstanceOfExpr<ResolvedState>, ctx:Pair<InferenceContext, Map<Binding,Type>>) = expr.unifyExpression(ctx.first, ctx.second)
    override fun visitTypeHintExpr(expr: TypeHintExpr<ResolvedState>, ctx:Pair<InferenceContext, Map<Binding,Type>>) = expr.unifyExpression(ctx.first, ctx.second)
    override fun visitCallExpr(expr: CallExpr<ResolvedState>, ctx:Pair<InferenceContext, Map<Binding,Type>>) = expr.unifyExpression(ctx.first, ctx.second)
    override fun visitPrimitiveExpr(expr: PrimitiveExpr<ResolvedState>, ctx:Pair<InferenceContext, Map<Binding,Type>>) = expr.unifyExpression(ctx.first, ctx.second)
    override fun visitCreateExpr(expr: CreateExpr<ResolvedState>, ctx:Pair<InferenceContext, Map<Binding,Type>>) = expr.unifyExpression(ctx.first, ctx.second)
    override fun visitLitExpr(expr: LitExpr<ResolvedState>, ctx:Pair<InferenceContext, Map<Binding,Type>>) = expr.unifyExpression(ctx.first, ctx.second)
    override fun visitValExpr(expr: ValExpr<ResolvedState>, ctx:Pair<InferenceContext, Map<Binding,Type>>) = expr.unifyExpression(ctx.first, ctx.second)
    override fun visitFunLinkExpr(expr: FunLinkExpr<ResolvedState>, ctx:Pair<InferenceContext, Map<Binding,Type>>) = expr.unifyExpression(ctx.first, ctx.second)
    override fun visitTypeLinkExpr(expr: TypeLinkExpr<ResolvedState>, ctx:Pair<InferenceContext, Map<Binding,Type>>) = expr.unifyExpression(ctx.first, ctx.second)
    override fun visitFieldExpr(expr: FieldExpr<ResolvedState>, ctx:Pair<InferenceContext, Map<Binding,Type>>) = expr.unifyExpression(ctx.first, ctx.second)
    override fun visitFunctionExpr(expr: FunctionExpr<ResolvedState>, ctx:Pair<InferenceContext, Map<Binding,Type>>) = expr.unifyExpression(ctx.first, ctx.second)
    override fun visitIfExpr(expr: IfExpr<ResolvedState>, ctx:Pair<InferenceContext, Map<Binding,Type>>) = expr.unifyExpression(ctx.first, ctx.second)
    override fun visitLetExpr(expr: LetExpr<ResolvedState>, ctx:Pair<InferenceContext, Map<Binding,Type>>) = expr.unifyExpression(ctx.first, ctx.second)
}

fun Elem<Expr<ResolvedState>>.unifyExpression(ctx: InferenceContext, binds:Map<Binding,Type>):Pair<Type,InferenceContext> {
    return data.accept(ExprUnifierVisitor, Pair(ctx,binds))
}

fun InstanceOfExpr<ResolvedState>.unifyInstanceAndKeepType(ctx: InferenceContext, binds:Map<Binding,Type>):Pair<Type,InferenceContext> {
    val (retType,nCtx) = type.unifyExpression(ctx,binds)
    val fCtx = nCtx.expectReturnType(retType){source.unifyExpression(it,binds)}
    return Pair(retType, fCtx)
}

fun InstanceOfExpr<ResolvedState>.unifyExpression(ctx: InferenceContext, binds:Map<Binding,Type>):Pair<Type,InferenceContext> {
    val (_,nCtx) = unifyInstanceAndKeepType(ctx,binds)
    return Pair(BoolType, nCtx)
}

fun TypeHintExpr<ResolvedState>.unifyExpression(ctx: InferenceContext, binds:Map<Binding,Type>):Pair<Type,InferenceContext> {
    val (retType,nCtx) = type.unifyExpression(ctx,binds)
    val fCtx = nCtx.expectReturnType(retType){target.unifyExpression(it,binds)}
    //We return the potential non-type var, to loosen the coupling
    //  Further the developer may have put it here to guide inference
    return Pair(retType,fCtx)
}

fun CallExpr<ResolvedState>.unifyExpression(ctx: InferenceContext, binds:Map<Binding,Type>):Pair<Type,InferenceContext> {
    val (retType,funCtx) = target.unifyExpression(ctx,binds)
    val funT = funCtx.getType(retType)
    val (nCtx, lt) = when(funT){
        is DataType -> {
            assert(funT.parameters.size > args.size)
            val retT = funT.parameters.last().param.data
            val retRes = funCtx.getType(retT)
            Pair(
                funT.parameters.zip(args).fold(funCtx){c, (t,a) -> c.expectReturnType(t.param.data){a.unifyExpression(it,binds)}},
                //loosen coupling if we already have a fixed type
                retRes?:retT
            )
        }
        //Something already is wrong for us not getting a DataType
        else -> Pair(
            args.fold(funCtx){ c, a -> a.unifyExpression(c,binds).second},
            UnknownType
        )
    }
    return Pair(lt,nCtx)
}

fun PrimitiveExpr<ResolvedState>.unifyExpression(ctx: InferenceContext, binds:Map<Binding,Type>):Pair<Type,InferenceContext> {
    val tVar = (extra[Extra.TYPE_VAR] as TypeValue).typ
    return Pair(tVar,ctx)
}

fun InferenceContext.unifyDataType(ref:AppliedReference<ResolvedState>):Pair<Type,InferenceContext>{
    val args = (ref.parameters as Provided<ResolvedState>).parameters
    val genDefs = when(val def = resolvedDefinition(ref.main.data.name).toNullable()){
        is VarArgParameterized<ResolvedState> -> def.genGenerics(args.size)
        is Parameterized<ResolvedState> -> def.generics
        //should never happen as it should be checked by previous passes
        else -> listOf()
    }
    val gens = args.map { it.data as Type }.toTypedArray()
    val tArgs = genDefs.zip(args).map { TypeParam(it.first.data.variance.data, it.second.map {t -> t as Type}) }
    val dCtx = genDefs.zip(gens).fold(this){c, (g,t) ->
        if(t is TypeVar && g.data.fallback != null){
            c.addFallback(t,(g.data.fallback.data as Type).substitute(gens))
        } else {
            c
        }.unifyConstraints(gens, g.data, t)
    }
    return Pair(DataType(ref.main.map { it as Ref }, tArgs), dCtx)
}

fun CreateExpr<ResolvedState>.unifyExpression(ctx: InferenceContext, binds:Map<Binding,Type>):Pair<Type,InferenceContext> {
    val (dataT, dataCtx) = ctx.unifyDataType(target.data)
    //unifyDataType always returns a concrete data type
    val fieldTs = when(val def = resolvedDefinition(target.data.main.data.name).toNullable()){
        is Tuple<ResolvedState> -> (dataT as DataType).parameters.map { it.param.data }
        is Array<ResolvedState> -> {
            val elem = (dataT as DataType).parameters.first()
            List(args.size){elem.param.data}
        }
        is Data<ResolvedState> -> {
            val gens = (target.data.parameters as Provided<ResolvedState>).parameters.map { it.data as Type }.toTypedArray()
            def.getAllFields(true).map { (it.data.type.data as Type).substitute(gens) }
        }
        else -> listOf()
    }
    val finCtx = fieldTs.zip(args).fold(dataCtx){c, (t,a) -> c.expectReturnType(t){a.unifyExpression(it,binds)}}
    return Pair(dataT, finCtx)
}

fun LitExpr<ResolvedState>.unifyExpression(ctx: InferenceContext, binds:Map<Binding,Type>):Pair<Type,InferenceContext> {
    val tVar = (extra[Extra.TYPE_VAR] as TypeValue).typ
    val nCtx = when(value.data){
        is Byte -> ctx.addFallback(tVar as TypeVar, IntType)
        is Int -> ctx.addFallback(tVar as TypeVar, IntType)
        else -> ctx
    }
    return Pair(tVar,nCtx)
}

fun ValExpr<ResolvedState>.unifyExpression(ctx: InferenceContext, binds:Map<Binding,Type>):Pair<Type,InferenceContext> {
    return Pair(binds[source.data]!!,ctx)
}

//Todo: Allow to register lastResorts
fun FunLinkExpr<ResolvedState>.unifyExpression(ctx: InferenceContext, binds:Map<Binding,Type>):Pair<Type,InferenceContext> {
    val def = resolvedDefinition(source.data.main.data.name).toNullable() as Callable
    val gens = (source.data.parameters as Provided<ResolvedState>).parameters.map { it.data as Type }.toTypedArray()
    val nArgs = def.args.map { (it.data.type.data as Type).substitute(gens) }
    val nRet = (def.ret.data as Type).substitute(gens)
    val fCtx = def.generics.zip(gens).fold(ctx){c, (g,t) ->
        if(t is TypeVar && g.data.fallback != null){
            c.addFallback(t,(g.data.fallback.data as Type).substitute(gens))
        } else {
            c
        }.unifyConstraints(gens, g.data, t)
    }
    //Todo: If def is a multi method Register lastResorts that if not all are typed, searches method table
    //      Be save and only if a single match or one match is more specific independent of generics


    return Pair(FunctionType(nRet, *nArgs.toTypedArray()), fCtx)
}

fun TypeLinkExpr<ResolvedState>.unifyExpression(ctx: InferenceContext, binds:Map<Binding,Type>):Pair<Type,InferenceContext> {
    return ctx.unifyDataType(source.data)
}

fun FieldExpr<ResolvedState>.unifyExpression(ctx: InferenceContext, binds:Map<Binding,Type>):Pair<Type,InferenceContext> {
    //Note: in literature, this is the hard case, because technically we should set an upper bound on target, based on the field
    //      However, we do not know that upperbound: That's way in haskell fields share a global namespace and in ML they use record Types
    //      That's why we leave it open if we get a type error then the developer can use x:Type.field
    //      Luckily we use a combination of HM and flow typing which is equipped to handle cases like this in many cases
    val (retType, nCtx) = target.unifyExpression(ctx, binds)
    val fieldVar = (extra[Extra.TYPE_VAR] as TypeValue).typ
    val resCtx = nCtx.whenTyped(retType,{it}){ c, dataT ->
        when(dataT){
            is DataType -> {
                when(val def = resolvedDefinition(dataT.base.data.name).toNullable()){
                    is Tuple<ResolvedState> -> {
                        if(name.data.startsWith("$")){
                            try {
                              c.unify(fieldVar,dataT.parameters[name.data.substring(1).toInt()].param.data)
                            } catch (e:Exception){
                                c
                            }
                        } else {
                            c
                        }
                    }
                    is Data<ResolvedState> -> {
                        val t = def.getField(name.data)
                        if(t == null) {
                            c
                        } else {
                            val subst = dataT.parameters.map { it.param.data }.toTypedArray()
                            c.unify(fieldVar,(t.data.type.data as Type).substitute(subst))
                        }
                    }
                    else -> c
                }
            }
            is GenericType -> TODO("We can look fo single data type bound and use that")
            else -> c
        }
    }
    //For the case the field already has a concrete type return it - break coupling
    return Pair(resCtx.getType(fieldVar) ?: fieldVar, resCtx)
}

fun FunctionExpr<ResolvedState>.unifyExpression(ctx: InferenceContext, binds:Map<Binding,Type>):Pair<Type,InferenceContext> {
    val retT = (extra[Extra.TYPE_VAR] as TypeValue).typ
    val argTs = args.map { it.data.type!!.data as Type }
    val nBinds = argTs.zip(args).fold(binds) {b, (t,a) -> b + Pair(a.data.name.data, t)}
    val nCtx = ctx.expectReturnType(retT){body.unifyExpression(it,nBinds)}
    return Pair(FunctionType(retT,*argTs.toTypedArray()), nCtx)
}

fun IfExpr<ResolvedState>.unifyExpression(ctx: InferenceContext, binds:Map<Binding,Type>):Pair<Type,InferenceContext> {
    //Flow typing opportunity detection
    val (condCtx, nBinds) = if(cond.data is InstanceOfExpr && cond.data.source.data is ValExpr){
        //We can flow type
        val (retType,typeCtx) = cond.data.unifyInstanceAndKeepType(ctx,binds)
        val bind = cond.data.source.data.source.data
        val nBinding = binds + Pair(bind, retType)
        Pair(typeCtx,nBinding)
    } else {
        //We can not flow type
        val condCtx = ctx.expectReturnType(BoolType){cond.unifyExpression(it,binds)}
        Pair(condCtx,binds)
    }
    //restore expectations
    val (tRet,thenCtx) = then.unifyExpression(condCtx,nBinds)
    val thenRet = thenCtx.getType(tRet)
    val (oRet,otherCtx) = other.unifyExpression(thenCtx,binds)
    val otherRet = otherCtx.getType(oRet)
    return if(thenRet != null && otherRet != null){
        //Utilize the opportunity to be better than HM (which does not support subtypes)
        val commonRet = commonSuperType(thenRet, otherRet)
        Pair(commonRet?:thenRet,otherCtx)
    } else {
        val finCtx = otherCtx.unify(otherRet?:oRet, thenRet?:tRet)
        Pair(otherRet?:thenRet?:tRet, finCtx)
    }
}

fun LetExpr<ResolvedState>.unifyExpression(ctx: InferenceContext, binds:Map<Binding,Type>):Pair<Type,InferenceContext> {
    val bindT = (extra[Extra.TYPE_VAR] as TypeValue).typ
    val nCtx = ctx.expectReturnType(bindT){bind.unifyExpression(it,binds)}
    val nBindT = nCtx.getType(bindT)
    val nBinds = binds + Pair(name.data, nBindT?:bindT)
    //Restore return value
    return body.unifyExpression(nCtx, nBinds)
}
