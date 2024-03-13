package nora.parsed.checking

import nora.*
import nora.Array
import nora.Function
import nora.Annotation
import nora.Modifier.*
import nora.codegen.primitiveAnnotation

sealed interface StructuralErrors
private data class TypeArityMismatch(val ref: Elem<Binding>, val expected:Int):StructuralErrors
private data class ArgArityMismatch(val ref: List<Elem<Expr<RefState>>>, val expected:Int):StructuralErrors
private data class CategoryMismatch(val ref: Elem<Binding>, val targ:ExpectedTarget, val expect:ExpectedTarget):StructuralErrors
private data class NotExtendable(val ref: Elem<Binding>):StructuralErrors
private data class NotVisible(val ref: Elem<Binding>):StructuralErrors
private data class NameMissing(val ref: Elem<String>):StructuralErrors
private data class NoHiddenFieldAllowed(val field: Elem<Argument<RefState>>):StructuralErrors
private data class NoHiddenGenericAllowed(val gen: Elem<Generic<RefState>>):StructuralErrors
private data class ModifierMismatch(val mod: Elem<Modifier>, val msg:String):StructuralErrors

enum class ExpectedTarget {
    DATA, FUNCTION, CONSTRAINT, TRAIT, ANNOTATION
}

data class LookupContext(val module:String, val bindings:Set<Binding>)


//Just for Dispatching
object StructuralChecker : DefinitionVisitor<RefState,List<StructuralErrors>, LookupContext> {
    override fun visitMultiMethod(multiMethod: MultiMethod<RefState>, ctx:LookupContext): List<StructuralErrors> = multiMethod.structuralCheck(ctx)
    override fun visitMethod(method: Method<RefState>, ctx:LookupContext):List<StructuralErrors> = method.structuralCheck(ctx)
    override fun visitCaseMethod(caseMethod: CaseMethod<RefState>, ctx:LookupContext):List<StructuralErrors> = caseMethod.structuralCheck(ctx)
    override fun visitObjectMethod(objectMethod: ObjectMethod<RefState>, ctx:LookupContext):List<StructuralErrors> = objectMethod.structuralCheck(ctx)
    override fun visitData(data: Data<RefState>, ctx:LookupContext):List<StructuralErrors> = data.structuralCheck(ctx)
    override fun visitTrait(trait: Trait<RefState>, ctx: LookupContext): List<StructuralErrors> = trait.structuralCheck(ctx)
    override fun visitTuple(tuple: Tuple<RefState>, ctx:LookupContext):List<StructuralErrors> = tuple.structuralCheck(ctx)
    override fun visitFunction(function: Function<RefState>, ctx:LookupContext):List<StructuralErrors> = function.structuralCheck(ctx)
    override fun visitArray(array: Array<RefState>, ctx:LookupContext): List<StructuralErrors> = array.structuralCheck(ctx)
    override fun visitAnnotation(anot: Annotation<RefState>, ctx: LookupContext)= anot.structuralCheck(ctx)
}

private fun Attribute.structuralCheck(ctx:LookupContext, def: Definition<RefState>):List<StructuralErrors> {
    return listOf()
}

private fun Modifier.structuralCheck(ctx:LookupContext, def: Definition<RefState>):List<StructuralErrors> {
    return when(this){
        PUBLIC ->listOf()
        PRIVATE -> {
            val mod = def.modifier[PUBLIC]
            if(mod != null) listOf(ModifierMismatch(Elem(PUBLIC,mod), "public definition can not be private"))
            else listOf()
        }
    }
}

//Todo: Are these needed Extras usually are put in later
private fun Extra.structuralCheck(ctx:LookupContext, def: Definition<RefState>):List<StructuralErrors> {
    return listOf()
}

private fun Extra.structuralCheck(ctx:LookupContext, exp: Expr<RefState>):List<StructuralErrors> {
    return listOf()
}

private fun Elem<Binding>.isVisible(def:Definition<RefState>, ctx: LookupContext):List<StructuralErrors> {
    if(def.modifier.containsKey(PRIVATE)){
        val refModule = this.data.name.split("::")[0]
        if(refModule != ctx.module) return listOf(NotVisible(this))
    }
    return listOf()
}

private fun Elem<Binding>.checkExpected(def:Definition<RefState>, expectedTarget: ExpectedTarget):List<StructuralErrors> {
    if(def is Callable<RefState>) {
        if(expectedTarget != ExpectedTarget.FUNCTION) return listOf(CategoryMismatch(this, expectedTarget, ExpectedTarget.FUNCTION))
        return listOf()
    }
    if(def is Trait<RefState>) {
        if(expectedTarget != ExpectedTarget.CONSTRAINT && expectedTarget != ExpectedTarget.TRAIT) {
            return listOf(CategoryMismatch(this, expectedTarget, ExpectedTarget.TRAIT))
        }
        return listOf()
    }
    if(def is Annotation<RefState>){
        if(expectedTarget == ExpectedTarget.ANNOTATION) return listOf(CategoryMismatch(this, expectedTarget, ExpectedTarget.ANNOTATION))
        return listOf()
    }
    //Can be both Data & Callable
    if(def is Function<RefState>){
        if(expectedTarget == ExpectedTarget.CONSTRAINT) return listOf(CategoryMismatch(this, expectedTarget, ExpectedTarget.DATA))
        return listOf()
    }
    if(expectedTarget == ExpectedTarget.FUNCTION) return listOf(CategoryMismatch(this, expectedTarget, ExpectedTarget.DATA))
    return listOf()
}

private fun Applies<RefState>.structuralCheck(def:Parameterized<RefState>, ctx:LookupContext):List<StructuralErrors>{
    return when(this){
        is Hinted<RefState> -> {
            val gens = def.generics.map { it.data.name.data.name }.toSet()
            val hintMissError = hints.flatMap {
                if(gens.contains(it.name.data)) listOf()
                else listOf(NameMissing(it.name))
            }
            hintMissError + hints.flatMap{ it.value.data.structuralCheck(ctx, ExpectedTarget.DATA)}
        }
        is Partial<RefState> -> parameters.flatMap{ it.data?.structuralCheck(ctx, ExpectedTarget.DATA) ?: listOf() }
        is Provided<RefState> -> parameters.flatMap{ it.data.structuralCheck(ctx, ExpectedTarget.DATA)}
    }
}

private fun Applies<RefState>.notEmpty():Boolean{
   return when(this){
        is Hinted<RefState> -> hints.isNotEmpty()
        is Partial<RefState> -> parameters.isNotEmpty()
        is Provided<RefState> -> parameters.isNotEmpty()
    }
}

private fun Applies<RefState>.isFixedSize():Boolean{
    return when(this){
        is Hinted<RefState> -> false
        is Partial<RefState> -> true
        is Provided<RefState> -> true
    }
}

//returns minSize in case of hints
private fun Applies<RefState>.size():Int{
    return when(this){
        is Hinted<RefState> -> hints.size
        is Partial<RefState> -> parameters.size
        is Provided<RefState> -> parameters.size
    }
}

private fun Applies<RefState>.needsInference(def:Parameterized<RefState>):Boolean{
    return when(this){
        is Hinted<RefState> -> {
            if(def is VarArgParameterized) {
                //The Vararg params are unnamed thus the hints are not sufficient
                true
            } else {
                val targNames = def.generics.map { it.data.name.data }.toSet()
                val hintNames = hints.map { it.name.data }.toSet()
                targNames != hintNames || hintNames.size != hints.size
            }
        }
        is Partial<RefState> -> parameters.any { it.data == null }
        is Provided<RefState> -> false
    }
}

private fun RefState.structuralCheck(ctx:LookupContext, expectedTarget: ExpectedTarget):List<StructuralErrors> {
    if(ctx.bindings.contains(main.data)){
        if(expectedTarget != ExpectedTarget.DATA) return listOf(CategoryMismatch(main, ExpectedTarget.DATA, expectedTarget))
        if(parameters.isNotEmpty()) return listOf(TypeArityMismatch(main, 0))
        return listOf()
    } else {
        val def = parsedDefinition(main.data.name).toNullable()!!
        val visError = main.isVisible(def, ctx)
        val nErrors = visError + main.checkExpected(def, expectedTarget)
        if(def !is Parameterized) return nErrors + TypeArityMismatch(main, 0)
        val allErrors = nErrors + parameters.flatMap { it.data.structuralCheck(ctx, ExpectedTarget.DATA) }
        if(def is VarArgParameterized){
            if(def.minVarArgs() <= parameters.size){
                return allErrors + TypeArityMismatch(main, def.minVarArgs())
            }
        } else {
            if(def.generics.size != parameters.size) {
                return allErrors + TypeArityMismatch(main, def.generics.size)
            }
        }
        return allErrors
    }
}

private fun Elem<Binding>.structuralCheck(ctx:LookupContext, expectedTarget: ExpectedTarget):List<StructuralErrors> {
    if(ctx.bindings.contains(data)){
        if(expectedTarget != ExpectedTarget.DATA) return listOf(CategoryMismatch(this, ExpectedTarget.DATA, expectedTarget))
        return listOf()
    } else {
        val def = parsedDefinition(data.name).toNullable()!!
        val visError = isVisible(def, ctx)
        val nErrors = visError + checkExpected(def, expectedTarget)
        if(def !is Parameterized) return nErrors + TypeArityMismatch(this, 0)
        return nErrors
    }
}

private fun Generic<RefState>.structuralCheck(ctx:LookupContext):List<StructuralErrors> {
    val fallBackErrors = fallback?.data?.structuralCheck(ctx, ExpectedTarget.DATA)?: listOf()
    return fallBackErrors + bounds.flatMap { it.data.structuralCheck(ctx, ExpectedTarget.CONSTRAINT) }
}


private fun List<Elem<Generic<RefState>>>.checkAndBindGenerics(ctx:LookupContext):Pair<LookupContext,List<StructuralErrors>> {
    return fold(Pair(ctx, listOf())){ (s,e), g -> Pair(
        s.copy(bindings = s.bindings + g.data.name.data),
        e + g.data.structuralCheck(s)
    )}
}

private fun Definition<RefState>.structuralCheck(ctx:LookupContext):List<StructuralErrors> {
    var errors = annotations.fold(listOf<StructuralErrors>()){ e, a ->
        e + a.first.structuralCheck(ctx, ExpectedTarget.ANNOTATION) +
                a.second.data.flatMap { it.data.structuralCheck(ctx,this)}
    }
    errors = modifier.keys.fold(errors){e,a -> e + a.structuralCheck(ctx,this)}
    errors = extra.keys.fold(errors){e,a -> e + a.structuralCheck(ctx,this)}
    return errors;
}

private fun Parameterized<RefState>.structuralCheck(ctx: LookupContext): List<StructuralErrors> {
    val errors = (this as Definition<RefState>).structuralCheck(ctx)
    val (_,genErrors) = generics.checkAndBindGenerics(ctx)
    return errors + genErrors
}

private fun Callable<RefState>.structuralCheck(ctx: LookupContext): List<StructuralErrors> {
    val errors = (this as Parameterized<RefState>).structuralCheck(ctx)
    val ctxWithBindings = ctx.copy(bindings = generics.map { it.data.name.data }.toSet())
    val argErrors = args.flatMap { it.data.type.data.structuralCheck(ctxWithBindings, ExpectedTarget.DATA) }
    val retErrors = ret.data.structuralCheck(ctxWithBindings, ExpectedTarget.DATA)
    return errors + argErrors + retErrors;
}

private fun Implemented<RefState>.structuralCheck(ctx: LookupContext): List<StructuralErrors>  {
    val errors = (this as Callable<RefState>).structuralCheck(ctx)
    val ctxWithBindings = ctx.copy(
        //Note: No args bound as not needed in body (no check goes on value level)
        bindings = (generics.map { it.data.name.data }).toSet()
    )
    return errors + body.data.structuralCheck(ctxWithBindings)
}

fun MultiMethod<RefState>.structuralCheck(ctx:LookupContext):List<StructuralErrors> {
    return (this as Callable<RefState>).structuralCheck(ctx)
}


fun Method<RefState>.structuralCheck(ctx:LookupContext):List<StructuralErrors> {
    return (this as Implemented<RefState>).structuralCheck(ctx)
}

fun CaseMethod<RefState>.structuralCheck(ctx:LookupContext):List<StructuralErrors> {
    val errors = (this as Implemented<RefState>).structuralCheck(ctx)
    val pApplyErrors = parentApplies.flatMap { it.data.structuralCheck(ctx, ExpectedTarget.DATA ) }
    return errors + pApplyErrors + parent.structuralCheck(ctx, ExpectedTarget.DATA)
}

fun ObjectMethod<RefState>.structuralCheck(ctx:LookupContext):List<StructuralErrors> {
    val errors = (this as Callable<RefState>).structuralCheck(ctx)
    //Parser should put the hidden ones under hidden generics
    // object method[Generics] MyTrait[ObjectType.Params|HiddenGenerics]#test .....
    val objErrors = objectType.data.structuralCheck(ctx, ExpectedTarget.DATA)
    val ctxWithGenericBindings = ctx.copy(bindings = generics.map { it.data.name.data }.toSet())
    val (ctxWithBindings,genErrors) = hiddenGenerics.checkAndBindGenerics(ctxWithGenericBindings)
    return errors + objErrors + genErrors + body.data.structuralCheck(ctxWithBindings)
}


fun Elem<Binding>?.parentErrors(ctx:LookupContext):List<StructuralErrors>{
    if(this == null){
        return listOf()
    } else {
        val parError = this.structuralCheck(ctx, ExpectedTarget.DATA)
        val parDef = parsedDefinition(data.name).toNullable()!!
        if(parDef is Data && parDef.isConcrete) {
            return parError + NotExtendable(this)
        }
        return parError
    }
}

fun Data<RefState>.structuralCheck(ctx:LookupContext):List<StructuralErrors> {
    val errors = (this as Parameterized<RefState>).structuralCheck(ctx)
    val traitErrors = traits.flatMap { it.data.structuralCheck(ctx, ExpectedTarget.TRAIT) }
    val ctxWithGenerics = ctx.copy(bindings = generics.map { it.data.name.data }.toSet())
    val fieldErrors = fields.flatMap { it.data.type.data.structuralCheck(ctxWithGenerics, ExpectedTarget.DATA) }
    val (ctxWithAllBindings, genErrors) = hiddenGenerics.checkAndBindGenerics(ctxWithGenerics)
    val hiddenFieldErrors = hiddenFields.flatMap { it.data.type.data.structuralCheck(ctxWithAllBindings, ExpectedTarget.DATA) }

    val concreteErrors = if(!isConcrete) {
        hiddenFields.map { NoHiddenFieldAllowed(it) } + hiddenGenerics.map { NoHiddenGenericAllowed(it) }
    } else {
        listOf()
    }

    val primitiveError = if(annotations.any { it.first.data.name == primitiveAnnotation }){
        TODO("Check only primitive")
    } else {
        listOf<StructuralErrors>()
    }

    return errors + traitErrors + fieldErrors + genErrors + hiddenFieldErrors + concreteErrors + primitiveError +
            parent.parentErrors(ctxWithGenerics)
}

fun Trait<RefState>.structuralCheck(ctx:LookupContext):List<StructuralErrors> {
    val errors = (this as Definition<RefState>).structuralCheck(ctx)
    return errors + traits.flatMap { it.data.structuralCheck(ctx, ExpectedTarget.TRAIT) }
}

fun Tuple<RefState>.structuralCheck(ctx:LookupContext):List<StructuralErrors> {
    val errors = (this as Definition<RefState>).structuralCheck(ctx)
    return errors + traits.flatMap { it.data.structuralCheck(ctx, ExpectedTarget.TRAIT) }
}

fun Array<RefState>.structuralCheck(ctx:LookupContext):List<StructuralErrors> {
    val errors = (this as Parameterized<RefState>).structuralCheck(ctx)
    val traitErrors = traits.flatMap { it.data.structuralCheck(ctx, ExpectedTarget.TRAIT) }
    return errors + traitErrors + parent.parentErrors(ctx)
}

fun Function<RefState>.structuralCheck(ctx:LookupContext):List<StructuralErrors> {
    val errors = (this as Definition<RefState>).structuralCheck(ctx)
    return errors + traits.flatMap { it.data.structuralCheck(ctx, ExpectedTarget.TRAIT) }
}

fun Annotation<RefState>.structuralCheck(ctx:LookupContext):List<StructuralErrors> {
    return (this as Definition<RefState>).structuralCheck(ctx)
}

private fun Expr<RefState>.structuralCheck(ctx:LookupContext):List<StructuralErrors> {
   return accept(StructuralExpChecker, ctx)
}

fun Expr<RefState>.checkExtra(ctx:LookupContext):List<StructuralErrors>{
    return extra.keys.fold(listOf()){ e, a -> e + a.structuralCheck(ctx,this)}
}

//Todo: Check PrimitiveExp is top level
//Just for Dispatching
object StructuralExpChecker : ExprVisitor<RefState, List<StructuralErrors>, LookupContext> {
    override fun merge(res: List<List<StructuralErrors>>): List<StructuralErrors> = res.flatten()
    override fun visitExpr(expr: Expr<RefState>, ctx: LookupContext): List<StructuralErrors> = expr.checkExtra(ctx) + super.visitExpr(expr, ctx)
    override fun visitCreateExpr(expr: CreateExpr<RefState>, ctx: LookupContext): List<StructuralErrors>  = expr.structuralCheck(ctx)
    override fun visitFunLinkExpr(expr: FunLinkExpr<RefState>, ctx: LookupContext): List<StructuralErrors>  = expr.structuralCheck(ctx)
    override fun visitTypeLinkExpr(expr: TypeLinkExpr<RefState>, ctx: LookupContext): List<StructuralErrors>  = expr.structuralCheck(ctx)
    override fun visitFunctionExpr(expr: FunctionExpr<RefState>, ctx: LookupContext): List<StructuralErrors>  = expr.structuralCheck(ctx)
}


private fun Elem<AppliedReference<RefState>>.structuralApplyCheck(ctx:LookupContext, expectedTarget: ExpectedTarget):List<StructuralErrors> {
    if(ctx.bindings.contains(data.main.data)){
        if(data.parameters.notEmpty()) return listOf(TypeArityMismatch(data.main, 0))
        return listOf()
    } else {
        val def = parsedDefinition(data.main.data.name).toNullable()!!
        val visError = data.main.isVisible(def, ctx)
        val nErrors = visError + data.main.checkExpected(def, expectedTarget)
        if(def !is Parameterized) return nErrors + TypeArityMismatch(data.main, 0)
        val allErrors = nErrors + data.parameters.structuralCheck(def, ctx)

        if(!data.parameters.isFixedSize()) return allErrors
        if(def.totalArgs() != data.parameters.size()) {
            return allErrors + TypeArityMismatch(data.main, def.generics.size)
        }
        return allErrors
    }
}

private fun Elem<AppliedReference<RefState>>.structuralApplyCheck(ctx:LookupContext):List<StructuralErrors> {
    return structuralApplyCheck(ctx, ExpectedTarget.DATA)
}

//Note: Call needs same but we can not check yet without types as it could be a function param
fun CreateExpr<RefState>.structuralCheck(ctx:LookupContext):List<StructuralErrors> {
    val errors = StructuralExpChecker.visitExpr(this, ctx);
    val targErrors = target.structuralApplyCheck(ctx)
    val targ = parsedDefinition(target.data.main.data.name).toNullable()!!
    if(targ is Data){
        if(targ.totalArgs() != args.size){
            return errors + targErrors + ArgArityMismatch(args, targ.totalArgs())
        }
    }
    return errors + targErrors
}

fun FunLinkExpr<RefState>.structuralCheck(ctx:LookupContext):List<StructuralErrors> {
    val errors = StructuralExpChecker.visitExpr(this, ctx)
    return errors + source.structuralApplyCheck(ctx, ExpectedTarget.FUNCTION)
}

fun TypeLinkExpr<RefState>.structuralCheck(ctx:LookupContext):List<StructuralErrors> {
    val errors = StructuralExpChecker.visitExpr(this, ctx)
    return errors + source.structuralApplyCheck(ctx)
}

fun FunctionExpr<RefState>.structuralCheck(ctx:LookupContext):List<StructuralErrors> {
    val errors = StructuralExpChecker.visitExpr(this, ctx);
    return errors + args.flatMap { it.data.type?.data?.structuralCheck(ctx, ExpectedTarget.DATA)?: listOf() }
}

