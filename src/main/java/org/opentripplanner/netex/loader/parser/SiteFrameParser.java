package org.opentripplanner.netex.loader.parser;

import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.Quays_RelStructure;
import org.rutebanken.netex.model.Site_VersionFrameStructure;
import org.rutebanken.netex.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

class SiteFrameParser extends NetexParser<Site_VersionFrameStructure> {
    private static final Logger LOG = LoggerFactory.getLogger(NetexParser.class);

    private final Collection<StopPlace> stopPlaces = new ArrayList<>();

    private final Collection<Quay> quays = new ArrayList<>();

    @Override
    public void parse(Site_VersionFrameStructure frame) {
        if(frame.getStopPlaces() != null) {
            parseStopPlaces(frame.getStopPlaces().getStopPlace());
        }
        // Keep list sorted alphabetically
        warnOnMissingMapping(LOG, frame.getAccesses());
        warnOnMissingMapping(LOG, frame.getAddresses());
        warnOnMissingMapping(LOG, frame.getCountries());
        warnOnMissingMapping(LOG, frame.getCheckConstraints());
        warnOnMissingMapping(LOG, frame.getCheckConstraintDelays());
        warnOnMissingMapping(LOG, frame.getCheckConstraintThroughputs());
        warnOnMissingMapping(LOG, frame.getGroupsOfStopPlaces());
        warnOnMissingMapping(LOG, frame.getFlexibleStopPlaces());
        warnOnMissingMapping(LOG, frame.getNavigationPaths());
        warnOnMissingMapping(LOG, frame.getParkings());
        warnOnMissingMapping(LOG, frame.getPathJunctions());
        warnOnMissingMapping(LOG, frame.getPathLinks());
        warnOnMissingMapping(LOG, frame.getPointsOfInterest());
        warnOnMissingMapping(LOG, frame.getPointOfInterestClassifications());
        warnOnMissingMapping(LOG, frame.getPointOfInterestClassificationHierarchies());
        warnOnMissingMapping(LOG, frame.getSiteFacilitySets());
        warnOnMissingMapping(LOG, frame.getTariffZones());
        warnOnMissingMapping(LOG, frame.getTopographicPlaces());

        verifyCommonUnusedPropertiesIsNotSet(LOG, frame);
    }

    @Override
    void setResultOnIndex(NetexImportDataIndex netexIndex) {
        netexIndex.stopPlaceById.addAll(stopPlaces);
        netexIndex.quayById.addAll(quays);
    }

    private void parseStopPlaces(Collection<StopPlace> stopPlaceList) {
        for (StopPlace stopPlace : stopPlaceList) {
            stopPlaces.add(stopPlace);
            parseQuays(stopPlace.getQuays());
        }
    }

    private void parseQuays(Quays_RelStructure quayRefOrQuay) {
        if(quayRefOrQuay == null) return;

        for (Object quayObject : quayRefOrQuay.getQuayRefOrQuay()) {
            if (quayObject instanceof Quay) {
                quays.add((Quay) quayObject);
            }
        }
    }
}
