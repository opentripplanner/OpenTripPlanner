package org.opentripplanner.netex.loader.parser;

import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.Organisation_VersionStructure;
import org.rutebanken.netex.model.OrganisationsInFrame_RelStructure;
import org.rutebanken.netex.model.ResourceFrame_VersionFrameStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Collection;

class ResourceFrameParser extends NetexParser<ResourceFrame_VersionFrameStructure> {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceFrameParser.class);

    private final Collection<Authority> authorityById = new ArrayList<>();

    @Override
    void parse(ResourceFrame_VersionFrameStructure frame) {
        parseOrganization(frame.getOrganisations());

        // Keep list sorted alphabetically
        logUnknownElement(LOG, frame.getBlacklists());
        logUnknownElement(LOG, frame.getControlCentres());
        logUnknownElement(LOG, frame.getDataSources());
        logUnknownElement(LOG, frame.getEquipments());
        logUnknownElement(LOG, frame.getGroupsOfEntities());
        logUnknownElement(LOG, frame.getGroupsOfOperators());
        logUnknownElement(LOG, frame.getOperationalContexts());
        logUnknownElement(LOG, frame.getResponsibilitySets());
        logUnknownElement(LOG, frame.getSchematicMaps());
        logUnknownElement(LOG, frame.getTypesOfValue());
        logUnknownElement(LOG, frame.getVehicles());
        logUnknownElement(LOG, frame.getVehicleEquipmentProfiles());
        logUnknownElement(LOG, frame.getVehicleModels());
        logUnknownElement(LOG, frame.getVehicleTypes());
        logUnknownElement(LOG, frame.getWhitelists());
        logUnknownElement(LOG, frame.getZones());

        checkCommonProperties(LOG, frame);
    }

    @Override void setResultOnIndex(NetexImportDataIndex netexIndex) {
        netexIndex.authoritiesById.addAll(authorityById);
    }


    /* private methods */

    private void parseOrganization(OrganisationsInFrame_RelStructure elements) {
        for (JAXBElement e : elements.getOrganisation_()) {
            parseOrganization((Organisation_VersionStructure) e.getValue());
        }
    }

    private void parseOrganization(Organisation_VersionStructure element) {
        if (element instanceof Authority) {
            authorityById.add((Authority) element);
        } else {
            logUnknownObject(LOG, element);
        }
    }
}
