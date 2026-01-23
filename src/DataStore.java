
import java.util.concurrent.ConcurrentHashMap;

public class DataStore<T> {
    
    private ConcurrentHashMap<String, String> hashMap;

    public DataStore() {
        this.hashMap = new ConcurrentHashMap<String, String>();
    }

    public String handler(String message) {
        Massege mappedMassege = mapMessage(message);

        switch (mappedMassege.getOperation()) {
            case "GET": return handleGet(mappedMassege);
            case "SET": return handleSet(mappedMassege);
            case "DEL": return handleDel(mappedMassege);
            default:
                return "Error";
        }
    }
    
    private Massege mapMessage(String message) {
        String temp[] = message.split(" ");
        if(temp.length == 2)
            return new Massege(temp[0], temp[1], null);
        return new Massege(temp[0], temp[1], temp[2]);
    }

    private String handleSet(Massege mappedMassege) {
        hashMap.put(mappedMassege.getKey(), mappedMassege.getValue());
        return "OK";
    }

    private String handleGet(Massege mappedMassege) {
        String value = hashMap.get(mappedMassege.getKey());
        if (value == null) {
            return "NOT FOUND";
        }
        return value;
    }

    private String handleDel(Massege mappedMassege) {
        String removed = hashMap.remove(mappedMassege.getKey());
        if(removed != null)
            return "1";
        
        return "0";
    }
}
