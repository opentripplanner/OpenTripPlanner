package fi.metatavu.airquality.configuration_parsing;

/**
 * This class describes the variables for the incoming .nc data file
 */
public class IndexVariable {
    private String name;
    private String variable;

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

}

