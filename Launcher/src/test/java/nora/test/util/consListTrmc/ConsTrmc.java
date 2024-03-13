package nora.test.util.consListTrmc;

import java.util.function.Function;

public class ConsTrmc<T> extends ConsListTrmc<T> {
    public final T head;
    public ConsListTrmc<T> tail;

    public ConsTrmc(T head, ConsListTrmc<T> tail) {
        this.head = head;
        this.tail = tail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConsTrmc<?> cons = (ConsTrmc<?>) o;
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
