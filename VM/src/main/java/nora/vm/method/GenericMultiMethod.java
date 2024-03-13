package nora.vm.method;

import meta.MetaMethodCaseFilter;
import nora.vm.runtime.NoraVmContext;
import nora.vm.types.TypeUtil;
import nora.vm.types.Type;
import nora.vm.types.pattern.TypePattern;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


//This needs to be spezialized before use and thus is not a runtime value
// Its made for fast processing
public class GenericMultiMethod {
    public record GenericDispatchInfo(int typParams, TypePattern[] appliedGenerics, TypePattern[] arguments, TypePattern ret, String methodIdentifier, MetaMethodCaseFilter[] metaFilters) {
        @Override
        public String toString() {
            return "DispatchInfo::"+methodIdentifier+"("+String.join(",", Arrays.stream(arguments).map(Object::toString).toList())+") -> "+ret;
        }
    }

    public record MethodDispatch(Type[] arguments, Method method) { }

    private record SpecKey(Type[] applies) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SpecKey specKey = (SpecKey) o;
            return Arrays.equals(applies, specKey.applies);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(applies);
        }
    }

    private final Map<SpecKey,MultiMethod> specCache = new HashMap<>();

    private final GenericDispatchInfo[] allMethods;
    private final TypePattern[] dynamicArgumentTypes;
    private final TypePattern[] staticArgumentTypes;
    private final TypePattern returnType;

    public GenericMultiMethod(GenericDispatchInfo[] allMethods, TypePattern[] dynamicArgumentTypes, TypePattern[] staticArgumentTypes, TypePattern returnType) {
        this.allMethods = allMethods;
        this.dynamicArgumentTypes = dynamicArgumentTypes;
        this.staticArgumentTypes = staticArgumentTypes;
        this.returnType = returnType;
    }

    //This is expensive, but its done only once per call side
    // So will probably not matter when taking parsing and constructing Ast + Spezialisation into account,
    public MultiMethod specialise(Type[] applies) {
        var key = new SpecKey(applies);
        return specCache.computeIfAbsent(key, k -> {
            //Shall we cache and reuse?
            MethodDispatch[] specialised = new MethodDispatch[allMethods.length];
            var nextFreeIndex = 0;

            //Build the new type signature
            var resolvedDynArgTypes = new Type[dynamicArgumentTypes.length];
            for(int i = 0; i < dynamicArgumentTypes.length; i++){
                resolvedDynArgTypes[i] = dynamicArgumentTypes[i].buildType(applies);
            }
            //Build the new type signature
            var resolvedStatArgTypes = new Type[staticArgumentTypes.length];
            for(int i = 0; i < staticArgumentTypes.length; i++){
                resolvedStatArgTypes[i] = staticArgumentTypes[i].buildType(applies);
            }
            var resolvedRetType = returnType.buildType(applies);

            boolean exhaustive = false;

            methodCheck:
            for (GenericDispatchInfo inf : allMethods) {
                var filters = inf.metaFilters;
                var name = inf.methodIdentifier;
                for (MetaMethodCaseFilter metaMethodCaseFilter : filters) {
                    if (!metaMethodCaseFilter.preCheck(name, applies)) continue methodCheck;
                }
                //Check the generics (and inference type applies)
                //This allows stuff like test[V] for multi[List[V]](String,Bool)
                //     this allows to infere V, despite beeing absent in the signature
                //     we can do a mixedMatch on the generics as safety is guaranteed:
                //      by matching whole signature with correct variance:
                //       then is: dynArguments: Mixed
                //                statArguments: Contra
                //                returnValue: Co
                var genPats = inf.appliedGenerics;
                var matcher = new Type[inf.typParams];
                for(int i = 0; i < genPats.length; i++){
                    if(!genPats[i].mixedTypeMatch(applies[i], matcher)) continue methodCheck;
                }

                //Check the Arguments (and inference type applies)
                var finalSubtype = true;
                for (int a = 0; a < dynamicArgumentTypes.length; a++) {
                    //Dynamic dispatch does not match on type parameters and thus is contra variant on them
                    //  See mixedVariance impl
                    //  This is because type parameters do only have a static context and no dynamic
                    //  As there is no value that could be more concrete and used for dispatch (they are the value)
                    //   Some may have a field but others have none or multiple so that can not be used
                    switch (inf.arguments[a].mixedDynamicArgTypeMatch(resolvedDynArgTypes[a], matcher)){
                        case Sub: finalSubtype = false;
                        case Super: break;
                        case None: continue methodCheck;
                    }
                }
                var staticOffset = dynamicArgumentTypes.length;
                for (int a = 0; a < staticArgumentTypes.length; a++) {
                    if(!inf.arguments[a+staticOffset].supertypeMatch(resolvedStatArgTypes[a], matcher)){
                        continue methodCheck;
                    }
                }

                //Check the return type is still safe to assign (and inference type applies)
                if(!inf.ret.subtypeMatch(resolvedRetType, matcher)) {
                    continue;
                }

                //We have it before stabilized so it could fill missing ones
                for (MetaMethodCaseFilter filter : filters) {
                    if (!filter.postCheck(name, applies, matcher)) {
                        continue methodCheck;
                    }
                }

                //stabilize the matched args
                for(int i = 0; i < matcher.length; i++){
                    matcher[i] = matcher[i].stabilized();
                }

                //Build the concrete argument stable argument List
                var args = inf.arguments;
                var appliedArgs = new Type[args.length];
                for(int i = 0; i < appliedArgs.length; i++){
                    appliedArgs[i] = args[i].buildType(matcher);
                }

                specialised[nextFreeIndex++] = new MethodDispatch(appliedArgs, NoraVmContext.getMethodFactory(null).create(inf.methodIdentifier, matcher));
                // this means that all future impls will be less specific
                //  it requires that most specific come first
                if(finalSubtype) {
                    exhaustive = true;
                    break;
                }
            }

            MethodDispatch[] result = new MethodDispatch[nextFreeIndex];
            System.arraycopy(specialised, 0, result, 0, nextFreeIndex);
            var sig = new Type.TypeParameter[dynamicArgumentTypes.length + staticArgumentTypes.length + 1];
            for(int i = 0; i < sig.length-1; i++){
                TypePattern pat;
                if(i < dynamicArgumentTypes.length) {
                    pat = dynamicArgumentTypes[i];
                } else {
                    pat = staticArgumentTypes[i-dynamicArgumentTypes.length];
                }
                sig[i] = new Type.TypeParameter(Type.Variance.Contra,pat.buildType(applies));
            }
            TypeUtil util = NoraVmContext.getTypeUtil(null);
            sig[sig.length-1] = new Type.TypeParameter(Type.Variance.Co, returnType.buildType(applies));
            var funType = new Type(util.FunctionTypeInfo, sig);
            if(result.length == 0) System.out.println(Arrays.toString(allMethods));
            return new MultiMethod(result, resolvedDynArgTypes, funType, exhaustive);
        });
    }

    @Override
    public String toString() {
        return Arrays.toString(allMethods);
    }
}
