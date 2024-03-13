package nora.test.util.consListTrmc;

public class NilTrmc<T> extends ConsListTrmc<T> {
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
