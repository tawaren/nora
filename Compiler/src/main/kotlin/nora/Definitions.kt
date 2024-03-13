package nora

import nora.typesys.Variance

interface Definition<L> {
    val name:Elem<String>
    val annotations:List<Pair<Elem<Ref>, Elem<List<Elem<Attribute>>>>>
    val extra:Map<Extra, Value>
    val modifier:Map<Modifier, Src>
    //for visitor pattern
    fun <T,C> accept(vis: DefinitionVisitor<L,T,C>, ctx:C): T
}


interface Parameterized<L> : Definition<L> {
    val generics:List<Elem<Generic<L>>>
    fun totalArgs():Int = generics.size //inclusive hidden ones
}

interface VarArgParameterized<L> : Parameterized<L> {
    fun minVarArgs():Int = 0
    fun genGenerics(amount:Int):List<Elem<Generic<L>>>
}

interface Callable<L> : Parameterized<L>{
    val args:List<Elem<Argument<L>>>
    val ret:Elem<L>
}

interface Implemented<L> : Callable<L>{
    val body:Elem<Expr<L>>
}

interface Container<L> : Parameterized<L> {
    val fields:List<Elem<Argument<L>>>
    val isConcrete: Boolean
}

data class MultiMethod<L>(
    override val annotations:List<Pair<Elem<Ref>, Elem<List<Elem<Attribute>>>>>,
    override val modifier:Map<Modifier, Src>,
    override val name:Elem<String>,
    override val extra:Map<Extra, Value>,
    override val generics:List<Elem<Generic<L>>>,
    val dynamicArgs:List<Elem<Argument<L>>>,
    val staticArgs:List<Elem<Argument<L>>>,
    override val ret:Elem<L>,
) : Callable<L> {
    override val args = dynamicArgs + staticArgs
    override fun <T,C> accept(vis: DefinitionVisitor<L,T,C>, ctx:C): T = vis.visitMultiMethod(this, ctx)
}

data class Method<L>(
    override val annotations:List<Pair<Elem<Ref>, Elem<List<Elem<Attribute>>>>>,
    override val modifier:Map<Modifier, Src>,
    override val name:Elem<String>,
    override val extra:Map<Extra, Value>,
    override val generics:List<Elem<Generic<L>>>,
    override val args:List<Elem<Argument<L>>>,
    override val ret:Elem<L>,
    override val body:Elem<Expr<L>>,
) : Implemented<L> {
    override fun <T,C> accept(vis: DefinitionVisitor<L,T,C>, ctx:C): T = vis.visitMethod(this, ctx)
}

data class CaseMethod<L>(
    override val annotations:List<Pair<Elem<Ref>, Elem<List<Elem<Attribute>>>>>,
    override val modifier:Map<Modifier, Src>,
    override val name:Elem<String>,
    override val extra:Map<Extra, Value>,
    override val generics:List<Elem<Generic<L>>>,
    override val args:List<Elem<Argument<L>>>,
    override val ret:Elem<L>,
    val parent: Elem<Ref>,
    val parentApplies: List<Elem<L>>,
    override val body:Elem<Expr<L>>,
) : Implemented<L> {
    override fun <T,C> accept(vis: DefinitionVisitor<L,T,C>, ctx:C): T = vis.visitCaseMethod(this, ctx)
}

data class ObjectMethod<L>(
    override val annotations:List<Pair<Elem<Ref>, Elem<List<Elem<Attribute>>>>>,
    override val modifier:Map<Modifier, Src>,
    override val name:Elem<String>,
    override val extra:Map<Extra, Value>,
    override val generics:List<Elem<Generic<L>>>,
    val objectType:Elem<L>,
    val hiddenGenerics:List<Elem<Generic<L>>>,
    override val args:List<Elem<Argument<L>>>,
    override val ret:Elem<L>,
    override val body:Elem<Expr<L>>,
) : Implemented<L> {
    override fun <T,C> accept(vis: DefinitionVisitor<L,T,C>, ctx:C): T = vis.visitObjectMethod(this, ctx)
    override fun totalArgs(): Int = generics.size + hiddenGenerics.size
}

data class Data<L>(
    override val annotations:List<Pair<Elem<Ref>, Elem<List<Elem<Attribute>>>>>,
    override val modifier:Map<Modifier, Src>,
    override val name:Elem<String>,
    override val extra:Map<Extra, Value>,
    override val isConcrete: Boolean,
    override val generics:List<Elem<Generic<L>>>,
    val hiddenGenerics:List<Elem<Generic<L>>>,
    override val fields:List<Elem<Argument<L>>>,
    val hiddenFields:List<Elem<Argument<L>>>,
    val parent: Elem<Ref>?,
    val traits: List<Elem<L>>,
) : Container<L> {
    override fun <T,C> accept(vis: DefinitionVisitor<L,T,C>, ctx:C): T = vis.visitData(this, ctx)
    override fun totalArgs(): Int = generics.size + hiddenGenerics.size
}

data class Trait<L>(
    override val annotations:List<Pair<Elem<Ref>, Elem<List<Elem<Attribute>>>>>,
    override val modifier:Map<Modifier, Src>,
    override val name:Elem<String>,
    override val extra:Map<Extra, Value>,
    override val generics:List<Elem<Generic<L>>>,
    val traits: List<Elem<L>>,
) : Parameterized<L> {
    override fun <T,C> accept(vis: DefinitionVisitor<L,T,C>, ctx:C): T = vis.visitTrait(this, ctx)
}

data class Tuple<L>(
    override val annotations:List<Pair<Elem<Ref>, Elem<List<Elem<Attribute>>>>>,
    override val modifier:Map<Modifier, Src>,
    override val name:Elem<String>,
    override val extra:Map<Extra, Value>,
    val traits: List<Elem<L>>,
) : VarArgParameterized<L> {
    override fun <T,C> accept(vis: DefinitionVisitor<L,T,C>, ctx:C): T = vis.visitTuple(this, ctx)
    override val generics: List<Elem<Generic<L>>> = listOf()
    override fun minVarArgs(): Int = 0
    override fun genGenerics(amount: Int): List<Elem<Generic<L>>> {
        val src = name.src
        return (0..<amount).map { Elem(Generic(Elem(Gen("$$it", it),src), Elem(Variance.CO, src), listOf(), null), src) }
    }
}

data class Array<L>(
    override val annotations:List<Pair<Elem<Ref>, Elem<List<Elem<Attribute>>>>>,
    override val modifier:Map<Modifier, Src>,
    override val name:Elem<String>,
    override val extra:Map<Extra, Value>,
    val elemType: Elem<Generic<L>>,
    val parent: Elem<Ref>?,
    val traits: List<Elem<L>>,
) : Parameterized<L> {
    override val generics: List<Elem<Generic<L>>> = listOf(elemType)
    override fun <T,C> accept(vis: DefinitionVisitor<L,T,C>, ctx:C): T = vis.visitArray(this, ctx)
}

data class Function<L>(
    override val annotations:List<Pair<Elem<Ref>, Elem<List<Elem<Attribute>>>>>,
    override val modifier:Map<Modifier, Src>,
    override val name:Elem<String>,
    override val extra:Map<Extra, Value>,
    val traits: List<Elem<L>>,
) : VarArgParameterized<L>  {
    override fun <T,C> accept(vis: DefinitionVisitor<L,T,C>, ctx:C): T = vis.visitFunction(this, ctx)
    override val generics: List<Elem<Generic<L>>> = listOf()
    override fun minVarArgs(): Int = 0
    override fun genGenerics(amount: Int): List<Elem<Generic<L>>> {
        if(amount == 0) return listOf()
        val src = name.src
        val args = (0..<(amount-1)).map { Elem(Generic(Elem(Gen("\$P$it",it),src), Elem(Variance.CONTRA, src), listOf<Elem<L>>(), null), src) }
        return args + Elem(Generic(Elem(Gen("\$R",args.size),src), Elem(Variance.CO, src), listOf(), null), src)
    }
}

data class Annotation<L>(
    override val name:Elem<String>,
    override val annotations:List<Pair<Elem<Ref>, Elem<List<Elem<Attribute>>>>>,
    override val extra:Map<Extra, Value>,
    override val modifier:Map<Modifier, Src>,
):Definition<L>{
    override fun <T,C> accept(vis: DefinitionVisitor<L,T,C>, ctx:C): T = vis.visitAnnotation(this, ctx)
}
