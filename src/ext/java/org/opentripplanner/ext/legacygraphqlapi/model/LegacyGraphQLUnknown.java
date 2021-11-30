package org.opentripplanner.ext.legacygraphqlapi.model;

/**
 * Class for unknown entities. Either no entity was defined or an entity that we don't support yet
 * in that context.
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
