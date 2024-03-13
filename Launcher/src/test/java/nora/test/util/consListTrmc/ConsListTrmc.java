package nora.test.util.consListTrmc;

import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class ConsListTrmc<T> {

    public ConsListTrmc<T> map(Function<T, T> f) {
        ConsListTrmc<T> res = null;
        ConsTrmc<T> ctx = null;
        ConsListTrmc<T> cur = this;
        while (cur instanceof ConsTrmc<T> ct){
            var n = new ConsTrmc<T>(f.apply(ct.head), null);
            if(ctx == null) res = n;
            else ctx.tail = n;
            ctx = n;
            cur = ct.tail;
        }
        if(ctx == null) res = cur;
        else ctx.tail = cur;
        return res;
    }
    public abstract void toString(StringBuilder builder);

    public static <T> boolean eq_opt(ConsListTrmc<T> left, ConsListTrmc<T> right) {
        while (true){
            if(left instanceof ConsTrmc<T> c1 && right instanceof ConsTrmc<T> c2) {
                if(!c1.head.equals(c2.head)) return false;
                left = c1.tail;
                right = c2.tail;
            } else if(left instanceof NilTrmc<T> && right instanceof NilTrmc<T>){
                return true;
            } else {
                return false;
            }
        }

    }

    public static ConsListTrmc<Integer> build(BiFunction<ConsListTrmc<Integer>,Integer, ConsListTrmc<Integer>> builder, ConsListTrmc<Integer> aggr, int iters){
        var res = builder.apply(aggr, iters);
        if(iters == 0) {
            return res;
        } else {
            return build(builder, res, iters-1);
        }
    }
}
