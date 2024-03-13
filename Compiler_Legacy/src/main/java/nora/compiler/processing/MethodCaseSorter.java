package nora.compiler.processing;

import nora.compiler.entries.GenericInstance;
import nora.compiler.entries.Instance;
import nora.compiler.entries.resolved.Argument;
import nora.compiler.entries.resolved.ResolvedCaseMethod;

import java.util.Comparator;

public class MethodCaseSorter implements Comparator<ResolvedCaseMethod> {
    private final boolean useDepedencyAnnotation;
    private final boolean useConfigAnnotation;
    private final boolean rankGenericLoweOnTie;
    private final boolean useReturnType;


    private final Comparator<Instance> instanceComparator = new Comparator<>() {
        private int boundedCompare(Instance o1, Instance o2) {
            if(o1 instanceof GenericInstance gi) return boundedCompare(gi.getBound(), o2);
            if(o2 instanceof GenericInstance gi) return boundedCompare(o1, gi.getBound());
            if(o1 == null && o2 != null) return 1;
            if(o1 != null && o2 == null) return -1;
            if(o1 == null) return 0;
            var o1SubO2 = o1.fulfills(o2);
            var o2SubO1 = o2.fulfills(o1);
            if(o1SubO2 == o2SubO1) return 0;
            if(o1SubO2) return -1;
            return 1;
        }

        @Override
        public int compare(Instance o1, Instance o2) {
            var boundedCmp = boundedCompare(o1,o2);
            if(!rankGenericLoweOnTie || boundedCmp != 0) return boundedCmp;
            //if bounded is same we rank the non-generic one as more specific
            if(o1.isGeneric() && !o2.isGeneric()) return 1;
            if(!o1.isGeneric() && o2.isGeneric()) return -1;
            return 0;
        }
    };

    public MethodCaseSorter(boolean useDepedencyAnnotation,  boolean rankGenericLoweOnTie, boolean useReturnType, boolean useConfigAnnotation) {
        this.useDepedencyAnnotation = useDepedencyAnnotation;
        this.useConfigAnnotation = useConfigAnnotation;
        this.useReturnType = useReturnType;
        this.rankGenericLoweOnTie = rankGenericLoweOnTie;
    }

    public MethodCaseSorter(int level) {
        this(level > 0, level > 1, level > 2, level > 3);
    }

    public MethodCaseSorter() {
        this(4);
    }

    public int compareDependencyClaims(ResolvedCaseMethod o1, ResolvedCaseMethod o2){
        var o1BeforeO2 = o1.getBefore() != null && o1.getBefore().getFullyQualifiedName().equals(o2.fullyQualifiedName);
        var o2AfterO1 = o2.getAfter() != null && o2.getAfter().getFullyQualifiedName().equals(o1.fullyQualifiedName);
        var o2BeforeO1 = o2.getBefore() != null && o2.getBefore().getFullyQualifiedName().equals(o1.fullyQualifiedName);
        var o1AfterO2 = o1.getAfter() != null && o1.getAfter().getFullyQualifiedName().equals(o2.fullyQualifiedName);
        if((o1BeforeO2 | o2AfterO1) & !(o2BeforeO1 | o1AfterO2)) return -1;
        if(!(o1BeforeO2 | o2AfterO1) & (o2BeforeO1 | o1AfterO2)) return 1;
        return 0;
    }

    @Override
    public int compare(ResolvedCaseMethod o1, ResolvedCaseMethod o2) {
        var o2Args = o2.getDynamicArgs();
        int i = 0;
        for(Argument arg: o1.getDynamicArgs()){
           var arg2 = o2Args.get(i++).typ();
           var res = instanceComparator.compare(arg.typ(), arg2);
           if(res != 0) return res;
        }

        if(useDepedencyAnnotation){
            var depOrder = compareDependencyClaims(o1,o2);
            if(depOrder != 0) return depOrder;
        }

        if(useConfigAnnotation){
            //Todo: @Configure
        }

        if(useReturnType){
            return instanceComparator.compare(o1.getReturnType(), o2.getReturnType());
        } else {
            return 0;
        }
    }
}
