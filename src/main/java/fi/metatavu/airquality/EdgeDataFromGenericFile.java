package fi.metatavu.airquality;

import ucar.ma2.ArrayFloat;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Map;

/**
 * this class is to be stored in StreetEdge and to describe the data it received from one of the .nc files
 * according to *settings.json configuration
 */
public class EdgeDataFromGenericFile implements Serializable {
    private final long dataStartTime;
    private final Map<String, float[]> variableValues;

    public EdgeDataFromGenericFile(long dataStartTime, Map<String, float[]> variableValues) {
        this.dataStartTime = dataStartTime;
        this.variableValues = variableValues;
    }

    public long getDataStartTime() {
        return dataStartTime;
    }

    public Map<String, float[]> getVariableValues() {
        return variableValues;
    }
}
