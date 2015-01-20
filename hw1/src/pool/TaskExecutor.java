package pool;

/**
 * Executes tasks provided by TasksProvider instance passed to constructor.
 * Before executing task and after execution is finished TaskExecutor informs
 * ThreadEventsHandler instance passed to constructor about these evens by calling
 * appropriate methods
 *
 * @see pool.ThreadEventsHandler
 */
public class TaskExecutor implements Runnable {
    private final long ID;
    private boolean isHot;
    private final long EXIT_TIMEOUT;
    private final TasksProvider tasksProvider;
    private final ThreadEventsHandler threadEventsHandler;

    private boolean shutdownIsCalled = false;

    public TaskExecutor(long id, boolean isHot, int exitTimeoutSeconds,
                        TasksProvider tasksProvider, ThreadEventsHandler threadEventsHandler) {
        this.ID = id;
        this.isHot = isHot;
        this.EXIT_TIMEOUT = exitTimeoutSeconds * 1000;
        this.tasksProvider = tasksProvider;
        this.threadEventsHandler = threadEventsHandler;
    }

    public long getID() { return ID; }

    public void makeHot() { isHot = true; }
    public void makeCold() { isHot = false; }
    public boolean isHot() { return isHot; }

    @Override
    public void run() {
        try {
            while (!shutdownIsCalled) {
                Task task = waitTask();
                if(task == null) {
                    return;
                }
                task.run();
                threadEventsHandler.threadFinishedTask(task);
                // this is to clear the interrupted status
                Thread.interrupted();
            }
        } catch (InterruptedException e) {
            // exit
        }
    }

    public void shutdown() { shutdownIsCalled = true; }

    private Task waitTask() throws InterruptedException {
        synchronized (tasksProvider) {
            long exitTimeoutLeft;
            long startTime = System.currentTimeMillis();
            while (!tasksProvider.hasTask() && !shutdownIsCalled) {
                if (!isHot) {
                    long currentTime = System.currentTimeMillis();
                    if (exitTimeoutExceeded(startTime, currentTime)) {
                        threadEventsHandler.threadExitsOnTimeout(ID);
                        return null;
                    }
                    exitTimeoutLeft = EXIT_TIMEOUT - (currentTime - startTime);
                    tasksProvider.wait(exitTimeoutLeft);
                } else {
                    tasksProvider.wait();
                }
            }
            Task task = tasksProvider.getTask();
            if(task == null || shutdownIsCalled) {
                return null;
            }
            threadEventsHandler.threadEntersTask(task.getID(), ID);
            return task;
        }
    }

    private boolean exitTimeoutExceeded(long startTime, long currentTime) {
        return currentTime - startTime >= EXIT_TIMEOUT;
    }
}
