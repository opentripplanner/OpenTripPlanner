package org.opentripplanner.model;

import java.io.Serializable;

public class KeyValue implements Serializable {

    private final String typeOfKey;

    private final String key;

    private final String value;

    public KeyValue(String typeOfKey, String key, String value) {
        this.typeOfKey = typeOfKey;
        this.key = key;
        this.value = value;
    }

    public String getTypeOfKey() {
        return typeOfKey;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
