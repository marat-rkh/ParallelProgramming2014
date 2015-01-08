package app.lists;

public class BlockingList {
    private Node head = null;

    public synchronized Node find(int key) {
        NodesPair pair = upperBound(key);
        if(pair.node != null && pair.node.key == key) {
            return pair.node;
        }
        return null;
    }

    public synchronized boolean insert(int key) {
        NodesPair pair = upperBound(key);
        if(pair.node != null && pair.node.key == key) {
            return false;
        }
        if(pair.prev == null) {
            head = new Node(key, head);
        } else {
            pair.prev.next = new Node(key, pair.node);
        }
        return true;
    }

    public synchronized boolean erase(int key) {
        NodesPair pair = upperBound(key);
        if(pair.node == null || pair.node.key != key) {
            return false;
        }
        if(pair.prev == null) {
            head = pair.node.next;
        } else {
            pair.prev.next = pair.node.next;
        }
        return true;
    }

    @Override
    public synchronized String toString() {
        StringBuilder builder = new StringBuilder("[");
        Node curr = head;
        while (curr != null) {
            builder.append(curr.key).append(' ');
            curr = curr.next;
        }
        builder.append("]");
        return builder.toString();
    }

    private NodesPair upperBound(int key) {
        Node prev = null;
        Node curr = head;
        while (curr != null && key > curr.key) {
            prev = curr;
            curr = curr.next;
        }
        return new NodesPair(prev, curr);
    }

    private class Node {
        int key;
        Node next;

        public Node(int key, Node next) {
            this.key = key;
            this.next = next;
        }
    }

    private class NodesPair {
        public Node node;
        public Node prev;

        public NodesPair(Node prev, Node node) {
            this.node = node;
            this.prev = prev;
        }
    }
}
