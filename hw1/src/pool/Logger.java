package pool;

import java.util.Scanner;

public class Logger {
    private final String afterMessage;

    public Logger(String afterMessage) {
        this.afterMessage = afterMessage;
    }

    public void log(String msg) {
        System.out.print(msg);
        System.out.print(afterMessage);
    }
}
