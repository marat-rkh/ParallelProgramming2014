package app.lists;

public interface ThreadSafeList {
    public boolean contains(int key);
    public boolean insert(int key);
    public boolean erase(int key);
}
