package green_green_avk.anotherterm.utils;

public final class BlockingSync<T> {
    private T mValue;
    private boolean mIsSet = false;

    public synchronized T get() throws InterruptedException {
        if (!mIsSet) wait();
        return mValue;
    }

    public synchronized boolean isSet() {
        return mIsSet;
    }

    public synchronized void set(final T value) {
        mValue = value;
        if (!mIsSet) notifyAll();
        mIsSet = true;
    }

    public synchronized void clear() {
        mIsSet = false;
        mValue = null;
    }
}
