package app;

public class BlockingList implements ThreadSafeList {
    private Node tail = new Node(Integer.MAX_VALUE, null);
    private Node head = new Node(Integer.MIN_VALUE, tail);

    public synchronized boolean contains(int key) {
        Pair<Node> bounds = getBounds(key);
        return bounds.snd.key == key;
    }

    public synchronized boolean insert(int key) {
        Pair<Node> bounds = getBounds(key);
        Node lower = bounds.fst;
        Node upper = bounds.snd;
        if(upper.key == key) {
            return false;
        }
        lower.next = new Node(key, upper);
        return true;
    }

    public synchronized boolean erase(int key) {
        Pair<Node> bounds = getBounds(key);
        Node lower = bounds.fst;
        Node upper = bounds.snd;
        if(upper.key != key) {
            return false;
        }
        lower.next = upper.next;
        return true;
    }

    @Override
    public synchronized String toString() {
        StringBuilder builder = new StringBuilder("[");
        Node curr = head.next;
        while (curr != tail) {
            builder.append(curr.key).append(' ');
            curr = curr.next;
        }
        builder.append("]");
        return builder.toString();
    }

    private Pair<Node> getBounds(int key) {
        Node prev = head;
        Node curr = prev.next;
        while (curr != tail && curr.key < key) {
            prev = curr;
            curr = curr.next;
        }
        return new Pair<Node>(prev, curr);
    }

    private class Node {
        int key;
        Node next;

        public Node(int key, Node next) {
            this.key = key;
            this.next = next;
        }
    }
}
