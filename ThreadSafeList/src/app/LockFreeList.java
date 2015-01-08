package app;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class LockFreeList implements ThreadSafeList {
    private Node tail = new Node(Integer.MAX_VALUE, null);
    private Node head = new Node(Integer.MIN_VALUE, new AtomicMarkableReference<Node>(tail, false));

    public boolean contains(int key) {
        boolean[] markHolder = { false };
        Node curr = head;
        while (curr.key < key) {
            curr = curr.next.getReference();
            if(curr == tail) {
                markHolder[0] = false;
            } else {
                curr.next.get(markHolder);
            }
        }
        return (curr.key == key && !markHolder[0]);
    }

    public boolean insert(int key) {
        Pair<Node> bounds;
        Node pred;
        Node curr;
        Node newNode;
        while (true) {
            bounds = getBounds(key);
            pred = bounds.fst;
            curr = bounds.snd;
            if (curr.key == key) {
                return false;
            } else {
                newNode = new Node(key, new AtomicMarkableReference<Node>(curr, false));
                if (pred.next.compareAndSet(curr, newNode, false, false)) {
                    return true;
                }
            }
        }
    }

    public boolean erase(int key) {
        Pair<Node> bounds;
        Node pred;
        Node curr;
        Node succ;
        boolean logicalRemoveSucceeded;
        while (true) {
            bounds = getBounds(key);
            pred = bounds.fst;
            curr = bounds.snd;
            if (curr.key != key) {
                return false;
            } else {
                succ = curr.next.getReference();
                logicalRemoveSucceeded = curr.next.attemptMark(succ, true);
                if (!logicalRemoveSucceeded) { continue; }
                pred.next.compareAndSet(curr, succ, false, false);
                return true;
            }
        }
    }

    private Pair<Node> getBounds(int key) {
        Node pred;
        Node curr;
        Node succ;
        boolean[] markHolder = { false };
        boolean physicalRemoveSucceeded;
        retry: while (true) {
            pred = head;
            curr = pred.next.getReference();
            while(true) {
                if(curr == tail) {
                    succ = null;
                    markHolder[0] = false;
                } else {
                    succ = curr.next.get(markHolder);
                }
                while(markHolder[0]) {
                    physicalRemoveSucceeded = pred.next.compareAndSet(curr, succ, false, false);
                    if(!physicalRemoveSucceeded) {
                        continue retry;
                    }
                    curr = succ;
                    if(curr == tail) {
                        succ = null;
                        markHolder[0] = false;
                    } else {
                        succ = curr.next.get(markHolder);
                    }
                }
                if(curr.key >= key) {
                    return new Pair<Node>(pred, curr);
                }
                pred = curr;
                curr = succ;
            }
        }
    }

    @Override
    public synchronized String toString() {
        StringBuilder builder = new StringBuilder("[");
        Node curr = head.next.getReference();
        while (curr != tail) {
            builder.append(curr.key).append(' ');
            curr = curr.next.getReference();
        }
        builder.append("]");
        return builder.toString();
    }

    private class Node {
        int key;
        AtomicMarkableReference<Node> next;

        public Node(int key, AtomicMarkableReference<Node> next) {
            this.key = key;
            this.next = next;
        }
    }
}
