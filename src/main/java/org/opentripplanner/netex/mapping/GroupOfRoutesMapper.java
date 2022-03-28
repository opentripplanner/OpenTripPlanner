package org.opentripplanner.netex.mapping;


import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.GroupOfRoutes;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.rutebanken.netex.model.GroupOfLines;

/**
 * Responsible for mapping NeTEx GroupOfLines into the OTP model.
 */
public class GroupOfRoutesMapper {

    private final FeedScopedIdFactory idFactory;

    public GroupOfRoutesMapper(FeedScopedIdFactory idFactory) {
        this.idFactory = idFactory;
    }

    /**
     * Convert NeTEx GroupOfLines Entity into OTP model.
     *
     * @param groupOfLines NeTEx GroupOfLines entity.
     * @return OTP GroupOfRoutes model
     */
    public GroupOfRoutes mapGroupOfRoutes(GroupOfLines groupOfLines) {
        final FeedScopedId id = idFactory.createId(groupOfLines.getId());

        final String privateCode = groupOfLines.getPrivateCode() != null ?
                groupOfLines.getPrivateCode().getValue()
                : null;

        final String shortName = groupOfLines.getShortName() != null ?
                groupOfLines.getShortName().getValue()
                : null;

        final String name = groupOfLines.getName() != null ?
                groupOfLines.getName().getValue()
                : null;

        final String description = groupOfLines.getDescription() != null ?
                groupOfLines.getDescription().getValue()
                : null;

        return new GroupOfRoutes(id, privateCode, shortName, name, description);
    }
}
