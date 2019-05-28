package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Stop;
import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.opentripplanner.netex.loader.util.HierarchicalMultimapById;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.Quays_RelStructure;
import org.rutebanken.netex.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

class StopMapper {
    private static final Logger LOG = LoggerFactory.getLogger(StopMapper.class);

    private final StopPlaceTypeMapper transportModeMapper = new StopPlaceTypeMapper();

    /**
     * Quay ids for all processed stop places
     */
    private final Set<String> quaysAlreadyProcessed = new HashSet<>();

    private final ArrayList<Stop> resultStops = new ArrayList<>();

    private final HierarchicalMultimapById<Quay> quayIndex;


    private StopMapper(HierarchicalMultimapById<Quay> quayIndex) {
        this.quayIndex = quayIndex;
    }

    /**
     * @param stopPlaces all stop places including multiple versions of each.
     */
    static Collection<Stop> mapParentAndChildStops(final Collection<StopPlace> stopPlaces,
            final NetexImportDataIndex dataIndex) {
        return new StopMapper(dataIndex.quayById).mapParentAndChildStops(stopPlaces);
    }

    /**
     * @param stopPlaces all stop places including multiple versions of each.
     */
    private Collection<Stop> mapParentAndChildStops(final Collection<StopPlace> stopPlaces) {

        // Sort by versions, latest first
        List<StopPlace> stopPlaceAllVersions = sortStopPlacesOnVersionDesc(stopPlaces);

        // Use the last(newest) stop place to create a station
        // TODO OTP2 - Can a station be extracted from the last element in all cases?
        Stop station = mapToStation(last(stopPlaceAllVersions));

        resultStops.add(station);

        for (StopPlace stopPlace : stopPlaceAllVersions) {
            for (Object quayObject : listOfQuays(stopPlace)) {
                Quay quay = (Quay) quayObject;
                addNewStop(quay, station);
            }
        }
        return resultStops;
    }

    /**
     * Sort stop places on version with latest version first (descending order).
     */
    private List<StopPlace> sortStopPlacesOnVersionDesc(Collection<StopPlace> stopPlaces) {
        return stopPlaces.stream()
                .sorted(Comparator.comparingInt(o -> Integer.parseInt(o.getVersion())))
                .collect(toList());
    }

    private Stop mapToStation(StopPlace stop) {
        Stop station = new Stop();
        station.setLocationType(1);

        if (stop.getName() != null) {
            station.setName(stop.getName().getValue());
        } else {
            station.setName("N/A");
        }
        boolean locOk = PointMapper.handleCoordinates(
                stop.getCentroid(),
                // This kind of awkward callback can be avoided if we add a
                // Coordinate type the the OTP model, and return that instead.
                p -> {
                    station.setLon(p.getLongitude().doubleValue());
                    station.setLat(p.getLatitude().doubleValue());
                }
        );

        if (!locOk) {
            LOG.warn("Station {} does not contain any coordinates.", station.getId());
        }

        station.setId(FeedScopedIdFactory.createFeedScopedId(stop.getId()));

        station.setVehicleType(transportModeMapper.getTransportMode(stop));
        return station;
    }

    private void addNewStop(Quay quay, Stop station) {
        // Continue if this is not newest version of quay
        if (!quayIndex.isNewLatestVersion(quay))
            return;
        if (quaysAlreadyProcessed.contains(quay.getId()))
            return;

        Stop otpQuay = new Stop();
        boolean locOk = PointMapper.handleCoordinates(
                quay.getCentroid(),
                // This kind of awkward callback can be avoided if we add a
                // Coordinate type the the OTP model, and return that instead.
                p -> {
                    otpQuay.setLon(p.getLongitude().doubleValue());
                    otpQuay.setLat(p.getLatitude().doubleValue());
                }
        );
        if (!locOk) {
            LOG.warn("Quay {} does not contain any coordinates. Quay is ignored.", quay.getId());
            return;
        }
        otpQuay.setLocationType(0);
        otpQuay.setName(station.getName());
        otpQuay.setId(FeedScopedIdFactory.createFeedScopedId(quay.getId()));
        otpQuay.setPlatformCode(quay.getPublicCode());
        otpQuay.setVehicleType(station.getVehicleType());
        otpQuay.setParentStation(station.getId().getId());

        resultStops.add(otpQuay);
        quaysAlreadyProcessed.add(quay.getId());
    }

    /**
     * Return the list of quays for the given {@code stopPlace} or an empty list if
     * no quays exist.
     */
    private List<Object> listOfQuays(StopPlace stopPlace) {
        Quays_RelStructure quays = stopPlace.getQuays();
        if(quays == null) return Collections.emptyList();
        return quays.getQuayRefOrQuay()
                .stream()
                .filter(v -> v instanceof Quay)
                .collect(toList());
    }

    private static StopPlace last(List<StopPlace> stops) {
        return stops.get(stops.size() - 1);
    }
}
