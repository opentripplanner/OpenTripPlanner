package org.opentripplanner.ext.transmodelapi.mapping;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import org.opentripplanner.ext.transmodelapi.model.MonoOrMultiModalStation;
import org.opentripplanner.ext.transmodelapi.model.PlaceType;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.ext.transmodelapi.model.TransmodelPlaceType;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.MultiModalStation;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.calendar.ServiceDate;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * Utility methods for mapping transmddel API values to and from internal formats.
 */
public class TransmodelMappingUtil {

    private static final String LIST_VALUE_SEPARATOR = ",";

    private static final String GTFS_LIBRARY_ID_SEPARATOR = ":";

    private String fixedAgencyId;

    private TimeZone timeZone;

    public TransmodelMappingUtil(String fixedAgencyId, TimeZone timeZone) {
        this.fixedAgencyId = fixedAgencyId;
        this.timeZone = timeZone;
    }


    public String toIdString(FeedScopedId id) {
        if (fixedAgencyId != null) {
            return id.getId();
        }
        return GtfsLibrary.convertIdToString(id);
    }

    public FeedScopedId fromIdString(String id) {
        if (fixedAgencyId != null) {
            return new FeedScopedId(fixedAgencyId, id);
        }
        return GtfsLibrary.convertIdFromString(id);
    }


    /**
     * Add agency id prefix to vertexIds if fixed agency is set.
     */
    /* TODO OTP2
    public String preparePlaceRef(String input) {
        if (fixedAgencyId != null && input != null) {
            GenericLocation location = GenericLocation.fromOldStyleString(input);

            if (location.hasVertexId()) {
                String prefixedPlace = prepareFeedScopedId(location.place, GTFS_LIBRARY_ID_SEPARATOR);
                return new GenericLocation(location.name, prefixedPlace).toString();
            }

        }
        return input;
    }
     */

    public String prepareListOfFeedScopedId(List<String> ids) {
        return mapCollectionOfValues(ids, this::prepareFeedScopedId);
    }

    public String prepareListOfFeedScopedId(List<String> ids, String separator) {
        return mapCollectionOfValues(ids, value -> prepareFeedScopedId(value, separator));
    }

    public String prepareFeedScopedId(String id) {
        return prepareFeedScopedId(id, null);
    }

    public String prepareFeedScopedId(String id, String separator) {
        if (fixedAgencyId != null && id != null) {
            return separator == null
                           ? FeedScopedId.concatenateId(fixedAgencyId, id)
                           : fixedAgencyId + separator + id;
        }
        return id;
    }

    public String mapCollectionOfValues(Collection<String> values, Function<String, String> mapElementFunction) {
        if (values == null) {
            return null;
        }
        List<String> otpModelModes = values.stream().map(value -> mapElementFunction.apply(value)).collect(Collectors.toList());

        return Joiner.on(LIST_VALUE_SEPARATOR).join(otpModelModes);
    }

    // Create a dummy route to be able to reuse GtfsLibrary functionality
    public Object mapVehicleTypeToTraverseMode(int vehicleType) {
        Route dummyRoute = new Route();
        dummyRoute.setType(vehicleType);
        try {
            return GtfsLibrary.getTraverseMode(dummyRoute);
        } catch (IllegalArgumentException iae) {
            return "unknown";
        }
    }


    public Long serviceDateToSecondsSinceEpoch(ServiceDate serviceDate) {
        if (serviceDate == null) {
            return null;
        }

        return LocalDate.of(serviceDate.getYear(), serviceDate.getMonth(), serviceDate.getDay())
                .atStartOfDay(timeZone.toZoneId()).toEpochSecond();
    }

    public ServiceDate secondsSinceEpochToServiceDate(Long secondsSinceEpoch) {
        if (secondsSinceEpoch == null) {
            return new ServiceDate();
        }
        return new ServiceDate(new Date(secondsSinceEpoch * 1000));
    }


    public List<PlaceType> mapPlaceTypes(List<TransmodelPlaceType> inputTypes) {
        if (inputTypes == null) {
            return null;
        }

        return inputTypes.stream().map(pt -> mapPlaceType(pt)).distinct().collect(Collectors.toList());
    }

    public MonoOrMultiModalStation getMonoOrMultiModalStation(
        String idString,
        Map<FeedScopedId, Station> stationById,
        Map<FeedScopedId, MultiModalStation> multiModalStationById,
        Map<Station, MultiModalStation> multimodalStationForStations
    ) {
        FeedScopedId id = fromIdString(idString);
        Station station = stationById.get(id);
        if (station != null) {
            return new MonoOrMultiModalStation(station, multimodalStationForStations.get(station));
        }
        MultiModalStation multiModalStation = multiModalStationById.get(id);
        if (multiModalStation != null) {
            return new MonoOrMultiModalStation(multiModalStation);
        }
        return null;
    }

    private PlaceType mapPlaceType(TransmodelPlaceType transmodelType){
        if (transmodelType!=null) {
            switch (transmodelType) {
                case QUAY:
                case STOP_PLACE:
                    return PlaceType.STOP;
                case BICYCLE_RENT:
                    return PlaceType.BICYCLE_RENT;
                case BIKE_PARK:
                    return PlaceType.BIKE_PARK;
                case CAR_PARK:
                    return PlaceType.CAR_PARK;
            }
        }
        return null;
    }
}
