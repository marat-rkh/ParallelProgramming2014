package pool;

public class TasksProvider {
    private Task nextTask = null;

    public boolean hasTask() { return nextTask != null; }

    public Task getTask() {
        Task task = nextTask;
        nextTask = null;
        return task;
    }

    public void setNextTask(long id, int seconds) {
        nextTask = new Task(id, seconds);
    }
}
