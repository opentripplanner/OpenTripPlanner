package fi.metatavu.airquality.configuration_parsing;

public class GenericFileConfiguration {
    private String fileName;
    private String latitudeVariable;
    private String longitudeVariable;
    private String timeVariable;
    private IndexVariable[] indexVariables;
    private RequestParameters[] requestParameters;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getLatitudeVariable() {
        return latitudeVariable;
    }

    public void setLatitudeVariable(String latitudeVariable) {
        this.latitudeVariable = latitudeVariable;
    }

    public String getLongitudeVariable() {
        return longitudeVariable;
    }

    public void setLongitudeVariable(String longitudeVariable) {
        this.longitudeVariable = longitudeVariable;
    }

    public String getTimeVariable() {
        return timeVariable;
    }

    public void setTimeVariable(String timeVariable) {
        this.timeVariable = timeVariable;
    }

    public IndexVariable[] getIndexVariables() {
        return indexVariables;
    }

    public void setIndexVariables(IndexVariable[] indexVariables) {
        this.indexVariables = indexVariables;
    }

    public RequestParameters[] getRequestParameters() {
        return requestParameters;
    }

    public void setRequestParameters(RequestParameters[] requestParameters) {
        this.requestParameters = requestParameters;
    }
}
