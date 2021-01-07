package fi.metatavu.airquality.configuration_parsing;

public class IndexVariables {
    private String name;
    public String variable;
    private DataType dataType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVariable() {
        return variable;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

}
enum DataType {
    DOUBLE,
    INTEGER
}

