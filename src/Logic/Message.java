package Logic;

public class Message {
    private String operation;
    private String key;
    private String value;

    // Constructor
    public Message(String operation, String key, String value) {
        this.operation = operation;
        this.key = key;
        this.value = value;
    }

    // Getters
    public String getOperation() {
        return operation;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    // Setters
    public void setOperation(String operation) {
        this.operation = operation;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
