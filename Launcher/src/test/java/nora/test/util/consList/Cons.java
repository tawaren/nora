package nora.util.consList;

import java.util.function.Function;

public class Cons<T> extends ConsList<T> {
    public final T head;
    public final ConsList<T> tail;

    public Cons(T head, ConsList<T> tail) {
        this.head = head;
        this.tail = tail;
    }

    @Override
    public ConsList<T> map(Function<T, T> f) {
        return new Cons<>(f.apply(head), tail.map(f));
    }

    @Override
    public ConsList<T> filter(Function<T, Boolean> f) {
        if (f.apply(head)) {
            return new Cons<>(head, tail.filter(f));
        } else {
            return tail.filter(f);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cons<?> cons = (Cons<?>) o;
        return head.equals(cons.head) && tail.equals(cons.tail);
    }

    @Override
    public int hashCode() {
        var cur = 0;
        cur = 31 * cur + head.hashCode();
        cur = 31 * cur + tail.hashCode();
        return cur;
    }

    @Override
    public void toString(StringBuilder builder) {
        builder.append("{");
        builder.append(head.toString());
        builder.append(", ");
        tail.toString(builder);
        builder.append("}");
    }

    @Override
    public String toString() {
        return "Cons{" + head + ", " + tail + "}";
    }
}
