package org.opentripplanner.netex.mapping;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.FareZone;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalVersionMapById;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.netex.mapping.support.StopPlaceVersionAndValidityComparator;
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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

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
class StopAndStationMapper {
    private static final Logger LOG = LoggerFactory.getLogger(StopAndStationMapper.class);

    private final ReadOnlyHierarchicalVersionMapById<Quay> quayIndex;
    private final StationMapper stationMapper;
    private final StopMapper stopMapper;
    private final EntityById<FareZone> tariffZonesById;
    private final FeedScopedIdFactory idFactory;

    /**
     * Quay ids for all processed stop places
     */
    private final Set<String> quaysAlreadyProcessed = new HashSet<>();

    final List<Stop> resultStops = new ArrayList<>();
    final List<Station> resultStations = new ArrayList<>();
    final Multimap<String, Station> resultStationByMultiModalStationRfs = ArrayListMultimap.create();


    StopAndStationMapper(
            FeedScopedIdFactory idFactory,
            ReadOnlyHierarchicalVersionMapById<Quay> quayIndex,
            EntityById<FareZone> tariffZonesById,
            DataImportIssueStore issueStore
    ) {
        this.stationMapper = new StationMapper(idFactory);
        this.stopMapper = new StopMapper(idFactory, issueStore);
        this.quayIndex = quayIndex;
        this.tariffZonesById = tariffZonesById;
        this.idFactory = idFactory;
    }

    /**
     * @param stopPlaces all stop places including multiple versions of each.
     */
    void mapParentAndChildStops(final Collection<StopPlace> stopPlaces) {

        // Prioritize StopPlace versions. Highest priority first.
        // TODO OTP2 - This should pushed up into the ReadOnlyHierarchicalVersionMapById as part of
        //           - Issue: Netex import resolve version for all entities , not just stops #2781
        List<StopPlace> stopPlaceAllVersions = sortStopPlacesByValidityAndVersionDesc(stopPlaces);
        Station station = mapStopPlaceAllVersionsToStation(stopPlaceAllVersions);
        List<FareZone> fareZones = mapTariffZones(stopPlaceAllVersions);

        // Loop through all versions of the StopPlace in order to collect all quays, even if they were deleted in
        // never versions of the StopPlace
        for (StopPlace stopPlace : stopPlaceAllVersions) {
            for (Quay quay : listOfQuays(stopPlace)) {
                addNewStopToParentIfNotPresent(quay, station, fareZones);
            }
        }
    }

    private Station mapStopPlaceAllVersionsToStation(List<StopPlace> stopPlaceAllVersions) {
        // Map the highest priority StopPlace version to station
        StopPlace selectedStopPlace = first(stopPlaceAllVersions);

        Station station = stationMapper.map(selectedStopPlace);
        if (selectedStopPlace.getParentSiteRef() != null) {
            resultStationByMultiModalStationRfs.put(
                    selectedStopPlace.getParentSiteRef().getRef(),
                    station
            );
        }
        resultStations.add(station);
        return station;
    }

    private List<FareZone> mapTariffZones(List<StopPlace> stopPlaceAllVersions) {
        StopPlace selectedStopPlace = first(stopPlaceAllVersions);
        return selectedStopPlace.getTariffZones() != null ? selectedStopPlace
            .getTariffZones()
            .getTariffZoneRef()
            .stream()
            .map(t -> tariffZonesById.get(idFactory.createId(t.getRef())))
            .filter(Objects::nonNull)
            .collect(Collectors.toList()) : Collections.emptyList();
    }

    /**
     * Sort stop places on version with latest version first (descending order).
     */
    private List<StopPlace> sortStopPlacesByValidityAndVersionDesc(Collection<StopPlace> stopPlaces) {
        return stopPlaces.stream()
                .sorted(new StopPlaceVersionAndValidityComparator())
                .collect(toList());
    }

    private void addNewStopToParentIfNotPresent(
        Quay quay,
        Station station,
        List<FareZone> fareZones
    ) {
        // TODO OTP2 - This assumtion is only valid because Norway have a
        //           - national stop register, we should add all stops/quays
        //           - for version resolution.
        // Continue if this is not newest version of quay
        if (!quayIndex.isNewerOrSameVersionComparedWithExistingValues(quay)) {
            return;
        }
        if (quaysAlreadyProcessed.contains(quay.getId())) {
            return;
        }

        Stop stop = stopMapper.mapQuayToStop(quay, station, fareZones);
        if (stop == null) return;

        station.addChildStop(stop);

        resultStops.add(stop);
        quaysAlreadyProcessed.add(quay.getId());
    }

    /**
     * Return the list of quays for the given {@code stopPlace} or an empty list if
     * no quays exist.
     * <p>
     * We do not support quay references, all quays must be included as part of the
     * given stopPlace.
     */
    private static List<Quay> listOfQuays(StopPlace stopPlace) {
        Quays_RelStructure quays = stopPlace.getQuays();

        if(quays == null) {
            LOG.warn("StopPlace {} has no quays.", stopPlace.getId());
            return Collections.emptyList();
        }

        List<Quay> result = new ArrayList<>();

        for (Object it : quays.getQuayRefOrQuay()) {
            if(it instanceof Quay) {
                result.add((Quay) it);
            }
            else {
                LOG.warn("StopPlace {} has unsupported quay reference: {}", stopPlace.getId(), it);
            }
        }
        return result;
    }

    private static StopPlace first(List<StopPlace> stops) {
        return stops.get(0);
    }
}
