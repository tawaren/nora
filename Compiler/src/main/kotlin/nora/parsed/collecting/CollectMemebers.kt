package nora.parsed.collecting

import nora.*
import nora.Array
import nora.Function
import nora.parsed.checking.RefState

//Todo: Later add collectors for annotations
//      & Custom Annotations

//Special Strings to collect extra infos
// We use ::: as these constitute illegal names
val DataRoots = Ref(":::DataRoot")

fun collectAllRefs():Map<String,List<Ref>> {
    val initial = mapOf<String, List<Ref>>()
    return moduleList().fold(initial){m, mp ->
        val defs = parsedModule(mp).toNullable()?.defs?.values?.toList()?: listOf()
        defs.collectRefs(m)
    }
}

fun List<Definition<RefState>>.collectRefs(col:Map<String,List<Ref>>):Map<String,List<Ref>> {
    return fold(col){m, l ->
        val nEntries = l.collectRefs()
        (m.keys + nEntries.keys).associateWith { (m[it]?:listOf()) + (nEntries[it]?:listOf()) }
    }
}


fun Definition<RefState>.collectRefs():Map<String,List<Ref>> {
    val self = Ref(name.data)

    val annotCol = annotations.fold(mapOf<String,List<Ref>>()) { m, a ->
        val entries = m[a.first.data.name]?: listOf()
        m + Pair(a.first.data.name, entries + self)
    }
    return accept(MemberCollector, Unit).fold(annotCol) { m, t ->
        val entries = m[t.name]?: listOf()
        m + Pair(t.name, entries + self)
    }
}

//Just for Dispatching
object MemberCollector : DefinitionVisitor<RefState, List<Ref>, Unit> {
    override fun visitDefinition(definition: Definition<RefState>, ctx: Unit):List<Ref> = listOf()
    override fun visitCaseMethod(caseMethod: CaseMethod<RefState>, ctx:Unit):List<Ref> = caseMethod.collectRefs()
    override fun visitData(data: Data<RefState>, ctx:Unit):List<Ref> = data.collectRefs()
    override fun visitTrait(trait: Trait<RefState>, ctx: Unit):List<Ref> = trait.collectRefs()
    override fun visitTuple(tuple: Tuple<RefState>, ctx:Unit):List<Ref> = tuple.collectRefs()
    override fun visitFunction(function: Function<RefState>, ctx:Unit):List<Ref> = function.collectRefs()
    override fun visitArray(array: Array<RefState>, ctx:Unit):List<Ref> = array.collectRefs()
}

fun CaseMethod<RefState>.collectRefs():List<Ref> = listOf(parent.data)
fun Data<RefState>.collectRefs():List<Ref>{
    val traitP = traits.map { it.data.main.data as Ref }
    return traitP + (parent?.data ?: DataRoots)
}
fun Trait<RefState>.collectRefs():List<Ref> = traits.map { it.data.main.data as Ref }
fun Tuple<RefState>.collectRefs():List<Ref> = traits.map { it.data.main.data as Ref }
fun Function<RefState>.collectRefs():List<Ref> = traits.map { it.data.main.data as Ref }
fun Array<RefState>.collectRefs():List<Ref>{
    val traitP = traits.map { it.data.main.data as Ref }
    if(parent != null) return traitP + parent.data
    return traitP
}

