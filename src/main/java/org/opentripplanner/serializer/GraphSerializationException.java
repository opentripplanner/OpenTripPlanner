package org.opentripplanner.serializer;

public class GraphSerializationException extends RuntimeException {

    public GraphSerializationException(String message, Exception e) {
        super(message,e);
    }

    public GraphSerializationException(String message) {
        super(message);
    }
}
