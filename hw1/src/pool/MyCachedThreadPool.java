package pool;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents cached thread pool parametrised by number of hot threads and timeout
 * before cold thread is finished.
 * Note that hot threads are not fixed: when hot thread starts task execution it becomes cold.
 * Upon thread completes task execution it signals MyCachedThreadPool and if the current
 * number of hot threads is less than maximum number MyCachedThreadPool marks that thread as hot.
 */
public class MyCachedThreadPool {
    private final TasksProvider tasksProvider = new TasksProvider();
    private Map<Long, Worker> workersMap = new ConcurrentHashMap<>();
    private Map<Long, Long> tasksMap = new ConcurrentHashMap<>();
    private final ThreadEventsHandler eventsHandler = new ThreadPoolEventsHandler();

    private final int HOT_THREADS_NUMBER;
    private final AtomicInteger currentHotThreadsNumber;
    private final int THREADS_TIMEOUT;

    private long lastTaskId = 0;
    private long lastExecutorId = 0;

    private final Logger logger;

    private boolean isClosed = false;

    public MyCachedThreadPool(int hotThreadsNumber, int timeoutSeconds, Logger logger) {
        this.HOT_THREADS_NUMBER = hotThreadsNumber;
        this.currentHotThreadsNumber = new AtomicInteger(HOT_THREADS_NUMBER);
        this.THREADS_TIMEOUT = timeoutSeconds;
        this.logger = logger;
        for(int i = 0; i < HOT_THREADS_NUMBER; i++) {
            startNewExecutorThread(/*isHot=*/true);
        }
    }

    /**
     * Adds new task to thread and returns task's ID
     *
     * @param lengthInSeconds task duration
     * @return created task's ID
     * @throws Exception if this method is called after closing thread pool
     */
    public long addTask(int lengthInSeconds) throws Exception {
        if(isClosed) {
            throw new Exception("thread pool is closed");
        }
        synchronized (tasksProvider) {
            tasksProvider.setNextTask(lastTaskId, lengthInSeconds);
            if(currentHotThreadsNumber.get() != 0) {
                tasksProvider.notify();
            } else {
                startNewExecutorThread(/*isHot=*/false);
            }
        }
        return lastTaskId ++;
    }

    /**
     * Interrupts active task with passed ID if presents
     *
     * @param taskId ID of the task to be interrupted
     * @return true if task was successfully interrupted, false if there is no
     * task with passed ID
     * @throws Exception if this method is called after closing thread pool
     */
    public boolean removeTask(long taskId) throws Exception {
        if(isClosed) {
            throw new Exception("thread pool is closed");
        }
        Long workerId = tasksMap.get(taskId);
        if(workerId != null) {
            Worker worker = workersMap.get(workerId);
            worker.getThread().interrupt();
            return true;
        }
        return false;
    }

    /**
     * Waits all active tasks to complete and shutdowns this thread pool
     *
     * @throws InterruptedException if interrupted during waiting tasks to complete
     */
    public void shutdown() throws InterruptedException {
        if(!isClosed) {
            synchronized (tasksProvider) {
                for (Map.Entry<Long, Worker> entry : workersMap.entrySet()) {
                    entry.getValue().getTaskExecutor().shutdown();
                }
                for (Map.Entry<Long, Worker> entry : workersMap.entrySet()) {
                    tasksProvider.notify();
                }
            }
            for (Map.Entry<Long, Worker> entry : workersMap.entrySet()) {
                entry.getValue().getThread().join();
            }
            isClosed = true;
        }
    }

    private class ThreadPoolEventsHandler implements ThreadEventsHandler {
        @Override
        public void threadEntersTask(long taskId, long executorId) {
            tasksMap.put(taskId, executorId);
            TaskExecutor executor = workersMap.get(executorId).getTaskExecutor();
            if(executor.isHot()) {
                int oldVal = currentHotThreadsNumber.get();
                while (!currentHotThreadsNumber.compareAndSet(oldVal, oldVal - 1)) {
                    oldVal = currentHotThreadsNumber.get();
                }
                executor.makeCold();
            }
        }

        @Override
        public synchronized void threadFinishedTask(Task finishedTask) {
            if(finishedTask.isDone()) {
                logIfNeeded("\ntask " + finishedTask.getID() + " is done\n");
            }
            Long workerId = tasksMap.remove(finishedTask.getID());
            TaskExecutor executor = workersMap.get(workerId).getTaskExecutor();
            if (currentHotThreadsNumber.get() == HOT_THREADS_NUMBER) {
                executor.shutdown();
                workersMap.remove(workerId);
            } else {
                int oldVal = currentHotThreadsNumber.get();
                while (!currentHotThreadsNumber.compareAndSet(oldVal, oldVal + 1)) {
                    oldVal = currentHotThreadsNumber.get();
                }
                executor.makeHot();
            }
        }

        @Override
        public void threadExitsOnTimeout(long executorId) {
            workersMap.remove(executorId);
        }
    }

    private void startNewExecutorThread(boolean isHot) {
        TaskExecutor newExecutor = new TaskExecutor(lastExecutorId, isHot,
                                       THREADS_TIMEOUT, tasksProvider, eventsHandler);
        Thread newThread = new Thread(newExecutor);
        workersMap.put(lastExecutorId, new Worker(newThread, newExecutor));
        newThread.start();
        ++lastExecutorId;
    }

    private void logIfNeeded(String msg) {
        if(logger != null) {
            logger.log(msg);
        }
    }

    private class Worker {
        private final Thread thread;
        private final TaskExecutor taskExecutor;

        private Worker(Thread thread, TaskExecutor taskExecutor) {
            this.thread = thread;
            this.taskExecutor = taskExecutor;
        }

        public Thread getThread() { return thread; }

        public TaskExecutor getTaskExecutor() { return taskExecutor; }
    }
}
