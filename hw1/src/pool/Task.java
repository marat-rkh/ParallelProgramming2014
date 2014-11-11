package pool;

public class Task implements Runnable {
    private final long ID;
    private final long DURATION;
    private boolean isDone = false;

    public Task(long id, int durationSeconds) {
        this.ID = id;
        this.DURATION = durationSeconds * 1000;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(DURATION);
            isDone = true;
        } catch (InterruptedException e) {
            isDone = false;
        }
    }

    public long getID() { return ID; }

    public boolean isDone() { return isDone; }
}
