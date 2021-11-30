package org.opentripplanner.ext.legacygraphqlapi.model;

/**
 * Class for unknown entities that contains no information.
 */
public class LegacyGraphQLUnknown {

    /**
     * Description of the entity
     */
    private final String description;

    public LegacyGraphQLUnknown(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
