package org.opentripplanner.ext.dataOverlay.configuration;

/**
 * This class describes the variables for the incoming .nc data file
 */
public class IndexVariable {

    private String name;
    private String displayName;
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
     * Gets display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
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
     * Sets display name.
     *
     * @param displayName the display name
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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