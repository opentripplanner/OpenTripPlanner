package org.opentripplanner.ext.dataOverlay;

import java.io.Serializable;
import java.util.Map;
import org.opentripplanner.ext.dataOverlay.configuration.TimeUnit;

/**
 * Abstract grid data description class which is stored at StreetEdge.
 *
 * @author Katja Danilova
 */
public class EdgeDataFromGenericFile implements Serializable {

    private final long dataStartTime;
    private final Map<String, float[]> variableValues;
    private final TimeUnit timeUnit;

    /**
     * Sets the abstract grid data
     *
     * @param dataStartTime  the time when the grid records start
     * @param variableValues map of variable names and arrays of their values
     * @param timeUnit       time unit of the data overlay
     */
    public EdgeDataFromGenericFile(
            long dataStartTime,
            Map<String, float[]> variableValues,
            TimeUnit timeUnit
    ) {
        this.dataStartTime = dataStartTime;
        this.variableValues = variableValues;
        this.timeUnit = timeUnit;
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

    /**
     * Gets time format of the data
     *
     * @return time format
     */
    public TimeUnit getTimeFormat() {
        return timeUnit;
    }

}
