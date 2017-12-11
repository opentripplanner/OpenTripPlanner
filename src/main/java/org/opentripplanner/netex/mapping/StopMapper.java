package org.opentripplanner.netex.mapping;

import com.google.common.collect.Iterables;
import org.opentripplanner.model.Stop;
import org.opentripplanner.netex.loader.NetexDao;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class StopMapper {
    private static final Logger LOG = LoggerFactory.getLogger(StopMapper.class);

    public Collection<Stop> mapParentAndChildStops(Collection<StopPlace> stopPlaceAllVersions, NetexDao netexDao){
        ArrayList<Stop> stops = new ArrayList<>();

        Stop stop = new Stop();
        stop.setLocationType(1);

        // Sort by versions, latest first
        stopPlaceAllVersions = stopPlaceAllVersions.stream()
                .sorted(Comparator.comparingInt(o -> Integer.parseInt(o.getVersion())))
                .collect(Collectors.toList());

        StopPlace stopPlaceLatest = Iterables.getLast(stopPlaceAllVersions);

        if (stopPlaceLatest.getName() != null) {
            stop.setName(stopPlaceLatest.getName().getValue());
        } else {
            stop.setName("N/A");
        }

        if(stopPlaceLatest.getCentroid() != null){
            stop.setLat(stopPlaceLatest.getCentroid().getLocation().getLatitude().doubleValue());
            stop.setLon(stopPlaceLatest.getCentroid().getLocation().getLongitude().doubleValue());
        }else{
            LOG.warn(stopPlaceLatest.getId() + " does not contain any coordinates.");
        }

        stop.setId(FeedScopedIdFactory.createFeedScopedId(stopPlaceLatest.getId()));
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
                        stopQuay.setId(FeedScopedIdFactory.createFeedScopedId(quay.getId()));
                        stopQuay.setPlatformCode(quay.getPublicCode());
                        stopQuay.setParentStation(stop.getId().getId());


                        // Continue if this is not newest version of quay
                        if (netexDao.lookupQuayById(stopQuay.getId().getId()).stream()
                                .anyMatch(q -> Integer.parseInt(q.getVersion()) > Integer.parseInt(quay.getVersion()))) {
                            continue;
                        }

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
