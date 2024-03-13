package nora.typesys

import nora.*
import nora.parsed.ParsedState
import kotlin.test.Test

/*
class TypeSysTest {
    val noSrc = Src("",0,0)

    private fun data(name:String, parent:String?, generics:List<Generic<ParsedState>>):Definition<ParsedState> = Data(
        listOf(),
        mapOf(),
        Elem(name, noSrc),
        mapOf(),
        false,
        generics.map { Elem(it,noSrc) },
        listOf(),
        listOf(),
        listOf(),
        if(parent == null) null else Elem(PlainReference(parent), noSrc),
        listOf()
    )
    private fun plainData(name:String, parent:String?):Definition<ParsedState> = data(name, parent, listOf())

    private fun plainTopData(name:String):Definition<ParsedState> = data(name, null, listOf())

    private fun genericTopData(name:String, generics:List<Generic<ParsedState>>):Definition<ParsedState> = data(name, null, generics)

    private fun unboundGen(name:String, variance: Variance):Generic<ParsedState> = Generic(
        Elem(name,noSrc),
        Elem(variance, noSrc),
        listOf()
    )

    @Test
    fun sameTypeTest() {
        setParsedDefinitionEntries(mapOf(
            Pair("DataA", Some(plainTopData("DataA"))),
            Pair("DataB", Some(plainTopData("DataB")))
        ))

        val t1 = DataType(Elem("DataA",noSrc), listOf())
        val t2 = DataType(Elem("DataA",noSrc), listOf())
        val t3 = DataType(Elem("DataB",noSrc), listOf())
        assert(t1.sameTypeAs(t2) is Success)
        assert(t2.sameTypeAs(t1) is Success)
        assert(t2.sameTypeAs(t3) is Failure)
        assert(t3.sameTypeAs(t2) is Failure)
        assert(t1.sameTypeAs(t3) is Failure)
        assert(t3.sameTypeAs(t1) is Failure)
        assert(t1.subTypeOf(t2) is Success)
        assert(t2.subTypeOf(t1) is Success)
        assert(t2.subTypeOf(t3) is Failure)
        assert(t3.subTypeOf(t2) is Failure)
        assert(t1.subTypeOf(t3) is Failure)
        assert(t3.subTypeOf(t1) is Failure)
    }

    @Test
    fun simpleSubTypeTest() {
        setParsedDefinitionEntries(mapOf(
            Pair("DataA", Some(plainTopData("DataA"))),
            Pair("DataB", Some(plainData("DataB", "DataA"))),
            Pair("DataC", Some(plainTopData("DataC")))
        ))

        val t1 = DataType(Elem("DataA", noSrc), listOf())
        val t2 = DataType(Elem("DataB", noSrc), listOf())
        val t3 = DataType(Elem("DataC", noSrc), listOf())
        assert(t1.sameTypeAs(t2) is Failure)
        assert(t2.sameTypeAs(t1) is Failure)
        assert(t2.sameTypeAs(t3) is Failure)
        assert(t3.sameTypeAs(t2) is Failure)
        assert(t1.sameTypeAs(t3) is Failure)
        assert(t3.sameTypeAs(t1) is Failure)
        assert(t1.subTypeOf(t2) is Failure)
        assert(t2.subTypeOf(t1) is Success)
        assert(t2.subTypeOf(t3) is Failure)
        assert(t3.subTypeOf(t2) is Failure)
        assert(t1.subTypeOf(t3) is Failure)
        assert(t3.subTypeOf(t1) is Failure)
    }

    @Test
    fun simpleSubTypeTest2() {
        setParsedDefinitionEntries(mapOf(
            Pair("DataA", Some(plainTopData("DataA"))),
            Pair("DataB", Some(plainData("DataB", "DataA"))),
            Pair("DataC", Some(plainData("DataC", "DataB"))),
            Pair("DataD", Some(plainData("DataD", "DataA")))
        ))

        val tA = DataType(Elem("DataA", noSrc), listOf())
        val tB = DataType(Elem("DataB", noSrc), listOf())
        val tC = DataType(Elem("DataC", noSrc), listOf())
        val tD = DataType(Elem("DataD", noSrc), listOf())

        assert(tB.subTypeOf(tA) is Success)
        assert(tC.subTypeOf(tB) is Success)
        assert(tC.subTypeOf(tA) is Success)
        assert(tD.subTypeOf(tA) is Success)

        assert(tA.subTypeOf(tB) is Failure)
        assert(tB.subTypeOf(tC) is Failure)
        assert(tA.subTypeOf(tC) is Failure)
        assert(tA.subTypeOf(tD) is Failure)

        assert(tC.subTypeOf(tD) is Failure)
        assert(tD.subTypeOf(tC) is Failure)
        assert(tB.subTypeOf(tD) is Failure)
        assert(tD.subTypeOf(tB) is Failure)
    }


    @Test
    fun invarianceSubTypeTest() {

        setParsedDefinitionEntries(mapOf(
            Pair("DataA", Some(genericTopData("DataA", listOf(unboundGen("T", Variance.IN), unboundGen("V", Variance.IN))))),
            Pair("DataB", Some(plainTopData("DataB"))),
            Pair("DataC", Some(plainData("DataC", "DataB"))),
            Pair("DataD", Some(plainData("DataD", "DataB")))
        ))

        val tIn1 = Elem(DataType(Elem("DataB", noSrc), listOf()) as Type, noSrc)
        val tIn2 = Elem(DataType(Elem("DataC", noSrc), listOf()) as Type, noSrc)
        val tIn3 = Elem(DataType(Elem("DataD", noSrc), listOf()) as Type, noSrc)

        assert(tIn2.data.subTypeOf(tIn1.data) is Success)

        val t1 = DataType(Elem("DataA", noSrc), listOf(TypeParam(Variance.IN, tIn1),TypeParam(Variance.IN, tIn1)))
        val t2 = DataType(Elem("DataA", noSrc), listOf(TypeParam(Variance.IN, tIn1),TypeParam(Variance.IN, tIn1)))
        val t3 = DataType(Elem("DataA", noSrc), listOf(TypeParam(Variance.IN, tIn1),TypeParam(Variance.IN, tIn2)))

        assert(t1.sameTypeAs(t2) is Success)
        assert(t1.sameTypeAs(t3) is Failure)
        assert(t3.sameTypeAs(t1) is Failure)
        assert(t1.subTypeOf(t2) is Success)
        assert(t2.subTypeOf(t1) is Success)

        assert(t1.subTypeOf(t3) is Failure)
        assert(t3.subTypeOf(t1) is Failure)
    }

    //Todo: Same with contra

    @Test
    fun covarianceSubTypeTest() {

        setParsedDefinitionEntries(mapOf(
            Pair("DataA", Some(genericTopData("DataA", listOf(unboundGen("T", Variance.CO), unboundGen("V", Variance.CO))))),
            Pair("DataB", Some(plainTopData("DataB"))),
            Pair("DataC", Some(plainData("DataC", "DataB"))),
            Pair("DataD", Some(plainData("DataD", "DataB"))),
            Pair("DataE", Some(plainData("DataE", "DataC")))
        ))

        val tInB = Elem(DataType(Elem("DataB", noSrc), listOf()) as Type, noSrc)
        val tInD = Elem(DataType(Elem("DataD", noSrc), listOf()) as Type, noSrc)
        val tInE = Elem(DataType(Elem("DataC", noSrc), listOf()) as Type, noSrc)

        assert(tInD.data.subTypeOf(tInB.data) is Success)
        assert(tInE.data.subTypeOf(tInB.data) is Success)
        assert(tInE.data.subTypeOf(tInD.data) is Failure)

        val tA1 = DataType(Elem("DataA", noSrc), listOf(TypeParam(Variance.CO, tInB),TypeParam(Variance.CO, tInB)))
        val tA2 = DataType(Elem("DataA", noSrc), listOf(TypeParam(Variance.CO, tInD),TypeParam(Variance.CO, tInE)))
        val tA3 = DataType(Elem("DataA", noSrc), listOf(TypeParam(Variance.CO, tInE),TypeParam(Variance.CO, tInE)))

        assert(tA2.subTypeOf(tA1) is Success)
        assert(tA2.sameTypeAs(tA1) is Failure)
        assert(tA1.subTypeOf(tA2) is Failure)
        assert(tA1.sameTypeAs(tA2) is Failure)

        assert(tA3.subTypeOf(tA1) is Success)
        assert(tA3.sameTypeAs(tA1) is Failure)
        assert(tA1.subTypeOf(tA3) is Failure)
        assert(tA1.sameTypeAs(tA3) is Failure)

        assert(tA3.subTypeOf(tA2) is Failure)
        assert(tA3.sameTypeAs(tA2) is Failure)
        assert(tA2.subTypeOf(tA3) is Failure)
        assert(tA2.sameTypeAs(tA3) is Failure)

    }

    @Test
    fun contravarianceSubTypeTest() {

        setParsedDefinitionEntries(mapOf(
            Pair("DataA", Some(genericTopData("DataA", listOf(unboundGen("T", Variance.CONTRA), unboundGen("V", Variance.CONTRA))))),
            Pair("DataB", Some(plainTopData("DataB"))),
            Pair("DataC", Some(plainData("DataC", "DataB"))),
            Pair("DataD", Some(plainData("DataD", "DataB"))),
            Pair("DataE", Some(plainData("DataE", "DataC")))
        ))

        val tInB = Elem(DataType(Elem("DataB", noSrc), listOf()) as Type, noSrc)
        val tInD = Elem(DataType(Elem("DataD", noSrc), listOf()) as Type, noSrc)
        val tInE = Elem(DataType(Elem("DataE", noSrc), listOf()) as Type, noSrc)

        assert(tInD.data.subTypeOf(tInB.data) is Success)
        assert(tInE.data.subTypeOf(tInB.data) is Success)
        assert(tInE.data.subTypeOf(tInD.data) is Failure)

        val tA1 = DataType(Elem("DataA", noSrc), listOf(TypeParam(Variance.CONTRA, tInB),TypeParam(Variance.CONTRA, tInB)))
        val tA2 = DataType(Elem("DataA", noSrc), listOf(TypeParam(Variance.CONTRA, tInD),TypeParam(Variance.CONTRA, tInE)))
        val tA3 = DataType(Elem("DataA", noSrc), listOf(TypeParam(Variance.CONTRA, tInE),TypeParam(Variance.CONTRA, tInE)))

        assert(tA1.subTypeOf(tA2) is Success)
        assert(tA1.sameTypeAs(tA2) is Failure)
        assert(tA2.subTypeOf(tA1) is Failure)
        assert(tA2.sameTypeAs(tA1) is Failure)

        assert(tA1.subTypeOf(tA3) is Success)
        assert(tA1.sameTypeAs(tA3) is Failure)
        assert(tA3.subTypeOf(tA1) is Failure)
        assert(tA3.sameTypeAs(tA1) is Failure)

        assert(tA2.subTypeOf(tA3) is Failure)
        assert(tA2.sameTypeAs(tA3) is Failure)
        assert(tA3.subTypeOf(tA2) is Failure)
        assert(tA3.sameTypeAs(tA2) is Failure)

    }

    @Test
    fun doubleContraSubTypeTest() {

        setParsedDefinitionEntries(mapOf(
            Pair("DataA", Some(genericTopData("DataA", listOf(unboundGen("T", Variance.CONTRA), unboundGen("V", Variance.CO))))),
            Pair("DataF", Some(genericTopData("DataF", listOf(unboundGen("T", Variance.CONTRA))))),
            Pair("DataB", Some(plainTopData("DataB"))),
            Pair("DataC", Some(plainData("DataC", "DataB"))),
            Pair("DataD", Some(plainData("DataD", "DataB"))),
            Pair("DataE", Some(plainData("DataE", "DataC")))
        ))

        val tInB = Elem(DataType(Elem("DataB", noSrc), listOf()) as Type, noSrc)
        val tInD = Elem(DataType(Elem("DataD", noSrc), listOf()) as Type, noSrc)
        val tInE = Elem(DataType(Elem("DataE", noSrc), listOf()) as Type, noSrc)

        assert(tInD.data.subTypeOf(tInB.data) is Success)
        assert(tInE.data.subTypeOf(tInB.data) is Success)
        assert(tInE.data.subTypeOf(tInD.data) is Failure)

        val tA1 = DataType(Elem("DataA", noSrc), listOf(TypeParam(Variance.CONTRA, Elem(DataType(Elem("DataF", noSrc), listOf(TypeParam(Variance.CONTRA, tInB))), noSrc)),TypeParam(Variance.CO, tInB)))
        val tA2 = DataType(Elem("DataA", noSrc), listOf(TypeParam(Variance.CONTRA, Elem(DataType(Elem("DataF", noSrc), listOf(TypeParam(Variance.CONTRA, tInD))), noSrc)),TypeParam(Variance.CO, tInE)))
        val tA3 = DataType(Elem("DataA", noSrc), listOf(TypeParam(Variance.CONTRA, Elem(DataType(Elem("DataF", noSrc), listOf(TypeParam(Variance.CONTRA, tInE))), noSrc)),TypeParam(Variance.CO, tInE)))

        assert(tA2.subTypeOf(tA1) is Success)
        assert(tA2.sameTypeAs(tA1) is Failure)
        assert(tA1.subTypeOf(tA2) is Failure)
        assert(tA1.sameTypeAs(tA2) is Failure)

        assert(tA3.subTypeOf(tA1) is Success)
        assert(tA3.sameTypeAs(tA1) is Failure)
        assert(tA1.subTypeOf(tA3) is Failure)
        assert(tA1.sameTypeAs(tA3) is Failure)

        assert(tA3.subTypeOf(tA2) is Failure)
        assert(tA3.sameTypeAs(tA2) is Failure)
        assert(tA2.subTypeOf(tA3) is Failure)
        assert(tA2.sameTypeAs(tA3) is Failure)

    }


    @Test
    fun multiVarianceSubTypeTest() {
        setParsedDefinitionEntries(mapOf(
            Pair("DataA", Some(genericTopData("DataA", listOf(unboundGen("T", Variance.IN), unboundGen("V", Variance.CO), unboundGen("E", Variance.CONTRA))))),
            Pair("DataB", Some(plainTopData("DataB"))),
            Pair("DataC", Some(plainData("DataC", "DataB"))),
            Pair("DataD", Some(plainData("DataD", "DataB"))),
            Pair("DataE", Some(plainData("DataE", "DataC")))
        ))

        val tInB = Elem(DataType(Elem("DataB", noSrc), listOf()) as Type, noSrc)
        val tInC = Elem(DataType(Elem("DataC", noSrc), listOf()) as Type, noSrc)
        val tInD = Elem(DataType(Elem("DataD", noSrc), listOf()) as Type, noSrc)
        val tInE = Elem(DataType(Elem("DataE", noSrc), listOf()) as Type, noSrc)

        assert(tInD.data.subTypeOf(tInB.data) is Success)
        assert(tInE.data.subTypeOf(tInB.data) is Success)
        assert(tInE.data.subTypeOf(tInD.data) is Failure)

        val tA1 = DataType(Elem("DataA", noSrc), listOf(TypeParam(Variance.IN, tInB),TypeParam(Variance.CO, tInC),TypeParam(Variance.CONTRA, tInE)))
        val tA2 = DataType(Elem("DataA", noSrc), listOf(TypeParam(Variance.IN, tInB),TypeParam(Variance.CO, tInE),TypeParam(Variance.CONTRA, tInB)))
        val tA3 = DataType(Elem("DataA", noSrc), listOf(TypeParam(Variance.IN, tInE),TypeParam(Variance.CO, tInE),TypeParam(Variance.CONTRA, tInB)))
        val tA4 = DataType(Elem("DataA", noSrc), listOf(TypeParam(Variance.IN, tInB),TypeParam(Variance.CO, tInD),TypeParam(Variance.CONTRA, tInB)))
        val tA5 = DataType(Elem("DataA", noSrc), listOf(TypeParam(Variance.IN, tInB),TypeParam(Variance.CO, tInE),TypeParam(Variance.CONTRA, tInD)))

        assert(tA2.subTypeOf(tA1) is Success)
        assert(tA1.subTypeOf(tA2) is Failure)
        assert(tA1.sameTypeAs(tA2) is Failure)
        assert(tA3.subTypeOf(tA1) is Failure)
        assert(tA4.subTypeOf(tA1) is Failure)
        assert(tA5.subTypeOf(tA1) is Failure)

    }

    //Todo: Constraints
    //Todo: Generics
    //Todo: Seperate File:
    //      Subst & Match
}*/