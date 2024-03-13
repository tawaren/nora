package nora.parser

import nora.compiler.ModuleBaseVisitor
import nora.compiler.ModuleLexer
import nora.compiler.ModuleParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension

class ModuleDefinitionParser(private val expectedName: String) : ModuleBaseVisitor<Set<String>>() {

    companion object {

        private fun extractBindings(moduleFile: Path):Set<String> {
            return parseModule(moduleFile, moduleFile.fileName.nameWithoutExtension)
        }

        private fun parseModule(moduleFile: Path, expectedName: String):Set<String> {
            val visitor = ModuleDefinitionParser(expectedName)
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

    override fun defaultResult(): Set<String> {
        return setOf()
    }

    override fun aggregateResult(aggregate: Set<String>, nextResult: Set<String>): Set<String> {
        return aggregate + nextResult
    }

    //Just here to speed it up a bit
    override fun visitFile_def(ctx: ModuleParser.File_defContext): Set<String> {
        return visitModule_def(ctx.module_def())
    }

    override fun visitModule_def(ctx: ModuleParser.Module_defContext): Set<String> {
        return if(expectedName != ctx.id().text){
            //Todo:Error
            setOf()
        } else {
            super.visitModule_def(ctx)
        }
    }

    override fun visitType_def(ctx: ModuleParser.Type_defContext): Set<String> {
        return if(ctx.sub_type() != null) {
            setOf(ctx.sub_type().id().text)
        } else {
            setOf(ctx.top_type().id().text)
        }
    }

    override fun visitData_def(ctx: ModuleParser.Data_defContext): Set<String> {
        return if(ctx.sub_data_def() != null) {
            setOf(ctx.sub_data_def().id().text)
        } else {
            setOf(ctx.top_data_def().id().text)
        }
    }

    override fun visitTrait_def(ctx: ModuleParser.Trait_defContext): Set<String> {
        return setOf(ctx.id().text)
    }

    override fun visitMulti_method_def(ctx: ModuleParser.Multi_method_defContext): Set<String> {
        return setOf(ctx.id().text)
    }

    override fun visitCase_method_def(ctx: ModuleParser.Case_method_defContext): Set<String> {
        return setOf(ctx.id().text)
    }

    override fun visitFunction_def(ctx: ModuleParser.Function_defContext): Set<String> {
        return setOf(ctx.id().text)
    }
}