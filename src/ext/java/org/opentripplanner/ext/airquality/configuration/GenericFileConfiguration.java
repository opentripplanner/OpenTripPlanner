package org.opentripplanner.ext.airquality.configuration;

/**
 * POJO class describing expected settings.json structure
 *
 * @author Katja Danilova
 */
public class GenericFileConfiguration {
    private String fileName;
    private String latitudeVariable;
    private String longitudeVariable;
    private String timeVariable;
    private TimeUnit timeFormat;
    private IndexVariable[] indexVariables;
    private RequestParameters[] requestParameters;

    /**
     * Gets file path to the .nc data file
     *
     * @return path to the .nc data file
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Sets file name.
     *
     * @param fileName the file name
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Gets latitude variable.
     *
     * @return the latitude variable
     */
    public String getLatitudeVariable() {
        return latitudeVariable;
    }

    /**
     * Sets latitude variable.
     *
     * @param latitudeVariable the latitude variable
     */
    public void setLatitudeVariable(String latitudeVariable) {
        this.latitudeVariable = latitudeVariable;
    }

    /**
     * Gets longitude variable.
     *
     * @return the longitude variable
     */
    public String getLongitudeVariable() {
        return longitudeVariable;
    }

    /**
     * Sets longitude variable.
     *
     * @param longitudeVariable the longitude variable
     */
    public void setLongitudeVariable(String longitudeVariable) {
        this.longitudeVariable = longitudeVariable;
    }

    /**
     * Gets time variable.
     *
     * @return the time variable
     */
    public String getTimeVariable() {
        return timeVariable;
    }

    /**
     * Sets time variable.
     *
     * @param timeVariable the time variable
     */
    public void setTimeVariable(String timeVariable) {
        this.timeVariable = timeVariable;
    }

    /**
     * Get index variables index variable [ ].
     *
     * @return the index variable [ ]
     */
    public IndexVariable[] getIndexVariables() {
        return indexVariables;
    }

    /**
     * Sets index variables.
     *
     * @param indexVariables the index variables
     */
    public void setIndexVariables(IndexVariable[] indexVariables) {
        this.indexVariables = indexVariables;
    }

    /**
     * Get request parameters request parameters [ ].
     *
     * @return the request parameters [ ]
     */
    public RequestParameters[] getRequestParameters() {
        return requestParameters;
    }

    /**
     * Sets request parameters.
     *
     * @param requestParameters the request parameters
     */
    public void setRequestParameters(RequestParameters[] requestParameters) {
        this.requestParameters = requestParameters;
    }

    /**
     * Gets time format
     *
     * @return the time format enum
     */
    public TimeUnit getTimeFormat() {
        return timeFormat;
    }

    /**
     * Sets time format
     *
     * @param timeFormat time format enum
     */
    public void setTimeFormat(TimeUnit timeFormat) {
        this.timeFormat = timeFormat;
    }
}
