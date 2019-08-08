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
        parseStopPlaces(frame.getStopPlaces().getStopPlace());

        // Keep list sorted alphabetically
        logUnknownElement(LOG, frame.getAccesses());
        logUnknownElement(LOG, frame.getAddresses());
        logUnknownElement(LOG, frame.getCountries());
        logUnknownElement(LOG, frame.getCheckConstraints());
        logUnknownElement(LOG, frame.getCheckConstraintDelays());
        logUnknownElement(LOG, frame.getCheckConstraintThroughputs());
        logUnknownElement(LOG, frame.getGroupsOfStopPlaces());
        logUnknownElement(LOG, frame.getFlexibleStopPlaces());
        logUnknownElement(LOG, frame.getNavigationPaths());
        logUnknownElement(LOG, frame.getParkings());
        logUnknownElement(LOG, frame.getPathJunctions());
        logUnknownElement(LOG, frame.getPathLinks());
        logUnknownElement(LOG, frame.getPointsOfInterest());
        logUnknownElement(LOG, frame.getPointOfInterestClassifications());
        logUnknownElement(LOG, frame.getPointOfInterestClassificationHierarchies());
        logUnknownElement(LOG, frame.getSiteFacilitySets());
        logUnknownElement(LOG, frame.getTariffZones());
        logUnknownElement(LOG, frame.getTopographicPlaces());

        checkCommonProperties(LOG, frame);
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
