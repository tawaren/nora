package nora.resolved.checking

import nora.Gen
import nora.typesys.*

//Note: We do not match generic constraints as the runtime does not have them
//      Thus we could get a error at runtime if we used a constraint
// However, unlike runtime we do not consider variance & sub super / mixed here
//      If they would missmatch we will get an error when we do the type check
//      Runtime combines matching + type check for performance reason
fun extractApplies(t1:Type, t2:Type):List<Pair<Gen, Type>> {
    return when(t1){
        is DataType -> when(t2){
            is DataType -> t1.parameters.zip(t2.parameters).fold(listOf()) {
                l, (p1,p2) -> l + extractApplies(p1.param.data, p2.param.data)
            }
            is GenericType -> listOf(Pair(t2.name.data, t1))
            else -> listOf()
        }
        is GenericType -> when(t2){
            is DataType -> listOf(Pair(t1.name.data, t2))
            //is GenericType -> if they different it will lead to type error later
            else -> listOf()
        }
        else -> listOf()
    }
}

//Todo: Gen errors!!
fun checkAndMapApplies(appls:List<Pair<Gen, Type>>):Map<Gen,Type> {
    val asMap = appls.toMap()
    val misMatches = appls.filter { asMap[it.first] != it.second  }.map { it.first }.toSet()
    return asMap.filterKeys { !misMatches.contains(it) }
}