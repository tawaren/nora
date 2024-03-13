package nora.util.consList;

import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class ConsList<T> {
    public abstract ConsList<T> map(Function<T, T> f);

    public abstract ConsList<T> filter(Function<T, Boolean> f);
    public abstract void toString(StringBuilder builder);

    public static <T> boolean eq_opt(ConsList<T> left, ConsList<T>  right) {
        while (true){
            if(left instanceof Cons<T> c1 && right instanceof Cons<T> c2) {
                if(!c1.head.equals(c2.head)) return false;
                left = c1.tail;
                right = c2.tail;
            } else if(left instanceof Nil<T> && right instanceof Nil<T>){
                return true;
            } else {
                return false;
            }
        }

    }

    public static ConsList<Integer> build(BiFunction<ConsList<Integer>,Integer,ConsList<Integer>> builder, ConsList<Integer> aggr, int iters){
        var res = builder.apply(aggr, iters);
        if(iters == 0) {
            return res;
        } else {
            return build(builder, res, iters-1);
        }
    }
}
