
import java.util.HashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DataStore<T> {

    private final int NUM_SHARDS = 10; // decide to be number of cores - m1 pro -> 10 cores
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
        Massege mappedMassege = mapMessage(message);

        switch (mappedMassege.getOperation().toUpperCase()) {
            case "GET":
                return handleGet(mappedMassege);
            case "SET":
                return handleSet(mappedMassege);
            case "DEL":
                return handleDel(mappedMassege);
            default:
                return "Error";
        }
    }

    private Massege mapMessage(String message) {
        String temp[] = message.split(" ");
        if (temp.length == 2) {
            return new Massege(temp[0], temp[1], null);
        }
        return new Massege(temp[0], temp[1], temp[2]);
    }

    private String handleSet(Massege mappedMassege) {
        // to get which hashmap - hashcode the key -> module with number of shards -> hashmap array index
        int index = Math.abs(mappedMassege.getKey().hashCode()) % NUM_SHARDS;
        try {
            locks[index].writeLock().lock();
            shards[index].put(mappedMassege.getKey(), mappedMassege.getValue());
        } finally {
            locks[index].writeLock().unlock();
        }
        return "OK";
    }

    private String handleGet(Massege mappedMassege) {
        // to get which hashmap - hashcode the key -> module with number of shards -> hashmap array index
        int index = Math.abs(mappedMassege.getKey().hashCode()) % NUM_SHARDS;
        String value;
        try {
            locks[index].readLock().lock();
            value = shards[index].get(mappedMassege.getKey());
            if (value == null) {
                return "NOT FOUND";
            }
        } finally {
            locks[index].readLock().unlock();
        }
        return value;
    }

    private String handleDel(Massege mappedMassege) {
        // to get which hashmap - hashcode the key -> module with number of shards -> hashmap array index
        int index = Math.abs(mappedMassege.getKey().hashCode()) % NUM_SHARDS;

        try {
        locks[index].writeLock().lock();
        String removed = shards[index].remove(mappedMassege.getKey());
        if (removed != null) {
            return "Deleted!";
        }

        } finally {
            locks[index].writeLock().unlock();
        }
        
        return "Failed To Delete";
    }
}
