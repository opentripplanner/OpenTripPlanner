package org.opentripplanner.netex.mapping;

import com.google.common.collect.Iterables;
import org.opentripplanner.model.Stop;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class StopMapper {
    private static final Logger LOG = LoggerFactory.getLogger(StopMapper.class);

    public Collection<Stop> mapParentAndChildStops(Collection<StopPlace> stopPlaceAllVersions){
        ArrayList<Stop> stops = new ArrayList<>();

        Stop stop = new Stop();
        stop.setLocationType(1);

        // Sort by versions, latest first
        stopPlaceAllVersions = stopPlaceAllVersions.stream()
                .sorted((o1, o2) -> Integer.compare(Integer.parseInt(o2.getVersion()), Integer.parseInt(o1.getVersion())))
                .collect(Collectors.toList());

        StopPlace stopPlaceLatest = Iterables.getLast(stopPlaceAllVersions);

        if(stopPlaceLatest.getCentroid() != null){
            stop.setLat(stopPlaceLatest.getCentroid().getLocation().getLatitude().doubleValue());
            stop.setLon(stopPlaceLatest.getCentroid().getLocation().getLongitude().doubleValue());
        }else{
            LOG.warn(stopPlaceLatest.getId() + " does not contain any coordinates.");
        }

        stop.setId(FeedScopedIdFactory.createAgencyAndId(stopPlaceLatest.getId()));
        stops.add(stop);

        // Get quays from all versions of stop place
        Set<String> quaysSeen = new HashSet<>();

        for (StopPlace stopPlace : stopPlaceAllVersions) {
            if (stopPlace.getQuays() != null) {
                List<Object> quayRefOrQuay = stopPlace.getQuays().getQuayRefOrQuay();
                for (Object quayObject : quayRefOrQuay) {
                    if (quayObject instanceof Quay) {
                        Quay quay = (Quay) quayObject;
                        Stop stopQuay = new Stop();
                        stopQuay.setLocationType(0);
                        if (quay.getCentroid() == null || quay.getCentroid().getLocation() == null
                                || quay.getCentroid().getLocation().getLatitude() == null
                                || quay.getCentroid().getLocation().getLatitude() == null) {
                            LOG.warn("Quay " + quay.getId() + " does not contain any coordinates.");
                            continue;
                        }
                        stopQuay.setName(stop.getName());
                        stopQuay.setLat(quay.getCentroid().getLocation().getLatitude().doubleValue());
                        stopQuay.setLon(quay.getCentroid().getLocation().getLongitude().doubleValue());
                        stopQuay.setId(FeedScopedIdFactory.createAgencyAndId(quay.getId()));
                        stopQuay.setParentStation(stop.getId().getId());

                        if (!quaysSeen.contains(quay.getId())) {
                            stops.add(stopQuay);
                            quaysSeen.add(quay.getId());
                        }
                    }
                }
            }
        }
        return stops;
    }
}

