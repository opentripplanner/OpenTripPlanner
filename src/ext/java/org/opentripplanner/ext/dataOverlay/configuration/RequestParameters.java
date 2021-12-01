package org.opentripplanner.ext.dataOverlay.configuration;

/**
 * This class describes the expected routing request parameters for the generic data
 */
public class RequestParameters {

    private String name;
    private String variable;
    private ParameterType parameterType;
    private String formula;
    private transient String value;

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
     * Gets parameter type.
     *
     * @return the parameter type
     */
    public ParameterType getParameterType() {
        return parameterType;
    }

    /**
     * Sets parameter type.
     *
     * @param parameterType the parameter type
     */
    public void setParameterType(ParameterType parameterType) {
        this.parameterType = parameterType;
    }

    /**
     * Gets formula.
     *
     * @return the formula
     */
    public String getFormula() {
        return formula;
    }

    /**
     * Sets formula.
     *
     * @param formula the formula
     */
    public void setFormula(String formula) {
        this.formula = formula;
    }

    /**
     * Gets value.
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets value.
     *
     * @param value the value
     */
    public void setValue(String value) {
        this.value = value;
    }
}
