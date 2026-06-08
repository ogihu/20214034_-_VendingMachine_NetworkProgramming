package vending.event;

import vending.protocol.VendingMessage;

// 전송큐기능
public class NetworkEventQueue {

    private static class Node {
        VendingMessage data;
        Node next;
    }

    private Node front;
    private Node rear;
    private int size;

    public synchronized void enqueue(VendingMessage message) {
        // 큐 삽입
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

        // 큐 제거
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
