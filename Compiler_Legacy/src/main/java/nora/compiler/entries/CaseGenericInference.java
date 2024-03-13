package nora.compiler.entries;

import java.util.*;
import java.util.function.Function;

//This algorithm checks two thigs:
// First that all the generics of a case method are actually used (such as the runtime can infer them)
// Second that whenever they are used they match up with a type that satisfy their bound
// How does the algo work:
//  It searches for the own generics in the signature of the case method.
//   If it finds one it sets it to SeenAndSucceeded | SeenAndFailed from NotSeen
//   while doing this it tracks the corresponding type in function signature (as long as possible / matching)
//   when a generic found it checks that the corresponding type provides its bounds (if not it fails)
//   if their is no corresponding type it fails as well (unless no bounds are present)
//   Examples:
//    fun:    List[List[String]]       List[List[String]]   T:Collection        T                   T:Collection
//    case:   List[List[V:Printable]]  List[V:Collection]   V:Collection        List[V:Collection]  List[V]
//    result: V = SeenAndSucceeded     SeenAndFailed        SeenAndSucceeded    SeenAndFailed       SeenAndSucceeded
// As soon as their are trait parameters it gets more complicated
//   example case
//   fun:       T:Collection[E:Printable]
//   case:      List[V:Printable]
//   result:    SeenAndSucceeded
//   reason:    as List[V] has trait Collection[V] we should pair V with E
//              we need a transform into bound: and then march bound (however only if V appears in bound)
public class CaseGenericInference {
    public enum InferenceState {
        Unseen,
        Succeeded,
        Failed
    }

    private static void inferenceFromValType(Instance caseVal, InferenceState[] states, boolean runtimeAvailable){
        if(caseVal instanceof GenericInstance gi){
            //We already failed
            if(states[gi.index] == InferenceState.Failed) return;
            //See if the bounds hold (if we have they fail)
            if(gi.bound != null) {
                states[gi.index] = InferenceState.Failed;
            } else if(runtimeAvailable) {
                states[gi.index] = InferenceState.Succeeded;
            }
        } else {
            var caseDi = (DefInstance) caseVal;
            for(Instance inst: caseDi.getArguments()) {
                inferenceFromValType(inst, states, runtimeAvailable);
            }
        }
    }

    private static Map<Definition, Instance> toMap(Set<Instance> insts){
        var map = new HashMap<Definition, Instance>();
        for(Instance inst:insts){
            map.put(((DefInstance)inst).getBase(), inst);
        }
        return map;
    }

    private static void inferenceFromValType(Instance caseVal, Instance funVal, InferenceState[] states, boolean runtimeAvailable){
        if(caseVal instanceof GenericInstance gi){
            //We already failed
            if(states[gi.index] == InferenceState.Failed) return;
            //See if the bounds hold
            if(gi.bound != null && !funVal.fulfills(gi.bound)) {
                states[gi.index] = InferenceState.Failed;
            } else if(runtimeAvailable) {
                states[gi.index] = InferenceState.Succeeded;
            }
        } else {
            var caseDiArgs = ((DefInstance) caseVal).getArguments();
            if(funVal instanceof DefInstance funDi){
                var funDiArgs = funDi.getArguments();
                //this assumes that we already checked that types are in same hierarchy
                int i = 0;
                for(Instance caseArg: caseDiArgs){
                    if(i < funDiArgs.size()){
                        inferenceFromValType(caseArg, funDiArgs.get(i++), states, runtimeAvailable);
                    } else {
                        inferenceFromValType(caseArg, states, runtimeAvailable);
                    }
                }
            } else if(funVal instanceof GenericInstance gi) {
                var funGiBound = gi.bound;
                if(funGiBound == null) {
                    inferenceFromValType(caseVal, states, runtimeAvailable);
                } else if(funGiBound.isData()){
                    inferenceFromValType(caseVal, funGiBound, states, false);
                } else {
                    var funInstances = toMap(Instance.collectTraitHierarchy(funGiBound));
                    var valInstances = toMap(Instance.collectTraitHierarchy(caseVal));
                    var sharedKeys = new HashSet<>(funInstances.keySet());
                    sharedKeys.retainAll(funInstances.keySet());
                    if(sharedKeys.isEmpty()){
                        for(Instance caseArg: caseDiArgs){
                            inferenceFromValType(caseArg, states, runtimeAvailable);
                        }
                    } else {
                        for(Definition def: sharedKeys){
                            inferenceFromValType(valInstances.get(def), funInstances.get(def), states, false);
                        }
                    }
                }
            }
        }
    }

    public static void inferenceFromValType(Instance caseVal, Instance funVal, InferenceState[] states){
        inferenceFromValType(caseVal, funVal, states, true);
    }

    public static boolean checkWitchState(int generics, Function<InferenceState[], Boolean> checker){
        var genStates = new InferenceState[generics];
        Arrays.fill(genStates, InferenceState.Unseen);
        if(!checker.apply(genStates)) return false;
        for(int i = 0; i < generics; i++){
            if(genStates[i] != InferenceState.Succeeded) return false;
        }
        return true;
    }
}
