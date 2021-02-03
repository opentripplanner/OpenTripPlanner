package fi.metatavu.airquality.configuration_parsing;

/**
 * This class describes the variables for the incoming .nc data file
 */
public class IndexVariable {
    private String name;
    public String variable;
    private DataType dataType;

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets variable.
     *
     * @return the variable
     */
    public String getVariable() {
        return variable;
    }

    /**
     * Sets variable.
     *
     * @param variable the variable
     */
    public void setVariable(String variable) {
        this.variable = variable;
    }

    /**
     * Gets data type.
     *
     * @return the data type
     */
    public DataType getDataType() {
        return dataType;
    }

    /**
     * Sets data type.
     *
     * @param dataType the data type
     */
    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

}

/**
 * Type of index variable
 */
enum DataType {
    DOUBLE,
    INTEGER
}

