package nora.resolved

import nora.*
import nora.resolved.inferenz.*
import nora.typesys.Type

fun typeDefinition(def: Definition<ResolvedState>): Definition<ResolvedState> = def.accept(TypeInference, Unit)

fun Implemented<ResolvedState>.inferenceBodyType(): Elem<Expr<ResolvedState>> {
    val preppedExp = body.prepareInference(PrepareContext(0)).first
    val argBindings = args.associate { Pair(it.data.name.data as Binding,it.data.type.data as Type) }
    val ctx = InferenceContext(mapOf(), mapOf()).expectReturnType(ret.data as Type){preppedExp.unifyExpression(it,argBindings)}
    return preppedExp.substituteTypeVars(ctx.resolve(), argBindings)
}

//Just for Dispatching
object TypeInference : DefinitionVisitor<ResolvedState, Definition<ResolvedState>, Unit> {
    override fun visitDefinition(definition: Definition<ResolvedState>, ctx: Unit): Definition<ResolvedState> = definition
    override fun visitMethod(method: Method<ResolvedState>, ctx:Unit): Method<ResolvedState> = method.inferenceType()
    override fun visitCaseMethod(caseMethod: CaseMethod<ResolvedState>, ctx:Unit): CaseMethod<ResolvedState> = caseMethod.inferenceType()
    override fun visitObjectMethod(objectMethod: ObjectMethod<ResolvedState>, ctx:Unit):ObjectMethod<ResolvedState> = objectMethod.inferenceType()
}


fun Method<ResolvedState>.inferenceType():Method<ResolvedState>{
    return copy(body = inferenceBodyType())
}

fun CaseMethod<ResolvedState>.inferenceType():CaseMethod<ResolvedState>{
    return copy(body = inferenceBodyType())
}

fun ObjectMethod<ResolvedState>.inferenceType():ObjectMethod<ResolvedState>{
    return copy(body = inferenceBodyType())
}