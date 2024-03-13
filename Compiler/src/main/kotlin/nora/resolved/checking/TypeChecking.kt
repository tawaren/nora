package nora.resolved.checking

import nora.*
import nora.Array
import nora.Function
import nora.resolved.ResolvedState
import nora.resolved.getAllFields
import nora.resolved.getField
import nora.resolved.inferenz.*
import nora.typesys.*
import java.math.BigInteger

sealed interface TypeErrors
data class UnknownTypeError(val t:Elem<TypeRelated>) : TypeErrors
data class TypeCheckError(val err:Failure) : TypeErrors
data class TypeCategoryError(val expected:Ref, val got:Elem<Ref>) : TypeErrors
data class NotADataType(val type:Type) : TypeErrors
data class NoSuchField(val typ: Type, val name: Elem<String>): TypeErrors
data class CanNotDeriveCaseGeneric(val gen:Elem<Gen>): TypeErrors

private fun Definition<ResolvedState>.typeCheck(): List<TypeErrors> {
    return accept(TypeChecker, Unit)
}

//Just for Dispatching
object TypeChecker : DefinitionVisitor<ResolvedState,List<TypeErrors>, Unit> {
    override fun visitImplemented(implemented: Implemented<ResolvedState>, ctx: Unit): List<TypeErrors> = implemented.typeCheck()
    override fun visitCallable(callable: Callable<ResolvedState>, ctx: Unit): List<TypeErrors> = callable.typeCheck()
    override fun visitCaseMethod(caseMethod: CaseMethod<ResolvedState>, ctx: Unit): List<TypeErrors> = caseMethod.typeCheck()
    override fun visitObjectMethod(objectMethod: ObjectMethod<ResolvedState>, ctx:Unit):List<TypeErrors> = objectMethod.typeCheck()
    override fun visitData(data: Data<ResolvedState>, ctx:Unit):List<TypeErrors> = data.typeCheck()
    override fun visitTrait(trait: Trait<ResolvedState>, ctx:Unit): List<TypeErrors> = trait.typeCheck()
    override fun visitTuple(tuple: Tuple<ResolvedState>, ctx:Unit):List<TypeErrors> = tuple.typeCheck()
    override fun visitFunction(function: Function<ResolvedState>, ctx:Unit):List<TypeErrors> = function.typeCheck()
    override fun visitArray(array: Array<ResolvedState>, ctx:Unit): List<TypeErrors> = array.typeCheck()
}

//Todo: Check
//  3. No Unknown Types & TypeVar's left -- CHECK

private fun Elem<TypeRelated>.typeCheck(): List<TypeErrors> {
    return when(data){
        is DataType -> data.parameters.flatMap { it.param.typeCheck() }
        is TraitConstraint -> data.parameters.flatMap { it.param.typeCheck() }
        is GenericType -> data.bounds.flatMap { it.typeCheck() }
        else -> listOf(UnknownTypeError(this))
    }
}

private fun Parameterized<ResolvedState>.typeCheck(): List<TypeErrors> {
    return generics.flatMap { it.data.bounds.flatMap { b -> b.typeCheck() }}
}

fun Callable<ResolvedState>.typeCheck(): List<TypeErrors> {
    val nErrors = (this as Parameterized<ResolvedState>).typeCheck()
    return nErrors + args.flatMap { it.data.type.typeCheck() }
}

private fun Type.checkSubTypeOf(other:Type):List<TypeErrors>{
    return when(val checkErr = subTypeOf(other)){
        is Failure -> listOf(TypeCheckError(checkErr))
        Success -> listOf()
    }
}

private fun Type.checkMixedTypeOf(other:Type):List<TypeErrors>{
    return when(val checkErr = subTypeOf(other)){
        is Failure -> when(val checkErr2 = other.subTypeOf(this)){
            is Failure -> listOf(TypeCheckError(checkErr),TypeCheckError(checkErr2))
            Success -> listOf()
        }
        Success -> listOf()
    }
}

private fun Implemented<ResolvedState>.typeCheck(): List<TypeErrors>  {
    val nErrors = (this as Callable<ResolvedState>).typeCheck()
    val ctx = args.associate { Pair(it.data.name.data as Binding, it.data.type.data as Type) }
    val bodyErrors = body.data.typeCheck(ctx)
    val resErrors = body.type().checkSubTypeOf(ret.data as Type)
    return nErrors + bodyErrors + resErrors
}

private fun CaseMethod<ResolvedState>.typeCheck(): List<TypeErrors>  {
    val nErrors = (this as Implemented<ResolvedState>).typeCheck()
    val multi = typedDefinition(parent.data.name).toNullable() as MultiMethod<ResolvedState>
    val subst = parentApplies.map { it.data as Type }.toTypedArray()
    //Todo: make sure structural check ensures arg sizes match
    val resMultiDynArgs = multi.dynamicArgs.map { (it.data.type.data as Type).substitute(subst) }
    val resMultiStatArgs = multi.staticArgs.map { (it.data.type.data as Type).substitute(subst) }
    val resMultiRet = (multi.ret.data as Type).substitute(subst)
    val multiSig = resMultiDynArgs + resMultiStatArgs + resMultiRet
    val sig = args.map { it.data.type.data as Type } + (ret.data as Type)
    //Match it
    val mappingArgs = multiSig.zip(sig).flatMap { (m, c) -> extractApplies(m,c) }
    val gens = checkAndMapApplies(mappingArgs)
    if(gens.size == generics.size){
        val caseSubst = generics.mapNotNull { gens[it.data.name.data] }.toTypedArray()
        assert(caseSubst.size == generics.size)
        val resCaseArgs = args.map { (it.data.type.data as Type).substitute(caseSubst) }
        val resCaseRet = ret.data.substitute(caseSubst)
        //Type check it
        val dynErrors = resMultiDynArgs.zip(resCaseArgs).flatMap { (m,c) -> m.checkMixedTypeOf(c) }
        val statErrors = resMultiDynArgs.zip(resCaseArgs.drop(resMultiDynArgs.size)).flatMap { (m,c) -> m.checkSubTypeOf(c) }
        val retErrors = resCaseRet.checkSubTypeOf(resMultiRet)
        //Constraint checking
        val multiConstraintErrors = multi.checkConstraints(subst)
        val caseConstraintErrors = checkConstraints(caseSubst)
        return nErrors + dynErrors + statErrors + retErrors + multiConstraintErrors + caseConstraintErrors
    } else {
        val deriveErrors = generics.filter { gens.containsKey(it.data.name.data) }.map { CanNotDeriveCaseGeneric(it.data.name) }
        return nErrors + deriveErrors
    }
}

fun ObjectMethod<ResolvedState>.typeCheck():List<TypeErrors> {
    val nErrors = (this as Implemented<ResolvedState>).typeCheck()
    val elemErrors = objectType.typeCheck()
    val hiddenGenErrors = hiddenGenerics.flatMap { it.data.bounds.flatMap { b -> b.typeCheck() } }
    return nErrors + elemErrors + hiddenGenErrors
}

fun Data<ResolvedState>.typeCheck():List<TypeErrors> {
    val nErrors = (this as Parameterized<ResolvedState>).typeCheck()
    val fieldErrors = (fields + hiddenFields).flatMap { it.data.type.typeCheck() }
    val hiddenGenErrors = hiddenGenerics.flatMap { it.data.bounds.flatMap { b -> b.typeCheck() } }
    return nErrors + fieldErrors + hiddenGenErrors + traits.flatMap { it.typeCheck() }
}

fun Trait<ResolvedState>.typeCheck():List<TypeErrors> {
    val nErrors = (this as Parameterized<ResolvedState>).typeCheck()
    return nErrors + traits.flatMap { it.typeCheck() }
}

fun Tuple<ResolvedState>.typeCheck():List<TypeErrors> {
    val nErrors = (this as Parameterized<ResolvedState>).typeCheck()
    return nErrors + traits.flatMap { it.typeCheck() }
}

fun Array<ResolvedState>.typeCheck():List<TypeErrors> {
    val nErrors = (this as Parameterized<ResolvedState>).typeCheck()
    return nErrors + traits.flatMap { it.typeCheck() }
}

fun Function<ResolvedState>.typeCheck():List<TypeErrors> {
    val nErrors = (this as Parameterized<ResolvedState>).typeCheck()
    return nErrors + traits.flatMap { it.typeCheck() }
}

private fun Expr<ResolvedState>.typeCheck(ctx:Map<Binding,Type>):List<TypeErrors> {
    return accept(TypeCheckExpr, ctx)
}

//Just for Dispatching
object TypeCheckExpr : ExprVisitor<ResolvedState, List<TypeErrors>, Map<Binding,Type>> {
    override fun merge(res: List<List<TypeErrors>>): List<TypeErrors> = res.flatten()
    override fun visitTypeHintExpr(expr: TypeHintExpr<ResolvedState>, ctx: Map<Binding, Type>): List<TypeErrors> = expr.typeCheck(ctx)
    override fun visitCallExpr(expr: CallExpr<ResolvedState>, ctx: Map<Binding, Type>): List<TypeErrors> = expr.typeCheck(ctx)
    override fun visitCreateExpr(expr: CreateExpr<ResolvedState>, ctx: Map<Binding, Type>): List<TypeErrors> = expr.typeCheck(ctx)
    override fun visitLitExpr(expr: LitExpr<ResolvedState>, ctx: Map<Binding, Type>): List<TypeErrors> = expr.typeCheck(ctx)
    override fun visitValExpr(expr: ValExpr<ResolvedState>, ctx: Map<Binding, Type>): List<TypeErrors> = expr.typeCheck(ctx)
    override fun visitFunLinkExpr(expr: FunLinkExpr<ResolvedState>, ctx: Map<Binding, Type>): List<TypeErrors> = expr.typeCheck(ctx)
    override fun visitTypeLinkExpr(expr: TypeLinkExpr<ResolvedState>, ctx: Map<Binding, Type>): List<TypeErrors> = expr.typeCheck(ctx)
    override fun visitFieldExpr(expr: FieldExpr<ResolvedState>, ctx: Map<Binding, Type>): List<TypeErrors> = expr.typeCheck(ctx)
    override fun visitFunctionExpr(expr: FunctionExpr<ResolvedState>, ctx: Map<Binding, Type>): List<TypeErrors> = expr.typeCheck(ctx)
    override fun visitIfExpr(expr: IfExpr<ResolvedState>, ctx: Map<Binding, Type>): List<TypeErrors> = expr.typeCheck(ctx)
    override fun visitLetExpr(expr: LetExpr<ResolvedState>, ctx: Map<Binding, Type>): List<TypeErrors> = expr.typeCheck(ctx)
}

fun Parameterized<ResolvedState>.checkConstraints(applies:kotlin.Array<Type>):List<TypeErrors> {
    return generics.zip(applies).flatMap { (g,a) ->
        g.data.bounds.flatMap {
            val checkRes = a.satisfies((it.data as Constraint).substitute(applies))
            if(checkRes is Failure) {
                listOf(TypeCheckError(checkRes))
            } else {
                listOf()
            }
        }
    }
}

fun TypeHintExpr<ResolvedState>.typeCheck(binds:Map<Binding,Type>):List<TypeErrors> {
    val targErrors = target.data.typeCheck(binds)
    val typeErrors = type.data.typeCheck(binds)
    val hintError = target.type().checkSubTypeOf(type.type())
    return targErrors + typeErrors + hintError
}

fun CallExpr<ResolvedState>.typeCheck(binds:Map<Binding,Type>):List<TypeErrors> {
    val targErrors = target.data.typeCheck(binds)
    val argErrs = args.flatMap { it.data.typeCheck(binds)}
    val funT = target.type()
    val matchErrors = if(funT is DataType) {
        if(funT.base.data == FunDef){
            val resT = funT.parameters.last().param.data
            val resError = resT.checkSubTypeOf(type())
            val argMatchErrors = funT.parameters.zip(args).flatMap { (p,a) -> a.type().checkSubTypeOf(p.param.data) }
            resError + argMatchErrors
        } else {
            listOf(TypeCategoryError(FunDef, funT.base))
        }
    } else {
        listOf(NotADataType(funT))
    }
    return targErrors + argErrs + matchErrors
}

fun CreateExpr<ResolvedState>.typeCheck(binds:Map<Binding,Type>):List<TypeErrors> {
    val constructedT = type()
    val targErrors = (target.data.parameters as Provided<ResolvedState>).parameters.flatMap { it.typeCheck()}
    val argErrs = args.flatMap { it.data.typeCheck(binds)}
    val matchErrors = if(constructedT is DataType) {
        when(val def = typedDefinition(constructedT.base.data.name).toNullable()){
            is Data<ResolvedState> -> {
                val substTs = constructedT.parameters.map { it.param }
                val substTErrs = substTs.flatMap { it.typeCheck() }
                val subst = substTs.map { it.data }.toTypedArray()
                val ctrErrs = def.checkConstraints(subst)
                val fieldTs = def.getAllFields(true).map { Elem((it.data.type.data as Type).substitute(subst), it.src) }
                val fieldTErrs = fieldTs.flatMap { it.typeCheck() }
                val argMatchErrors = args.zip(fieldTs).flatMap { (a,t) -> a.type().checkSubTypeOf(t.data) }
                substTErrs + ctrErrs + fieldTErrs + argMatchErrors
            }
            is Tuple<ResolvedState> -> {
                val fieldTs = constructedT.parameters.map { it.param }
                val fieldTErrs = fieldTs.flatMap { it.typeCheck() }
                val argMatchErrors = args.zip(fieldTs).flatMap { (a,t) -> a.type().checkSubTypeOf(t.data) }
                fieldTErrs + argMatchErrors
            }
            is Array<ResolvedState> -> {
                val arrT = constructedT.parameters.first().param
                val arrTErrors = arrT.typeCheck()
                val argMatchErrors = args.flatMap { it.type().checkSubTypeOf(arrT.data) }
                arrTErrors + argMatchErrors
            }
            else -> listOf() //earlier pass checks
        }
    } else {
        listOf(NotADataType(constructedT))
    }
    return targErrors + argErrs + matchErrors
}

fun LitExpr<ResolvedState>.typeCheck(binds:Map<Binding,Type>):List<TypeErrors> {
    return when(value.data){
        is String -> StringType.checkSubTypeOf(type())
        is Boolean -> BoolType.checkSubTypeOf(type())
        is Long -> NumType.checkSubTypeOf(type())
        is BigInteger -> NumType.checkSubTypeOf(type())
        is Int -> {
            val isNum = NumType.checkSubTypeOf(type())
            if(isNum.isEmpty()){
                listOf()
            } else {
                IntType.checkSubTypeOf(type())
            }
        }
        is Byte -> {
            val isNum = NumType.checkSubTypeOf(type())
            if(isNum.isEmpty()){
                listOf()
            } else {
                val isByte = ByteType.checkSubTypeOf(type())
                if(isByte.isEmpty()){
                    listOf()
                } else {
                    IntType.checkSubTypeOf(type())
                }
            }
        }
        else -> listOf(TypeCheckError(Failure(UnknownType, "is not", type())))
    }
}

fun ValExpr<ResolvedState>.typeCheck(binds:Map<Binding,Type>):List<TypeErrors> {
    return binds[source.data]!!.checkSubTypeOf(type())
}

fun AppliedReference<ResolvedState>.buildType():Pair<List<TypeErrors>,Type>{
    val params = (parameters as Provided<ResolvedState>).parameters
    val subst = params.map { it.data as Type }.toTypedArray()

    fun buildDataType(genDefs:List<Elem<Generic<ResolvedState>>>):Type {
        val tArgs = genDefs.zip(params).map { TypeParam(it.first.data.variance.data, it.second.map { p -> p as Type }) }
        return DataType(main.map { it as Ref }, tArgs)
    }

    return when(val def = typedDefinition(main.data.name).toNullable()){
        is Callable<ResolvedState> -> {
            val retT = (def.ret.data as Type).substitute(subst)
            val argTs = def.args.map { (it.data.type.data as Type).substitute(subst) }
            Pair(def.checkConstraints(subst),FunctionType(retT, *argTs.toTypedArray()))
        }
        is VarArgParameterized<ResolvedState> -> Pair(def.checkConstraints(subst),buildDataType(def.genGenerics(params.size)))
        is Parameterized<ResolvedState> -> Pair(def.checkConstraints(subst),buildDataType(def.generics))
        //should never happen as it should be checked by previous passes
        else -> Pair(listOf(),buildDataType(listOf()))
    }

}


fun FunLinkExpr<ResolvedState>.typeCheck(binds:Map<Binding,Type>):List<TypeErrors> {
    val (ctrErrors, funT) = source.data.buildType()
    val funTErrors = Elem(funT, source.src).typeCheck()
    val sigErrors = funT.checkSubTypeOf(type())
    return ctrErrors + funTErrors + sigErrors
}

fun TypeLinkExpr<ResolvedState>.typeCheck(binds:Map<Binding,Type>):List<TypeErrors> {
    val (ctrErrors, dataT) = source.data.buildType()
    val dataTErrors = Elem(dataT, source.src).typeCheck()
    val sigErrors = dataT.checkSubTypeOf(type())
    return ctrErrors + dataTErrors + sigErrors
}

fun FieldExpr<ResolvedState>.typeCheck(binds:Map<Binding,Type>):List<TypeErrors> {
    val targErrors = target.data.typeCheck(binds)
    val targType = target.type()
    val fieldErrors = when(targType){
        is DataType -> when(val def = typedDefinition(targType.base.data.name).toNullable()){
            is Tuple<ResolvedState> -> {
                if(name.data.startsWith("$")){
                    val index = name.data.substring(1).toInt()
                    if(index < targType.parameters.size) {
                        val fType = targType.parameters[index].param.data
                        fType.checkSubTypeOf(type())
                    } else {
                        listOf(NoSuchField(targType, name))
                    }
                } else {
                    listOf(NoSuchField(targType, name))
                }
            }
            is Data<ResolvedState> -> {
                val t = def.getField(name.data) ?: return listOf(NoSuchField(targType, name))
                val subst = targType.parameters.map { it.param.data }.toTypedArray()
                val fType = (t.data.type.data as Type).substitute(subst)
                fType.checkSubTypeOf(type())
            }
            else -> listOf(NoSuchField(targType, name))
        }
        is GenericType -> TODO("We can look fo single data type bound and use that")
        else -> listOf(NotADataType(targType))
    }
    return targErrors + fieldErrors
}

fun FunctionExpr<ResolvedState>.typeCheck(binds:Map<Binding,Type>):List<TypeErrors> {
    val funT = type() as DataType
    val argErrors = args.flatMap { it.data.type?.typeCheck()?:listOf(UnknownTypeError(Elem(UnknownType, it.src))) }

    val argTs = args.map { (it.data.type?.data?:UnknownType) as Type }
    //Todo: do we need error?
    assert(argTs.size == funT.parameters.size -1)
    val argTErrors = funT.parameters.zip(argTs).flatMap { (t,a) -> t.param.data.checkSubTypeOf(a) }
    val nBinds = argTs.zip(args).fold(binds) {b, (t,a) -> b + Pair(a.data.name.data, t)}
    val bodyErrors = body.data.typeCheck(nBinds)
    val retT = funT.parameters.last().param.data
    val bodyTErrors = body.type().checkSubTypeOf(retT)
    return argErrors + argTErrors + bodyErrors + bodyTErrors
}

fun IfExpr<ResolvedState>.typeCheck(binds:Map<Binding,Type>):List<TypeErrors> {
    val condErrors = cond.data.typeCheck(binds)
    val condTErrors = cond.type().checkSubTypeOf(BoolType)
    val thenErrors = then.data.typeCheck(binds)
    val thenTErrors = then.type().checkSubTypeOf(type())
    val otherErrors = other.data.typeCheck(binds)
    val otherTErrors = other.type().checkSubTypeOf(type())
    return condErrors + condTErrors + thenErrors + thenTErrors + otherErrors + otherTErrors
}

fun LetExpr<ResolvedState>.typeCheck(binds:Map<Binding,Type>):List<TypeErrors> {
    val bindErrors = bind.data.typeCheck(binds)
    val nBinds = binds + Pair(name.data, bind.type())
    val inErrors = body.data.typeCheck(nBinds)
    val inTErrors = body.type().checkSubTypeOf(type())
    return bindErrors + inErrors + inTErrors
}

