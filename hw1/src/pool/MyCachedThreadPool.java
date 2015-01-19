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
    private final AtomicInteger readyThreadsNumber = new AtomicInteger(0);
    private final int THREADS_TIMEOUT;

    private long lastTaskId = 0;
    private long lastExecutorId = 0;

    private final Logger logger;

    private boolean isClosed = false;

    public MyCachedThreadPool(int hotThreadsNumber, int timeoutSeconds, Logger logger) {
        this.HOT_THREADS_NUMBER = hotThreadsNumber;
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
    public synchronized long addTask(int lengthInSeconds) throws Exception {
        if(isClosed) {
            throw new Exception("thread pool is closed");
        }
        synchronized (tasksProvider) {
            tasksProvider.setNextTask(lastTaskId, lengthInSeconds);
            logger.debug("ready threads num on add task: " + readyThreadsNumber.get());
            if(readyThreadsNumber.get() != 0) {
                logger.debug("existing thread is notified about task " + lastTaskId);
                tasksProvider.notify();
            } else {
                logger.debug("new thread is created for task " + lastTaskId);
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
    public synchronized boolean removeTask(long taskId) throws Exception {
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
    public synchronized void shutdown() throws InterruptedException {
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
            logger.debug("executor " + executorId + " enters task " + taskId);
            tasksMap.put(taskId, executorId);
            TaskExecutor executor = workersMap.get(executorId).getTaskExecutor();
            readyThreadsNumber.decrementAndGet();
            executor.makeCold();
        }

        @Override
        public synchronized void threadFinishedTask(Task finishedTask) {
            logger.debug("task " + finishedTask.getID() + " is finished");
            if(finishedTask.isDone()) {
                logIfNeeded("\ntask " + finishedTask.getID() + " is done\n");
            }
            Long workerId = tasksMap.remove(finishedTask.getID());
            TaskExecutor executor = workersMap.get(workerId).getTaskExecutor();
            int oldVal = readyThreadsNumber.getAndIncrement();
            if (oldVal < HOT_THREADS_NUMBER) {
                executor.makeHot();
            }
        }

        @Override
        public void threadExitsOnTimeout(long executorId) {
            logger.debug("executor " + executorId + " exits on timeout");
            readyThreadsNumber.decrementAndGet();
            workersMap.remove(executorId);
        }
    }

    private void startNewExecutorThread(boolean isHot) {
        TaskExecutor newExecutor = new TaskExecutor(lastExecutorId, isHot,
                                       THREADS_TIMEOUT, tasksProvider, eventsHandler);
        Thread newThread = new Thread(newExecutor);
        workersMap.put(lastExecutorId, new Worker(newThread, newExecutor));
        readyThreadsNumber.incrementAndGet();
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
