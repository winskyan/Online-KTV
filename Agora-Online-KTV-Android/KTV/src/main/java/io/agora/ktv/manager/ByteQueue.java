package io.agora.ktv.manager;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Queue;

public class ByteQueue {
    private final Queue<Byte> queue;
    private final int maxSize;

    public ByteQueue(int maxSize) {
        this.queue = new ArrayDeque<>();
        this.maxSize = maxSize;
    }

    public void enqueue(byte data) {
        if (queue.size() >= maxSize) {
            queue.poll(); // Remove the oldest data
        }
        queue.offer(data);
    }

    public Byte dequeue() {
        return queue.poll();
    }

    public int size() {
        return queue.size();
    }

    public byte[] getAllData() {
        byte[] data = new byte[queue.size()];
        Queue<Byte> tempQueue = new LinkedList<>(queue);

        int i = 0;
        while (!tempQueue.isEmpty()) {
            data[i++] = tempQueue.poll();
        }
        return data;
    }
}
