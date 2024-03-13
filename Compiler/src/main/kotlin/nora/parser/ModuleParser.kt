package nora.parser

import nora.*
import nora.Function
import nora.compiler.ModuleBaseVisitor
import nora.compiler.ModuleLexer
import nora.compiler.ModuleParser
import nora.parsed.ParsedState
import nora.typesys.Variance
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.misc.Interval
import java.nio.file.Path

class ModuleParser(private val moduleFile: Path, private val expectedName: String, private val path:String) : ModuleBaseVisitor<Any?>() {
    val pathPrefixes = mutableMapOf<String,String>()
    val importedDefs = mutableMapOf<String,String>()
    var localBindings = setOf<String>()
    var genericBindings = mapOf<String,Int>()

    //Todo: Init Context
    //      Then have methods to add local bindings
    //      Do not forget to add tail rec info as Extra

    // Todo: have a mutable def map where we collect the definitions
    //       then build a module from it

    companion object {
        private fun parseModule(moduleFile: Path, expectedName: String, path:String):Set<String> {
            val visitor = ModuleParser(moduleFile, expectedName, path)
            try {
                val lexer = ModuleLexer(CharStreams.fromPath(moduleFile))
                val tokens = CommonTokenStream(lexer)
                val parser = ModuleParser(tokens)
                val parsed = parser.file_def()
                return visitor.visitFile_def(parsed)
            } catch (e: Exception) {
                //Todo: Better error Handling
                throw RuntimeException(moduleFile.toString(), e)
            }
        }
    }

    private fun <T> withBindings(binds:Set<String>, f:() -> T):T {
        val oldBinds = localBindings
        localBindings = localBindings + binds
        val res = f()
        localBindings = oldBinds
        return res
    }

    private fun <T> withGenerics(bind: Map<String, Int>, f:() -> T):T {
        val oldBinds = genericBindings //Should be empty but better save
        genericBindings = bind
        val res = f()
        genericBindings = oldBinds
        return res
    }

    override fun visitImport_def(ctx: ModuleParser.Import_defContext): Any? {
        if(ctx.reference() != null){
            //Todo: Shadowing warning
            val name = ctx.text
            importedDefs[ctx.reference().id().text] = name
        } else {
            if(ctx.star != null){
                val path = ctx.text
                when(val defs = moduleDefs(ctx.path().text)){
                    //Todo: Import Warning
                    None -> {}
                    is Some -> {
                        defs.elem.forEach {
                            //Todo: Shadowing warning
                            importedDefs[it] = path
                        }
                    }
                }
            } else {
                val fullPath = ctx.path().id().map { it.text }
                val key = fullPath.last()
                val prefix = fullPath.joinToString(separator = ".")
                //Todo: Shadowing warning
                pathPrefixes[key] = prefix
            }
        }
        return null
    }

    override fun visitModule_def(ctx: ModuleParser.Module_defContext): Any? {
        return if(expectedName != ctx.id().text){
            //Todo: Error
            null
        } else {
            //add our own defs
            when(val defs = moduleDefs(path)){
                //Todo: Import Warning
                None -> null
                is Some -> {
                    defs.elem.forEach {
                        //Todo: Shadowing warning
                        importedDefs[it] = "$path::$it"
                    }
                    //Todo: construct module
                    super.visitModule_def(ctx)
                }
            }

        }
    }

    //Todo: make static in companion
    val idRegex = Regex("::")
    val pathRegex = Regex(".")
    //Anything ending in ::id
    private fun resolvePath(path:String, srcCtx: ParserRuleContext):Elem<Ref>{
        //Todo: Allow _. prefix to mean fully qualified
        val pathIdSplit = path.split(idRegex, 2)
        assert(pathIdSplit.size == 2)
        val defId = pathIdSplit[1]
        val modulePath = pathIdSplit[0]
        val startRestSplit = modulePath.split(pathRegex, 2)
        val prefix = pathPrefixes[startRestSplit[0]]
        val resModPath = if(prefix == null) {
            modulePath //Try it as fully qualified
        } else if(startRestSplit.size == 2){
            val rest = startRestSplit[1]
            "$prefix.$rest"
        } else {
            prefix
        }
        val src = srcCtx.sourceInterval
        return Elem(Ref("$resModPath::$defId"), Src(moduleFile, src.a, src.b))
    }

    //Todo: this shall only be called if not in local bindings
    private fun resolveSymbolPath(name:String, srcCtx: ParserRuleContext):Elem<Binding>{
        //Todo: check if in generic
        val src = srcCtx.sourceInterval
        val genBind = genericBindings[name]
        return if(genBind != null){
            Elem(Gen(name, genBind), Src(moduleFile, src.a, src.b))
        } else {
            val resolved = importedDefs[name]
            return if(resolved == null) {
                Elem(Ref(name), Src(moduleFile, src.a, src.b))
            } else {
                Elem(Ref(resolved), Src(moduleFile, src.a, src.b))
            }
        }
    }

    fun <T> genElem(elem:T, src: ParserRuleContext):Elem<T>{
        val interval = src.sourceInterval
        return Elem(elem, Src(moduleFile, interval.a, interval.b))
    }

    override fun visitValue_ref(ctx: ModuleParser.Value_refContext): Elem<Binding> {
        return if(ctx.reference() != null){
            resolvePath(ctx.reference().text, ctx)
        } else {
            resolveSymbolPath(ctx.id().text, ctx)
        }
    }

    fun visitStrictValue_ref(ctx: ModuleParser.Value_refContext): Elem<Ref> {
        return visitValue_ref(ctx).map {
            if (it is Ref) {
                it
            } else {
                //Todo: Error?
                Ref(it.name)
            }
        }
    }

    override fun visitParametric_value_ref(ctx: ModuleParser.Parametric_value_refContext): Elem<ParametricReference> {
        val applies = if(ctx.type_appl() == null) {
            listOf()
        } else {
            ctx.type_appl().parametric_ref().map { visitParametric_ref(it) }
        }
        return genElem(ParametricReference(visitValue_ref(ctx.value_ref()), applies), ctx)
    }

    override fun visitFunRef(ctx: ModuleParser.FunRefContext): Elem<ParametricReference> {
        return genElem(ParametricReference(
            genElem(FunDef, ctx),
            ctx.parametric_ref().map { visitParametric_ref(it) }),
            ctx
        )
    }

    override fun visitTupleRef(ctx: ModuleParser.TupleRefContext): Elem<ParametricReference> {
        return genElem(ParametricReference(
            genElem(TupleDef, ctx),
            ctx.parametric_ref().map { visitParametric_ref(it) }),
            ctx
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun visitParametric_ref(ctx: ModuleParser.Parametric_refContext): Elem<ParametricReference> {
        return super.visitParametric_ref(ctx) as Elem<ParametricReference>
    }

    //Todo: Resolver returning binding either Local / Generic or Ref

    //TODO: Have the Annotation defs to reference

    override fun visitAnnotation_value(ctx: ModuleParser.Annotation_valueContext): Elem<Attribute> {
        val name = genElem(ctx.id().text, ctx.id())
        val value = if(ctx.parametric_ref() != null) {
            visitParametric_ref(ctx.parametric_ref()).map { RefAttribute(it) }
        } else {
            assert(ctx.lit() != null)
            genElem(TODO("WAIT UNTIL WE HAVE LIT MAYBE WE CAN REUSE"), ctx.lit())
        }
        return genElem(Attribute(name, value), ctx)
    }

    override fun visitAnnotation(ctx: ModuleParser.AnnotationContext): Pair<Elem<Ref>, Elem<List<Elem<Attribute>>>> {
        return Pair(
            visitStrictValue_ref(ctx.value_ref()),
            genElem(ctx.annotation_value().map { visitAnnotation_value(it) },ctx)
        )
    }

    fun visitAnnotations(ctx: List<ModuleParser.AnnotationContext>): List<Pair<Elem<Ref>, Elem<List<Elem<Attribute>>>>> {
        return ctx.map { visitAnnotation(it) }
    }

    override fun visitAccess_modifier(ctx: ModuleParser.Access_modifierContext): Pair<Modifier, Src> {
        val interval = ctx.sourceInterval
        return if(ctx.PUBLIC() != null) {
            Pair(Modifier.PUBLIC, Src(moduleFile, interval.a, interval.b))
        } else {
            assert(ctx.PRIVATE() != null)
            Pair(Modifier.PRIVATE, Src(moduleFile, interval.a, interval.b))
        }
    }

    override fun visitFull_modifier(ctx: ModuleParser.Full_modifierContext): Pair<Modifier, Src> {
        val interval = ctx.sourceInterval
        return if(ctx.SEALED() != null) {
            Pair(Modifier.SEALED, Src(moduleFile, interval.a, interval.b))
        } else {
            assert(ctx.access_modifier() != null)
            visitAccess_modifier(ctx.access_modifier())
        }
    }

    override fun visitGeneric_def(ctx: ModuleParser.Generic_defContext): Elem<Generic<ParsedState>> {
        return genElem(Generic(
            genElem(Gen(ctx.id().text, genericBindings.size),ctx.id()),
            //Todo: Allow to set default variance -- if needed
            genElem(Variance.IN, ctx),
            visitBounds(ctx.bounds()),
            null
        ),ctx)
    }

    override fun visitTraits(ctx: ModuleParser.TraitsContext?): List<Elem<ParametricReference>>{
        return TODO()
    }

    override fun visitVariance(ctx: ModuleParser.VarianceContext): Elem<Variance> {
        val variance = if(ctx.ADD() != null) {
            Variance.CO
        } else if(ctx.SUB() != null) {
            Variance.CONTRA
        } else if (ctx.INV() != null) {
            Variance.IN
        } else {
            //Todo: Variance default by context?
            Variance.IN
        }
        return genElem(variance,ctx)
    }

    override fun visitGeneric_hint_def(ctx: ModuleParser.Generic_hint_defContext): Elem<Generic<ParsedState>> {
        val plainGen = visitGeneric_def(ctx.generic_def())
        return plainGen.map { it.copy(fallback = visitParametric_ref(ctx.parametric_ref())) }
    }

    override fun visitGenerics_hint_def(ctx: ModuleParser.Generics_hint_defContext): Pair<Map<String,Int>,List<Elem<Generic<ParsedState>>>> {
        return ctx.generic_hint_def().fold(Pair(mapOf(), listOf())) { (m,l), vgh ->
            val gen = withGenerics(m) { visitGeneric_hint_def(vgh) }
            Pair(m + Pair(gen.data.name.data.name, m.size), l + gen)
        }
    }

    override fun visitVariant_generic_def(ctx: ModuleParser.Variant_generic_defContext): Elem<Generic<ParsedState>> {
        val plainGen = visitGeneric_def(ctx.generic_def())
        return plainGen.map { it.copy(variance = visitVariance(ctx.variance())) }
    }

    override fun visitGenerics_def(ctx: ModuleParser.Generics_defContext): Pair<Map<String,Int>,List<Elem<Generic<ParsedState>>>> {
        return ctx.variant_generic_def()
            .fold(Pair(mapOf(), listOf())) { (m,l), vgf ->
                val gen = withGenerics(m) { visitVariant_generic_def(vgf) }
                Pair(m + Pair(gen.data.name.data.name, m.size), l + gen)
            }
    }

    override fun visitType_def(ctx: ModuleParser.Type_defContext): Definition<ParsedState> {
        val (binds, generics) = visitGenerics_def(ctx.generics_def())
        return Data(
            visitAnnotations(ctx.annotation()),
            ctx.full_modifier().associate { visitFull_modifier(it) },
            genElem("$path::${ctx.id().text}", ctx.id()),
            mapOf(), //extra
            false, //isConcrete
            generics,
            listOf(), //hidden generics -- we dely those
            listOf(), //types do not have fields only values (this is already new model)
            listOf(), //hidden fields -- we dely those
            if(ctx.value_ref() == null) null else visitStrictValue_ref(ctx.value_ref()),
            withGenerics(binds){visitTraits(ctx.traits())}
        )
    }

    override fun visitData_body(ctx: ModuleParser.Data_bodyContext?): List<Elem<Argument<ParsedState>>> {
        return TODO()
    }

    override fun visitData_def(ctx: ModuleParser.Data_defContext): Definition<ParsedState> {
        val (binds, generics) = visitGenerics_def(ctx.generics_def())
        return Data(
            visitAnnotations(ctx.annotation()),
            ctx.access_modifier().associate { visitAccess_modifier(it) },
            genElem("$path::${ctx.id().text}", ctx.id()),
            mapOf(), //extra
            true, //isConcrete
            generics,
            listOf(), //hidden generics -- we delay those
            if(ctx.data_body() == null) listOf() else withGenerics(binds){visitData_body(ctx.data_body())},
            listOf(), //hidden fields -- we delay those
            if(ctx.value_ref() == null) null else visitStrictValue_ref(ctx.value_ref()),
            withGenerics(binds){visitTraits(ctx.traits())}
        )
    }

    //todo: check sealed in traits / everywhere if nit yet checked
    override fun visitTrait_def(ctx: ModuleParser.Trait_defContext): Definition<ParsedState> {
        val (binds, generics) = visitGenerics_def(ctx.generics_def())
        return Trait(
            visitAnnotations(ctx.annotation()),
            ctx.full_modifier().associate { visitFull_modifier(it) },
            genElem("$path::${ctx.id().text}", ctx.id()),
            mapOf(), //extra
            generics,
            withGenerics(binds){visitTraits(ctx.traits())}
        )
    }

    override fun visitBounds(ctx: ModuleParser.BoundsContext): List<Elem<ParsedState>> {
        return ctx.parametric_ref().map { visitParametric_ref(it) }
    }



    private var locals = 0;
    private fun visitLocal(ctx:  ModuleParser.IdContext): Elem<Loc> {
        return genElem(Loc(ctx.text,locals++), ctx)
    }

    override fun visitArgument_def(ctx: ModuleParser.Argument_defContext): Elem<Argument<ParsedState>> {
        return genElem(Argument(
            visitLocal(ctx.id()),
            visitParametric_ref(ctx.parametric_ref())
        ), ctx)
    }

    override fun visitArguments_def(ctx: ModuleParser.Arguments_defContext): List<Elem<Argument<ParsedState>>> {
        return ctx.argument_def().map { visitArgument_def(it) }
    }

    override fun visitMulti_method_def(ctx: ModuleParser.Multi_method_defContext): Definition<ParsedState> {
        val (binds, generics) = visitGenerics_hint_def(ctx.generics_hint_def())
        return MultiMethod(
            visitAnnotations(ctx.annotation()),
            ctx.full_modifier().associate { visitFull_modifier(it) },
            genElem("$path::${ctx.id().text}", ctx.id()),
            mapOf(), //extra
            generics,
            if(ctx.dynamic == null) listOf() else withGenerics(binds){visitArguments_def(ctx.dynamic)},
            if(ctx.static_ == null) listOf() else withGenerics(binds){visitArguments_def(ctx.static_)},
            withGenerics(binds){visitParametric_ref(ctx.parametric_ref())}
        )
    }

    override fun visitType_appl(ctx: ModuleParser.Type_applContext?): List<Elem<ParsedState>> {
        return TODO()
    }

    override fun visitCase_method_def(ctx: ModuleParser.Case_method_defContext): Definition<ParsedState> {
        val (binds, generics) = visitGenerics_hint_def(ctx.generics_hint_def())
        return CaseMethod(
            visitAnnotations(ctx.annotation()),
            ctx.access_modifier().associate { visitAccess_modifier(it) },
            genElem("$path::${ctx.id().text}", ctx.id()),
            mapOf(), //extra
            generics,
            withGenerics(binds){visitArguments_def(ctx.arguments_def())},
            withGenerics(binds){visitParametric_ref(ctx.parametric_ref())},
            visitStrictValue_ref(ctx.parametric_value_ref().value_ref()),
            if(ctx.parametric_value_ref().type_appl() == null) listOf()
            else withGenerics(binds){visitType_appl(ctx.parametric_value_ref().type_appl())},
            //Todo: Add with arguments
            withGenerics(binds){visitExpr(ctx.expr())}
        )
    }

    override fun visitFunction_def(ctx: ModuleParser.Function_defContext): Definition<ParsedState> {
        val (binds, generics) = visitGenerics_hint_def(ctx.generics_hint_def())
        return Method(
            visitAnnotations(ctx.annotation()),
            ctx.access_modifier().associate { visitAccess_modifier(it) },
            genElem("$path::${ctx.id().text}", ctx.id()),
            mapOf(), //extra
            generics,
            withGenerics(binds){visitArguments_def(ctx.arguments_def())},
            withGenerics(binds){visitParametric_ref(ctx.parametric_ref())},
            //Todo: Add with arguments
            withGenerics(binds){visitExpr(ctx.expr())}
        )
    }

    override fun visitExpr(ctx: ModuleParser.ExprContext?): Elem<Expr<ParsedState>> {
        return TODO()
    }
}