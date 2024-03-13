package nora.parsed

import nora.*
import nora.resolved.ResolvedState
import nora.typesys.*

fun resolveDefinition(def:Definition<ParsedState>):Definition<ResolvedState> = def.accept(DefinitionResolverVisitor, Unit);

//Just for Dispatching
object DefinitionResolverVisitor : DefinitionVisitor<ParsedState, Definition<ResolvedState>, Unit> {
    override fun visitMultiMethod(multiMethod: MultiMethod<ParsedState>, ctx:Unit) = multiMethod.resolve()
    override fun visitMethod(method: Method<ParsedState>, ctx:Unit) = method.resolve()
    override fun visitCaseMethod(caseMethod: CaseMethod<ParsedState>, ctx:Unit) = caseMethod.resolve()
    override fun visitObjectMethod(objectMethod: ObjectMethod<ParsedState>, ctx:Unit) = objectMethod.resolve()
    override fun visitData(data: Data<ParsedState>, ctx:Unit) = data.resolve()
    override fun visitTuple(tuple: Tuple<ParsedState>, ctx:Unit) = tuple.resolve()
    override fun visitFunction(function: nora.Function<ParsedState>, ctx:Unit) = function.resolve()
    override fun visitArray(array: nora.Array<ParsedState>, ctx:Unit) = array.resolve()
    override fun visitTrait(trait: Trait<ParsedState>, ctx:Unit) = trait.resolve()
}

fun ParsedState.resolveType(gens:Map<Gen, GenericType>):Type {
    return when(val bind = main.data){
        is Gen -> {
            val gen = gens[main.data]
            if(gen != null){
                assert(parameters.isEmpty())
                gen
            } else {
                UnknownType
            }
        }
        is Ref -> {
            val name = Elem(bind, main.src)
            when(val def = parsedDefinition(main.data.name)){
                None -> UnknownType
                is Some -> {
                    when(def.elem){
                        is VarArgParameterized<ParsedState> -> DataType(
                            name,
                            def.elem.genGenerics(parameters.size).map { it.data.variance.data }.zip(parameters)
                                .map { (v, p) -> TypeParam(v, p.map { d -> d.resolveType(gens)}) }
                        )
                        is Parameterized<ParsedState> -> {
                            assert(def.elem.generics.size == parameters.size)
                            DataType(
                                name,
                                def.elem.generics.map { it.data.variance.data }.zip(parameters)
                                    .map { (v, p) -> TypeParam(v, p.map { d -> d.resolveType(gens)}) }
                            )
                        }
                        else -> UnknownType
                    }
                }
            }
        }
        else -> UnknownType
    }
}

fun ParsedState.resolveConstraint(gens:Map<Gen, GenericType>):Constraint? {
    return when(val bind = main.data) {
        is Ref -> {
            val name = Elem(bind, main.src)
            when (val def = parsedDefinition(name.data.name).toNullable()!!) {
                is Trait<ParsedState> -> TraitConstraint(
                    name,
                    def.generics.map { it.data.variance.data }.zip(parameters)
                        .map { (v, p) -> TypeParam(v, p.map { d -> d.resolveType(gens) }) }
                )

                is VarArgParameterized<ParsedState> -> DataType(
                    name,
                    def.genGenerics(parameters.size).map { it.data.variance.data }.zip(parameters)
                        .map { (v, p) -> TypeParam(v, p.map { d -> d.resolveType(gens) }) }
                )

                is Parameterized<ParsedState> -> {
                    assert(def.generics.size == parameters.size)
                    DataType(
                        name,
                        def.generics.map { it.data.variance.data }.zip(parameters)
                            .map { (v, p) -> TypeParam(v, p.map { d -> d.resolveType(gens) }) }
                    )
                }
                else -> null
            }
        }
        else -> null
    }
}

fun Generic<ParsedState>.resolve(gens:Map<Gen, GenericType>):Generic<ResolvedState> {
    return Generic(name, variance,
        bounds.map { it.map { d -> d.resolveConstraint(gens)!! }},
        fallback?.map { it.resolveType(gens) }
    )
}

fun Argument<ParsedState>.resolve(gens:Map<Gen, GenericType>):Argument<ResolvedState> {
    return Argument(name, type.map { it.resolveType(gens) })
}

fun resolveGenerics(
    generics:List<Elem<Generic<ParsedState>>>,
    prev:Map<Gen, GenericType>
):Pair<Map<Gen, GenericType>,List<Elem<Generic<ResolvedState>>>> {
    return generics.fold(Pair(prev, listOf())) { (m,l), g ->
        val nGen = g.map { it.resolve(m)  }
        val gType = GenericType(nGen.data.name, nGen.data.bounds.map { it.map { c -> c as Constraint } })
        Pair(m + Pair(nGen.data.name.data, gType), l + nGen)
    }
}

fun MultiMethod<ParsedState>.resolve(): MultiMethod<ResolvedState> {
    val (bindings,generics) = resolveGenerics(generics, mapOf())
    return MultiMethod(
        annotations,
        modifier,
        name,
        extra,
        generics,
        dynamicArgs.map { it.map { d -> d.resolve(bindings) } },
        staticArgs.map { it.map { d -> d.resolve(bindings) } },
        ret.map { it.resolveType(bindings) }
    )
}

fun Method<ParsedState>.resolve(): Method<ResolvedState> {
    val (bindings,generics) = resolveGenerics(generics, mapOf())
    return Method(
        annotations,
        modifier,
        name,
        extra,
        generics,
        args.map { it.map { d -> d.resolve(bindings) } },
        ret.map { it.resolveType(bindings) },
        body.map { it.resolve(bindings) }
    )
}

fun CaseMethod<ParsedState>.resolve(): CaseMethod<ResolvedState> {
    val (bindings,generics) = resolveGenerics(generics, mapOf())
    return CaseMethod(
        annotations,
        modifier,
        name,
        extra,
        generics,
        args.map { it.map { d -> d.resolve(bindings) } },
        ret.map { it.resolveType(bindings) },
        parent,
        parentApplies.map { it.map {t -> t.resolveType(bindings) } },
        body.map { it.resolve(bindings) }
    )
}

fun ObjectMethod<ParsedState>.resolve(): ObjectMethod<ResolvedState> {
    //Todo: Not sure the Hidden needs resolving!!
    val (bindings,generics) = resolveGenerics(generics, mapOf())
    val (fullBindings,hiddenGenerics) = resolveGenerics(hiddenGenerics, bindings)
    return ObjectMethod(
        annotations,
        modifier,
        name,
        extra,
        generics,
        objectType.map { it.resolveType(bindings) },
        hiddenGenerics,
        args.map { it.map { d -> d.resolve(bindings) } },
        ret.map { it.resolveType(bindings) },
        body.map { it.resolve(fullBindings) }
    )
}

fun Data<ParsedState>.resolve(): Data<ResolvedState> {
    val (bindings,generics) = resolveGenerics(generics, mapOf())
    val (fullBindings,hiddenGenerics) = resolveGenerics(hiddenGenerics, bindings)
    return Data(
        annotations,
        modifier,
        name,
        extra,
        isConcrete,
        generics,
        hiddenGenerics,
        fields.map { it.map { d -> d.resolve(bindings) }},
        hiddenFields.map { it.map { d -> d.resolve(fullBindings) }},
        parent,
        traits.map { it.map { d -> d.resolveConstraint(bindings)!! }}
    )
}

fun Tuple<ParsedState>.resolve(): Tuple<ResolvedState> {
    return Tuple(
        annotations,
        modifier,
        name,
        extra,
        traits.map { it.map { d -> d.resolveConstraint(mapOf())!! }}
    )
}

fun nora.Function<ParsedState>.resolve(): nora.Function<ResolvedState> {
    return nora.Function(
        annotations,
        modifier,
        name,
        extra,
        traits.map { it.map { d -> d.resolveConstraint(mapOf())!! }}
    )
}

fun nora.Array<ParsedState>.resolve(): nora.Array<ResolvedState> {
    val generic = elemType.map { it.resolve(mapOf()) }
    val gType = GenericType(generic.data.name, generic.data.bounds.map { it.map { c -> c as Constraint } })
    return nora.Array(
        annotations,
        modifier,
        name,
        extra,
        generic,
        parent,
        traits.map { it.map { d -> d.resolveConstraint(mapOf(Pair(generic.data.name.data, gType)))!! }}
    )
}

fun Trait<ParsedState>.resolve(): Trait<ResolvedState> {
    val (bindings,generics) = resolveGenerics(generics, mapOf())
    return Trait(
        annotations,
        modifier,
        name,
        extra,
        generics,
        traits.map { it.map { d -> d.resolveConstraint(bindings)!! }}
    )
}

fun Expr<ParsedState>.resolve(gens:Map<Gen, GenericType>): Expr<ResolvedState> {
    return accept(ExprResolverVisitor, gens)
}

//Just for Dispatching
object ExprResolverVisitor: ExprVisitor<ParsedState,Expr<ResolvedState>, Map<Gen, GenericType>> {
    override fun visitInstanceOfExpr(expr: InstanceOfExpr<ParsedState>, ctx: Map<Gen, GenericType>) = expr.resolve(ctx)
    override fun visitTypeHintExpr(expr: TypeHintExpr<ParsedState>, ctx: Map<Gen, GenericType>) = expr.resolve(ctx)
    override fun visitCallExpr(expr: CallExpr<ParsedState>, ctx: Map<Gen, GenericType>) = expr.resolve(ctx)
    override fun visitPrimitiveExpr(expr: PrimitiveExpr<ParsedState>, ctx: Map<Gen, GenericType>) = expr.resolve(ctx)
    override fun visitCreateExpr(expr: CreateExpr<ParsedState>, ctx: Map<Gen, GenericType>) = expr.resolve(ctx)
    override fun visitLitExpr(expr: LitExpr<ParsedState>, ctx: Map<Gen, GenericType>) = expr.resolve(ctx)
    override fun visitValExpr(expr: ValExpr<ParsedState>, ctx: Map<Gen, GenericType>) = expr.resolve(ctx)
    override fun visitFunLinkExpr(expr: FunLinkExpr<ParsedState>, ctx: Map<Gen, GenericType>) = expr.resolve(ctx)
    override fun visitTypeLinkExpr(expr: TypeLinkExpr<ParsedState>, ctx: Map<Gen, GenericType>) = expr.resolve(ctx)
    override fun visitFieldExpr(expr: FieldExpr<ParsedState>, ctx: Map<Gen, GenericType>) = expr.resolve(ctx)
    override fun visitFunctionExpr(expr: FunctionExpr<ParsedState>, ctx: Map<Gen, GenericType>) = expr.resolve(ctx)
    override fun visitIfExpr(expr: IfExpr<ParsedState>, ctx: Map<Gen, GenericType>) = expr.resolve(ctx)
    override fun visitLetExpr(expr: LetExpr<ParsedState>, ctx: Map<Gen, GenericType>) = expr.resolve(ctx)
}

fun Elem<Expr<ParsedState>>.resolve(ctx:Map<Gen, GenericType>): Elem<Expr<ResolvedState>> {
    return map { it.resolve(ctx)}
}

fun Applies<ParsedState>.resolve(ctx:Map<Gen, GenericType>): Applies<ResolvedState> {
    return when(this){
        is Hinted -> Hinted(hints.map { Hint(it.name, it.value.map { d -> d.resolveType(ctx) }) })
        is Partial -> Partial(parameters.map { it.map { d -> d?.resolveType(ctx) } })
        is Provided -> Provided(parameters.map { it.map { d -> d.resolveType(ctx) } })
    }
}

fun AppliedReference<ParsedState>.resolve(ctx:Map<Gen, GenericType>): AppliedReference<ResolvedState> {
    return AppliedReference(
        main,
        parameters.resolve(ctx)
    )
}

fun ClosureArgument<ParsedState>.resolve(ctx:Map<Gen, GenericType>): ClosureArgument<ResolvedState> {
    val resT = type?.data?.resolveType(ctx)
    return ClosureArgument(
        name,
        if(resT == null) null else Elem(resT, type!!.src)
    )
}

fun InstanceOfExpr<ParsedState>.resolve(ctx: Map<Gen, GenericType>):InstanceOfExpr<ResolvedState> {
    return InstanceOfExpr(
        extra,
        source.resolve(ctx),
        type.resolve(ctx)
    )
}

fun TypeHintExpr<ParsedState>.resolve(ctx: Map<Gen, GenericType>):TypeHintExpr<ResolvedState> {
    return TypeHintExpr(
        extra,
        target.resolve(ctx),
        type.resolve(ctx)
    )
}

fun CallExpr<ParsedState>.resolve(ctx: Map<Gen, GenericType>):CallExpr<ResolvedState> {
    return CallExpr(
        extra,
        target.resolve(ctx),
        args.map { it.resolve(ctx) }
    )
}

fun PrimitiveExpr<ParsedState>.resolve(ctx: Map<Gen, GenericType>):PrimitiveExpr<ResolvedState> {
    return PrimitiveExpr(
        extra,
        target
    )
}

fun CreateExpr<ParsedState>.resolve(ctx: Map<Gen, GenericType>):CreateExpr<ResolvedState> {
    return CreateExpr(
        extra,
        target.map { it.resolve(ctx) },
        args.map { it.resolve(ctx) }
    )
}

fun LitExpr<ParsedState>.resolve(ctx: Map<Gen, GenericType>):LitExpr<ResolvedState> {
    @Suppress("UNCHECKED_CAST")
    return this as LitExpr<ResolvedState>
}

fun ValExpr<ParsedState>.resolve(ctx: Map<Gen, GenericType>):ValExpr<ResolvedState> {
    @Suppress("UNCHECKED_CAST")
    return this as ValExpr<ResolvedState>
}

fun FunLinkExpr<ParsedState>.resolve(ctx: Map<Gen, GenericType>):FunLinkExpr<ResolvedState> {
    return FunLinkExpr(
        extra,
        source.map { it.resolve(ctx) }
    )
}

fun TypeLinkExpr<ParsedState>.resolve(ctx: Map<Gen, GenericType>):TypeLinkExpr<ResolvedState> {
    return TypeLinkExpr(
        extra,
        source.map { it.resolve(ctx) }
    )
}

fun FieldExpr<ParsedState>.resolve(ctx: Map<Gen, GenericType>):FieldExpr<ResolvedState> {
    return FieldExpr(
        extra,
        target.resolve(ctx),
        name
    )
}

fun FunctionExpr<ParsedState>.resolve(ctx: Map<Gen, GenericType>):FunctionExpr<ResolvedState> {
    return FunctionExpr(
        extra,
        args.map { it.map { d -> d.resolve(ctx)}},
        body.resolve(ctx)
    )
}

fun IfExpr<ParsedState>.resolve(ctx: Map<Gen, GenericType>):IfExpr<ResolvedState> {
    return IfExpr(
        extra,
        cond.resolve(ctx),
        then.resolve(ctx),
        other.resolve(ctx)
    )
}

fun LetExpr<ParsedState>.resolve(ctx: Map<Gen, GenericType>):LetExpr<ResolvedState> {
    return LetExpr(
        extra,
        name,
        bind.resolve(ctx),
        body.resolve(ctx)
    )
}