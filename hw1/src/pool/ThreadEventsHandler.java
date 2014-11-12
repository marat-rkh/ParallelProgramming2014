package pool;

/**
 * Executor thread events handler. Contains callbacks for task processing
 * events that happen in executor thread
 */
public interface ThreadEventsHandler {
    public void threadEntersTask(long taskId, long executorId);
    public void threadFinishedTask(Task finishedTask);
    public void threadExitsOnTimeout(long executorId);
}
