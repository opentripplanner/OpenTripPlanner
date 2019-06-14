package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Stop;
import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.opentripplanner.netex.loader.util.HierarchicalMultimapById;
import org.opentripplanner.netex.support.StopPlaceVersionAndValidityComparator;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.Quays_RelStructure;
import org.rutebanken.netex.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static org.opentripplanner.netex.mapping.PointMapper.verifyPointAndProcessCoordinate;

/**
 * This maps a NeTEx StopPlace and its child quays to and OTP parent stop and child stops. NeTEx also contains
 * GroupsOfStopPlaces and these are also mapped to parent stops, because searching from a StopPlace and searching from
 * a GroupOfStopPlaces both corresponding to searching from all of its underlying quays. Model changes in OTP are
 * required if we want to preserve the original NeTEx hierarchy.
 * <p>
 * A NeTEx StopPlace can contain both a version and a validity period. Since none of these are present in the OTP model
 * we have to choose which version should be mapped based on both of these parameters.
 * <p>
 * To ensure compatibility with older data sets, we also have to keep quays that are only present in older versions
 * of the StopPlace.
 */
class StopMapper {
    private static final Logger LOG = LoggerFactory.getLogger(StopMapper.class);

    private final StopPlaceTypeMapper transportModeMapper = new StopPlaceTypeMapper();

    /**
     * Quay ids for all processed stop places
     */
    private final Set<String> quaysAlreadyProcessed = new HashSet<>();

    private final ArrayList<Stop> resultStops = new ArrayList<>();

    private final HierarchicalMultimapById<Quay> quayIndex;


    StopMapper(HierarchicalMultimapById<Quay> quayIndex) {
        this.quayIndex = quayIndex;
    }

    /**
     * @param stopPlaces all stop places including multiple versions of each.
     */
    Collection<Stop> mapParentAndChildStops(final Collection<StopPlace> stopPlaces) {

        // Prioritize StopPlace versions. Highest priority first.
        List<StopPlace> stopPlaceAllVersions = sortStopPlacesByValidityAndVersionDesc(stopPlaces);

        // Map the highest priority StopPlace version to station
        Stop station = mapToStation(first(stopPlaceAllVersions));

        resultStops.add(station);

        // Loop through all versions of the StopPlace in order to collect all quays, even if they were deleted in
        // never versions of the StopPlace
        for (StopPlace stopPlace : stopPlaceAllVersions) {
            for (Object quayObject : listOfQuays(stopPlace)) {
                Quay quay = (Quay) quayObject;
                addNewStopToParentIfNotPresent(quay, station);
            }
        }
        return resultStops;
    }

    /**
     * Sort stop places on version with latest version first (descending order).
     */
    private List<StopPlace> sortStopPlacesByValidityAndVersionDesc(Collection<StopPlace> stopPlaces) {
        return stopPlaces.stream()
                .sorted(new StopPlaceVersionAndValidityComparator())
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
        boolean locationOk = verifyPointAndProcessCoordinate(
                stop.getCentroid(),
                // This kind of awkward callback can be avoided if we add a
                // Coordinate type the the OTP model, and return that instead.
                coordinate -> {
                    station.setLon(coordinate.getLongitude().doubleValue());
                    station.setLat(coordinate.getLatitude().doubleValue());
                }
        );

        if (!locationOk) {
            LOG.warn("Station {} does not contain any coordinates.", station.getId());
        }

        station.setId(FeedScopedIdFactory.createFeedScopedId(stop.getId()));

        station.setVehicleType(transportModeMapper.getTransportMode(stop));
        return station;
    }

    private void addNewStopToParentIfNotPresent(Quay quay, Stop station) {
        // TODO OTP2 - This assumtion is only valid because Norway have a
        // TODO OTP2 - national stop register, we should add all stops/quays
        // TODO OTP2 - for version resolution.
        // Continue if this is not newest version of quay
        if (!quayIndex.isNewerOrSameVersionComparedWithExistingValues(quay))
            return;
        if (quaysAlreadyProcessed.contains(quay.getId()))
            return;

        Stop otpQuay = new Stop();
        boolean locationOk = verifyPointAndProcessCoordinate(
                quay.getCentroid(),
                // This kind of awkward callback can be avoided if we add a
                // Coordinate type the the OTP model, and return that instead.
                coordinate -> {
                    otpQuay.setLon(coordinate.getLongitude().doubleValue());
                    otpQuay.setLat(coordinate.getLatitude().doubleValue());
                }
        );
        if (!locationOk) {
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

    private static StopPlace first(List<StopPlace> stops) {
        return stops.get(0);
    }
}
