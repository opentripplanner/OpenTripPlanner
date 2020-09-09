package org.opentripplanner.ext.transmodelapi.mapping;

import org.opentripplanner.ext.transmodelapi.model.TransmodelPlaceType;
import org.opentripplanner.ext.transmodelapi.model.stop.MonoOrMultiModalStation;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.graphfinder.PlaceAtDistance;
import org.opentripplanner.routing.graphfinder.PlaceType;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * Utility methods for mapping transmddel API values to and from internal formats.
 */
public class TransmodelMappingUtil {

    private final TimeZone timeZone;

    public TransmodelMappingUtil(TimeZone timeZone) {
        this.timeZone = timeZone;
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


     public static List<org.opentripplanner.routing.graphfinder.PlaceType> mapPlaceTypes(List<TransmodelPlaceType> inputTypes) {
         if (inputTypes == null) {
             return null;
         }

         return inputTypes.stream().map(TransmodelMappingUtil::mapPlaceType).distinct().collect(Collectors.toList());
     }

    private static PlaceType mapPlaceType(TransmodelPlaceType transmodelType){
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

    /**
     * Create PlaceAndDistance objects for all unique stopPlaces according to specified multiModalMode if client has requested stopPlace type.
     *
     * Necessary because nearest does not support StopPlace (stations), so we need to fetch quays instead and map the response.
     *
     * Remove PlaceAndDistance objects for quays if client has not requested these.
     */
    public static List<PlaceAtDistance> convertQuaysToStopPlaces(List<TransmodelPlaceType> placeTypes, List<PlaceAtDistance> places, String multiModalMode, RoutingService routingService) {
        if (placeTypes==null || placeTypes.contains(TransmodelPlaceType.STOP_PLACE)) {
            // Convert quays to stop places
            List<PlaceAtDistance> stations = places
                .stream()
                .filter(p -> p.place instanceof Stop)
                .map(p -> new PlaceAtDistance(new MonoOrMultiModalStation(((Stop) p.place).getParentStation(),
                    null
                ), p.distance))
                .collect(Collectors.toList());

            List<PlaceAtDistance> parentStations = stations.stream()
                .filter(p -> routingService.getMultiModalStationForStations().containsKey((MonoOrMultiModalStation) p.place))
                .map(p -> new PlaceAtDistance( routingService.getMultiModalStationForStations().get((MonoOrMultiModalStation) p.place), p.distance))
                .collect(Collectors.toList());

            if ("parent".equals(multiModalMode)) {
                // Replace monomodal children with their multimodal parents
                stations = parentStations;
            }
            else if ("all".equals(multiModalMode)) {
                // Add multimodal parents in addition to their monomodal children
                places.addAll(parentStations);
            }

            places.addAll(stations);

            if (placeTypes != null && !placeTypes.contains(TransmodelPlaceType.QUAY)) {
                // Remove quays if only stop places are requested
                places = places.stream().filter(p -> !(p.place instanceof Stop)).collect(Collectors.toList());
            }

        }
        places.sort(Comparator.comparing(p -> p.distance));

        Set<Object> uniquePlaces= new HashSet<>();
        return places.stream().filter(s -> uniquePlaces.add(s.place)).collect(Collectors.toList());
    }
}
