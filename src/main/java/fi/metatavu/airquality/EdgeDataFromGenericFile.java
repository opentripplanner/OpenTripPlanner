package fi.metatavu.airquality;

import ucar.ma2.ArrayFloat;

import java.lang.reflect.Array;
import java.util.Map;

/**
 * this class is to be stored in StreetEdge and to describe the data it received from one of the .nc files
 * according to *settings.json confguration
 */
public class EdgeDataFromGenericFile {
    private final String name;
    private final long dataStartTime;
    private final Map<String, float[]> variableValues;

    public EdgeDataFromGenericFile(String name, long dataStartTime, Map<String, float[]> variableValues) {
        this.name = name;
        this.dataStartTime = dataStartTime;
        this.variableValues = variableValues;
    }

    public String getName() {
        return name;
    }

    public long getDataStartTime() {
        return dataStartTime;
    }

    public Map<String, float[]> getVariableValues() {
        return variableValues;
    }
}
