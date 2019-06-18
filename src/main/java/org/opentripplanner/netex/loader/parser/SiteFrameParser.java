package org.opentripplanner.netex.loader.parser;

import org.opentripplanner.netex.loader.util.HierarchicalMultimapById;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.SiteFrame;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.StopPlacesInFrame_RelStructure;

import java.util.Collection;

class SiteFrameParser {

    private final HierarchicalMultimapById<StopPlace> stopPlaceById = new HierarchicalMultimapById<>();

    private final HierarchicalMultimapById<Quay> quayById = new HierarchicalMultimapById<>();

    public void parse(SiteFrame sf) {
        StopPlacesInFrame_RelStructure stopPlaces = sf.getStopPlaces();
        Collection<StopPlace> stopPlaceList = stopPlaces.getStopPlace();
        for (StopPlace stopPlace : stopPlaceList) {
            stopPlaceById.add(stopPlace);
            if (stopPlace.getQuays() != null) {
                Collection<Object> quayRefOrQuay = stopPlace.getQuays().getQuayRefOrQuay();
                for (Object quayObject : quayRefOrQuay) {
                    if (quayObject instanceof Quay) {
                        Quay quay = (Quay) quayObject;
                        quayById.add(quay);
                    }
                }
            }
        }
    }

    HierarchicalMultimapById<StopPlace> getStopPlaceById() {
        return stopPlaceById;
    }

    HierarchicalMultimapById<Quay> getQuayById() {
        return quayById;
    }
}
