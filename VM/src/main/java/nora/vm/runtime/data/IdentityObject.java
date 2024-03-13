package nora.vm.runtime.data;

//Volatile Objects are never equal
public class IdentityObject {
    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }
}
