package org.opentripplanner.ext.transmodelapi.mapping;

import org.opentripplanner.ext.transmodelapi.model.stop.PlaceType;
import org.opentripplanner.ext.transmodelapi.model.TransmodelPlaceType;
import org.opentripplanner.model.calendar.ServiceDate;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
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

    private static org.opentripplanner.routing.graphfinder.PlaceType mapPlaceType(TransmodelPlaceType transmodelType){
        if (transmodelType!=null) {
            switch (transmodelType) {
                case QUAY:
                case STOP_PLACE:
                    return org.opentripplanner.routing.graphfinder.PlaceType.STOP;
                case BICYCLE_RENT:
                    return org.opentripplanner.routing.graphfinder.PlaceType.BICYCLE_RENT;
                case BIKE_PARK:
                    return org.opentripplanner.routing.graphfinder.PlaceType.BIKE_PARK;
                case CAR_PARK:
                    return org.opentripplanner.routing.graphfinder.PlaceType.CAR_PARK;
            }
        }
        return null;
    }
}
