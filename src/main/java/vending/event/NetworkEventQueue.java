package vending.event;

import vending.protocol.VendingMessage;

/**
 * 서버 전송 대기 메시지 큐.
 */
public class NetworkEventQueue {

    private static class Node {
        VendingMessage data;
        Node next;
    }

    private Node front;
    private Node rear;
    private int size;

    public synchronized void enqueue(VendingMessage message) {
        Node node = new Node();
        node.data = message;
        if (rear == null) {
            front = rear = node;
        } else {
            rear.next = node;
            rear = node;
        }
        size++;
        notifyAll();
    }

    public synchronized VendingMessage dequeue() throws InterruptedException {
        while (front == null) {
            wait(500);
            if (front == null) {
                return null;
            }
        }
        VendingMessage data = front.data;
        front = front.next;
        if (front == null) {
            rear = null;
        }
        size--;
        return data;
    }

    public synchronized int size() {
        return size;
    }
}
