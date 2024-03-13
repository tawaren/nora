package nora.compiler.entries.exprs.resolved;

import nora.compiler.entries.DefInstance;
import nora.compiler.entries.GenericInstance;
import nora.compiler.entries.Instance;
import nora.compiler.entries.exprs.Expr;
import nora.compiler.entries.resolved.Argument;
import nora.compiler.entries.resolved.Variance;
import nora.compiler.processing.TypeCheckContext;
import nora.compiler.processing.TypeCollector;
import nora.compiler.processing.TypeMismatchException;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CreateExpr extends ResolvedExpr {
    private Instance target;
    private final Map<String,Expr> fields;
    private final boolean isTailCtx;
    //will be filled by getAndInferenceType
    private final List<Expr> positionalFields = new LinkedList<>();
    private boolean isValidated = false;


    public CreateExpr(Instance target, Map<String,Expr> fields, boolean isTailCtx) {
        this.target = target;
        this.fields = fields;
        this.isTailCtx = isTailCtx;
    }

    @Override
    public Instance getAndInferenceType(TypeCheckContext context) {
        if(!isValidated) {
            if(target.isData() && target instanceof DefInstance di){
                //We can not validate whole data type yet as target may need arguments infering
                if(!di.getBase().validateAndInfer()) throw new RuntimeException("Invalid data type");
                var data = di.getBase().asData();
                var isTuple = TypeCheckContext.isTuple(data);
                List<Argument> nFields;
                //I hate this special treatment so much
                // todo: a real compiler must have a much better architecture
                //  Instance must be subtypable with tuple, and no direct Definition access (always proxie by insatance)
                if(!isTuple){
                    nFields = data.getFields();
                } else if(di.getArguments() != null){
                    nFields = new LinkedList<>();
                    var tArgs = di.getArguments();
                    for(int i = 0; i < tArgs.size(); i++){
                        nFields.add(new Argument(TypeCheckContext.getTupleFieldName(i), tArgs.get(i)));
                    }
                } else {
                    nFields = new LinkedList<>();
                    var numArgs = fields.size();
                    for(int i = 0; i < numArgs; i++){
                        nFields.add(new Argument(TypeCheckContext.getTupleFieldName(i), new GenericInstance(Variance.Covariance, i,null)));
                    }
                }

                if(fields.size() != nFields.size() && !isTuple) throw new TypeMismatchException();

                var types = new LinkedList<Instance>();
                var retHint = context.getRetHint();
                if(retHint instanceof DefInstance ri){
                    int i = 0;
                    for(Argument arg:nFields){
                        Expr argExp = fields.get(arg.name());
                        if(argExp == null) argExp = fields.get("$"+i);
                        //Todo: make a structure exception
                        if(argExp == null) throw new RuntimeException("Not all fields were provided");
                        positionalFields.add(argExp);
                        types.add(context.withReturnHint(arg.typ().substitute(ri.getArguments()), argExp::getAndInferenceType));
                        i++;
                    }
                } else {
                    int i = 0;
                    for(Argument arg:nFields){
                        var argExp = fields.get(arg.name());
                        if(argExp == null) argExp = fields.get("$"+i);
                        //Todo: make a structure exception
                        if(argExp == null) throw new RuntimeException("Not all fields were provided");
                        positionalFields.add(argExp);
                        types.add(context.withoutReturnHint(argExp::getAndInferenceType));
                        i++;
                    }
                }
                if(di.getArguments() == null) {
                    target = context.inferDataArgs(target, types);
                    di = (DefInstance) target;
                }
                //We delayed validation until we inferenced the arguments
                //Todo: Make a validation Exception
                //We are in the body, where variance does not matter for instantiation
                //  it will only matter for assignements but this is checked later
                if(!target.validateAndInfer(null)) throw new RuntimeException("Invalid data type");
                if(types.size() != fields.size()) throw new TypeMismatchException();
                int i = 0;
                for(Instance t: types) {
                    var fieldT = nFields.get(i++).typ().substitute(di.getArguments());
                    if(!t.subType(fieldT)) throw new TypeMismatchException();
                }
                isValidated = true;
            } else {
                //Todo: make a structure exception
                throw new RuntimeException("Only Data types can be created");
            }
        }
        return target;
    }

    @Override
    public boolean needsHints() {
        if(target instanceof DefInstance di && di.getArguments() == null){
            return di.isData() && di.getBase().asData().getGenerics().size() != 0;
        }
        return false;
    }

    @Override
    public int countLocals() {
        return fields.values().stream().mapToInt(Expr::countLocals).sum();
    }

    @Override
    public void generateCode(OutputStreamWriter out, int ident) throws IOException {
        if(isTailCtx){
            out.append("<tail-create>#").append(TypeCollector.get().resolveEntityString(target));
        } else {
            out.append("<create>#").append(TypeCollector.get().resolveEntityString(target));
        }
        out.append("(");
        for (Iterator<Expr> it = positionalFields.iterator(); it.hasNext();) {
            it.next().generateCode(out, ident);
            if (it.hasNext()) out.append(",");
        }
        out.append(")");
    }

    @Override
    public String toString() {
        return target+"("+String.join(", ", positionalFields.stream().map(Object::toString).toList())+")";
    }
}
