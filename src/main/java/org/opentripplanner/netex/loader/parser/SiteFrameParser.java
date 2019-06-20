package org.opentripplanner.netex.loader.parser;

import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.Quays_RelStructure;
import org.rutebanken.netex.model.SiteFrame;
import org.rutebanken.netex.model.StopPlace;

import java.util.ArrayList;
import java.util.Collection;

class SiteFrameParser {

    private final Collection<StopPlace> stopPlaces = new ArrayList<>();

    private final Collection<Quay> quays = new ArrayList<>();

    public void parse(SiteFrame sf) {
        parseStopPlaces(sf.getStopPlaces().getStopPlace());
    }

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
