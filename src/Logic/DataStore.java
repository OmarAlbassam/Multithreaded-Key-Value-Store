package Logic;

import java.util.HashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DataStore {

    public static final int NUM_SHARDS = 10; // made it static so its printed in the test
    private final HashMap<String, String>[] shards;
    private final ReadWriteLock[] locks;

    public DataStore() {
        shards = new HashMap[NUM_SHARDS];
        locks = new ReadWriteLock[NUM_SHARDS];
        for (int i = 0; i < NUM_SHARDS; i++) {
            shards[i] = new HashMap<>();
            locks[i] = new ReentrantReadWriteLock();
        }
    }

    public String handler(String message) {
        Message mappedMessage = mapMessage(message);

        switch (mappedMessage.getOperation().toUpperCase()) {
            case "GET":
                return handleGet(mappedMessage);
            case "SET":
                return handleSet(mappedMessage);
            case "DEL":
                return handleDel(mappedMessage);
            default:
                return "Error";
        }
    }

    private Message mapMessage(String message) {
        String temp[] = message.split(" ");
        if (temp.length == 2) {
            return new Message(temp[0], temp[1], null);
        }
        return new Message(temp[0], temp[1], temp[2]);
    }

    private String handleSet(Message mappedMessage) {
        // to get which hashmap - hashcode the key -> module with number of shards -> hashmap array index
        int index = Math.abs(mappedMessage.getKey().hashCode()) % NUM_SHARDS;
        try {
            locks[index].writeLock().lock();
            shards[index].put(mappedMessage.getKey(), mappedMessage.getValue());
        } finally {
            locks[index].writeLock().unlock();
        }
        return "OK";
    }

    private String handleGet(Message mappedMessage) {
        // to get which hashmap - hashcode the key -> module with number of shards -> hashmap array index
        int index = Math.abs(mappedMessage.getKey().hashCode()) % NUM_SHARDS;
        String value;
        try {
            locks[index].readLock().lock();
            value = shards[index].get(mappedMessage.getKey());
            if (value == null) {
                return "NOT FOUND";
            }
        } finally {
            locks[index].readLock().unlock();
        }
        return value;
    }

    private String handleDel(Message mappedMessage) {
        // to get which hashmap - hashcode the key -> module with number of shards -> hashmap array index
        int index = Math.abs(mappedMessage.getKey().hashCode()) % NUM_SHARDS;

        try {
        locks[index].writeLock().lock();
        String removed = shards[index].remove(mappedMessage.getKey());
        if (removed != null) {
            return "Deleted!";
        }

        } finally {
            locks[index].writeLock().unlock();
        }
        
        return "Failed To Delete";
    }
}
