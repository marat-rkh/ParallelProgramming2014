package pool;

public class Logger {
    private final String afterMessage;
    private final boolean DEBUG_ON = false;

    public Logger(String afterMessage) {
        this.afterMessage = afterMessage;
    }

    public synchronized void log(String msg) {
        System.out.print(msg);
        System.out.print(afterMessage);
    }

    public synchronized void debug(String msg) {
        if(DEBUG_ON) {
            log("\n#DEBUG> " + msg + "\n");
        }
    }
}
