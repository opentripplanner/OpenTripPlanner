package org.opentripplanner.ext.transmodelapi.mapping;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import org.opentripplanner.ext.transmodelapi.model.MonoOrMultiModalStation;
import org.opentripplanner.ext.transmodelapi.model.PlaceType;
import org.opentripplanner.ext.transmodelapi.model.TransmodelPlaceType;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.MultiModalStation;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.TransitEntity;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.RoutingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOG = LoggerFactory.getLogger(TransmodelMappingUtil.class);
    private static final String LIST_VALUE_SEPARATOR = ",";

    private static String fixedFeedId;
    private final TimeZone timeZone;

    public TransmodelMappingUtil(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    /**
     * This initialize the 'fixedFeedId', before this is done the GraphQL API will use the
     * full id including the feedId.
     *
     * @param entities The entities to pick the feedId from, if more than one feedID exist,
     *                 the feedId with the most occurrences will be used. This is done to prevent
     *                 a few "cases" of wrongly set feedIds to block the entire API from working.
     * @return the fixedFeedId - used to unit test this method.
     */
    public static String setupFixedFeedId(Collection<? extends TransitEntity<FeedScopedId>> entities) {
        fixedFeedId = "UNKNOWN_FEED";

        // Count each feedId
        Map<String, Integer> feedIds = entities
            .stream()
            .map(a -> a.getId().getFeedId())
            .collect(
                Collectors.groupingBy(
                    it -> it,
                    Collectors.reducing(0, i -> 1, Integer::sum)
                )
            );

        if(feedIds.isEmpty()) {
            LOG.warn("No data, unable to resolve fixedFeedScope to use in the Transmodel GraphQL API.");
        }
        else if(feedIds.size() == 1) {
            fixedFeedId = feedIds.keySet().iterator().next();
        }
        else {
            //noinspection OptionalGetWithoutIsPresent
            fixedFeedId = feedIds
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .get()
                .getKey();
            LOG.warn("More than one feedId exist in the list of agencies. The feed-id used by"
                + "most agencies will be picked.");
        }
        LOG.info("Starting Transmodel GraphQL Schema with fixed FeedId: '" + fixedFeedId +
            "'. All FeedScopedIds in API will be assumed to belong to this agency.");
        return fixedFeedId;
    }

    public String toIdString(FeedScopedId id) {
        if (fixedFeedId != null) {
            return id.getId();
        }
        return id.toString();
    }

    public FeedScopedId fromIdString(String id) {
        if (fixedFeedId != null) {
            return new FeedScopedId(fixedFeedId, id);
        }
        return FeedScopedId.parseId(id);
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
        if (fixedFeedId != null && id != null) {
            return separator == null
                           ? FeedScopedId.concatenateId(fixedFeedId, id)
                           : fixedFeedId + separator + id;
        }
        return id;
    }

    public String mapCollectionOfValues(Collection<String> values, Function<String, String> mapElementFunction) {
        if (values == null) {
            return null;
        }
        List<String> otpModelModes = values.stream().map(mapElementFunction).collect(Collectors.toList());

        return Joiner.on(LIST_VALUE_SEPARATOR).join(otpModelModes);
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
        RoutingService routingService
    ) {
        FeedScopedId id = fromIdString(idString);
        Station station = routingService.getStationById(id);
        if (station != null) {
            return new MonoOrMultiModalStation(station, routingService.getMultiModalStationForStations().get(station));
        }
        MultiModalStation multiModalStation = routingService.getMultiModalStationById(id);
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
