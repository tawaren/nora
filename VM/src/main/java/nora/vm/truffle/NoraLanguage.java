package nora.vm.truffle;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.nodes.Node;
import nora.vm.method.EntryPoint;
import nora.vm.method.Method;
import nora.vm.nodes.ArgNode;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.interop.ForeignToNoraConversionNodeGen;
import nora.vm.nodes.interop.NoraToForeignConversionNodeGen;
import nora.vm.nodes.method.NoraCallNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.types.Type;



//Todo: This does not work yet this we pass null
@TruffleLanguage.Registration(id = "nora", name = "Nora")
public class NoraLanguage extends TruffleLanguage<NoraVmContext> {
    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        var id = request.getSource().getCharacters().toString();
        //Todo: can we parse generics
        var generics = Type.NO_GENERICS;

        final Method fun = NoraVmContext.getMethodFactory(null).create(id, generics);
        if(NoraVmContext.get(null).runSave){
            var appls = fun.getType().applies;
            var args = new NoraNode[appls.length-1];
            for(int i = 0; i < args.length; i++){
                args[i] = ForeignToNoraConversionNodeGen.create(appls[i].type(), new ArgNode(i+1));
            }
            var callNode = NoraCallNode.createDirectMethodCall(fun,args);
            var ret = NoraToForeignConversionNodeGen.create(callNode);
            return new EntryPoint(0,ret).getCallTarget();
        } else {
            return fun.getTarget();
        }
    }

    @Override
    protected NoraVmContext createContext(Env env) {
        return new NoraVmContext(env.getEnvironment().get("base-dir"), env.getEnvironment().get("run-save").equals("true"));
    }

    @Override
    protected void disposeContext(NoraVmContext context) {
        context.close();
    }

    @Override
    protected void initializeContext(NoraVmContext context) throws Exception {
        context.init();
    }

    private static final LanguageReference<NoraLanguage> REFERENCE =
            LanguageReference.create(NoraLanguage.class);

    public static NoraLanguage get(Node node) {
        return REFERENCE.get(node);
    }

}
