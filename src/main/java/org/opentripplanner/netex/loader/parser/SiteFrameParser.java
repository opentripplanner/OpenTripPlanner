package org.opentripplanner.netex.loader.parser;

import org.opentripplanner.netex.loader.util.HierarchicalMultimapById;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.SiteFrame;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.StopPlacesInFrame_RelStructure;

import java.util.List;

public class SiteFrameParser {

    private final HierarchicalMultimapById<StopPlace> stopPlaceById = new HierarchicalMultimapById<>();

    private final HierarchicalMultimapById<Quay> quayById = new HierarchicalMultimapById<>();

    public void parse(SiteFrame sf) {
        StopPlacesInFrame_RelStructure stopPlaces = sf.getStopPlaces();
        List<StopPlace> stopPlaceList = stopPlaces.getStopPlace();
        for (StopPlace stopPlace : stopPlaceList) {
            stopPlaceById.add(stopPlace);
            if (stopPlace.getQuays() != null) {
                List<Object> quayRefOrQuay = stopPlace.getQuays().getQuayRefOrQuay();
                for (Object quayObject : quayRefOrQuay) {
                    if (quayObject instanceof Quay) {
                        Quay quay = (Quay) quayObject;
                        quayById.add(quay);
                    }
                }
            }
        }
    }

    public HierarchicalMultimapById<StopPlace> getStopPlaceById() {
        return stopPlaceById;
    }

    public HierarchicalMultimapById<Quay> getQuayById() {
        return quayById;
    }
}
