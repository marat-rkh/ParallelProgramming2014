package pool;

import java.io.IOException;
import java.util.Scanner;

public class Main {
    private static final String ADD_COMMAND_PREFIX = "a";
    private static final String RM_COMMAND_PREFIX = "r";
    private static final String QUIT_COMMAND = "q";

    public static void main(String[] args) {
        if(args.length < 2) {
            System.out.println("need 2 arguments: hot threads number and timeout");
            return;
        }
        Scanner consoleReader = new Scanner(System.in);
        MyCachedThreadPool threadPool = createThreadPool(args);
        if(threadPool == null) {
            System.out.println("bad arguments, both must be numeric");
            return;
        }
        try {
            registerSignalsHandler(threadPool);
            printHelp();
            startRepl(threadPool, consoleReader);
        } catch (IOException e) {
            System.out.println("IO error occurred");
        }
        System.out.println("waiting active tasks to complete");
        try {
            threadPool.shutdown();
        } catch (InterruptedException e) {
            System.out.println("resources releasing error, details: " + e.getMessage());
        }
    }

    private static MyCachedThreadPool createThreadPool(String[] args) {
        try {
            int hotThreadsNumber = Integer.parseInt(args[0]);
            int timeout = Integer.parseInt(args[1]);
            return new MyCachedThreadPool(hotThreadsNumber,
                   timeout, new Logger("command> "));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void printHelp() {
        System.out.println("Commands:\n" +
                            ADD_COMMAND_PREFIX + " <duration> - add task\n" +
                            RM_COMMAND_PREFIX + " <id> - remove task\n" +
                            QUIT_COMMAND + " - quit");
        System.out.println();
    }

    private static void startRepl(MyCachedThreadPool threadPool,
                                  Scanner consoleReader) throws IOException {
        String command = getCommand(consoleReader);
        while (!command.equals(QUIT_COMMAND)) {
            try {
                String[] cmdParts = command.split(" ");
                if (cmdParts.length == 2 && cmdParts[0].equals(ADD_COMMAND_PREFIX)) {
                    final long id = threadPool.addTask(Integer.parseInt(cmdParts[1]));
                    System.out.println("task " + id + " is accepted");
                } else if (cmdParts.length == 2 && cmdParts[0].equals(RM_COMMAND_PREFIX)) {
                    final long id = Long.parseLong(cmdParts[1]);
                    final boolean result = threadPool.removeTask(id);
                    if(result) {
                        System.out.println("task " + id + " is removed");
                    } else {
                        System.out.println("no task with id = " + id);
                    }
                } else if (!command.isEmpty()) {
                    System.out.println("unknown command");
                }
            } catch (NumberFormatException e) {
                System.out.println("argument must be numeric, try again");
            } catch (Exception e) {
                System.out.println("error: " + e.getMessage());
            }
            command = getCommand(consoleReader);
        }
    }

    private static void registerSignalsHandler(final MyCachedThreadPool threadPool) {
        Thread hook = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("releasing resources...");
                try {
                    threadPool.shutdown();
                    System.out.println("released successfully");
                } catch (InterruptedException e) {
                    System.out.println("resources releasing error, details: " + e.getMessage());
                }
            }
        });
        Runtime.getRuntime().addShutdownHook(hook);
    }

    private static String getCommand(Scanner consoleReader) throws IOException {
        System.out.print("command> ");
        return consoleReader.nextLine().trim().replaceAll("\\s+", " ");
    }
}
