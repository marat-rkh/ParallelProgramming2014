package app;

import app.lists.BlockingList;
import app.lists.LockFreeList;
import app.lists.ThreadSafeList;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        if(args.length != 4) {
            System.out.println(
                    "4 arguments are expected: <readers num> <writers num> <operations num> <list type>\n" +
                    "where <list type> is 0 for 'blocking' and any other value for 'lock-free'"
            );
            return;
        }
        try {
            int readersNum = Integer.parseInt(args[0]);
            int writersNum = Integer.parseInt(args[1]);
            int operationsNum = Integer.parseInt(args[2]);
            int listType = Integer.parseInt(args[3]);

            ThreadSafeList targetList = listType == 0 ? new BlockingList() : new LockFreeList();

            List<Thread> threads = new LinkedList<Thread>();
            for(int i = 0; i < readersNum; i++) {
                threads.add(new Thread(new ReadTask(targetList, operationsNum)));
            }
            for(int i = 0; i < writersNum; i++) {
                threads.add(new Thread(new WriteTask(targetList, operationsNum)));
            }
            for (Thread t : threads) {
                t.start();
            }
            for (Thread t : threads) {
                t.join();
            }
        } catch (Exception e) {
            System.out.println("error: " + e.getMessage());
        }
    }

    private static abstract class Task implements Runnable {
        protected final ThreadSafeList list;
        protected final int operationsNum;
        protected final Random gen = new Random();

        public Task(ThreadSafeList list, int operationsNum) {
            this.list = list;
            this.operationsNum = operationsNum;
        }
    }

    private static class ReadTask extends Task {
        public ReadTask(ThreadSafeList list, int operationsNum) {
            super(list, operationsNum);
        }

        @Override
        public void run() {
            for (int i = 0; i < operationsNum; i++) {
                list.contains(gen.nextInt(Integer.MAX_VALUE));
            }
        }
    }

    private static class WriteTask extends Task {
        public WriteTask(ThreadSafeList list, int operationsNum) {
            super(list, operationsNum);
        }

        @Override
        public void run() {
            for(int i = 0; i < operationsNum; i++) {
                if (gen.nextBoolean()) {
                    list.insert(gen.nextInt(Integer.MAX_VALUE));
                } else {
                    list.erase(gen.nextInt(Integer.MAX_VALUE));
                }
            }
        }
    }

}