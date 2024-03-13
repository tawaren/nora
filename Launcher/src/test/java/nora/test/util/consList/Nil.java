package nora.util.consList;

import java.util.function.Function;

public class Nil<T> extends ConsList<T> {
    @Override
    public ConsList<T> map(Function<T, T> f) {
        return this;
    }

    @Override
    public ConsList<T> filter(Function<T, Boolean> f) {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public void toString(StringBuilder builder) {
        builder.append("{}");
    }

    @Override
    public String toString() {
        return "{}";
    }
}
