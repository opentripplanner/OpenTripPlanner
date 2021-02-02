package fi.metatavu.airquality;

import ucar.ma2.ArrayFloat;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Map;

/**
 * Abstract grid data description class which is stored at StreetEdge.
 *
 * @author Katja Danilova
 */
public class EdgeDataFromGenericFile implements Serializable {
    private final long dataStartTime;
    private final Map<String, float[]> variableValues;

    /**
     * Sets the abstract grid data
     *
     * @param dataStartTime the time when the grid records start
     * @param variableValues map of variable names and arrays of their values
     */
    public EdgeDataFromGenericFile(long dataStartTime, Map<String, float[]> variableValues) {
        this.dataStartTime = dataStartTime;
        this.variableValues = variableValues;
    }

    /**
     * Gets a start time of a generic dataset
     *
     * @return data start time
     */
    public long getDataStartTime() {
        return dataStartTime;
    }

    /**
     * Gets the map of variable names with their values
     *
     * @return map of variables with values
     */
    public Map<String, float[]> getVariableValues() {
        return variableValues;
    }
}
