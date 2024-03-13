package nora.vm.runtime;

import nora.vm.loading.Loader;

public class Frame {
    final Object[] stack;
    final Object[] arguments;
    final Object[] captures;

    final Loader loader;

    public Frame(int stackSize, Object[] arguments, Object[] captures, Loader loader) {
        this.stack = new Object[stackSize];
        this.arguments = arguments;
        this.loader = loader;
        this.captures = captures;
    }

    public Frame(int stackSize, Object[] arguments, Loader loader) {
        this(stackSize, arguments, null, loader);
    }

    public record FrameSlot(int index){
        @Override
        public int index() {
            return index;
        }
    }

    public Object getValue(FrameSlot slot){
        return stack[slot.index];
    }

    public Object getCapture(int i){
        return captures[i];
    }

    public Object getArgument(int i){
        return arguments[i];
    }

    public Loader getLoader() {
        return loader;
    }

    public void setValue(FrameSlot slot, Object value){
        stack[slot.index] = value;
    }
}
