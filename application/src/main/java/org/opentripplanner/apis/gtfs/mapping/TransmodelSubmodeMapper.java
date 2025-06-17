package org.opentripplanner.apis.gtfs.mapping;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.apis.transmodel.model.TransmodelTransportSubmode;
import org.opentripplanner.transit.model.basic.SubMode;

public class TransmodelSubmodeMapper {

  record Mapping(TransmodelTransportSubmode submode, int gtfsMode) {}

  record ExtraMapping(String submode, int gtfsMode) {}

  // Mapping from
  // https://enturas.atlassian.net/wiki/spaces/PUBLIC/pages/825393529/Norwegian+submodes+and+their+definitions
  static final Mapping[] mappings = {
    new Mapping(TransmodelTransportSubmode.LOCAL_BUS, 704),
    new Mapping(TransmodelTransportSubmode.REGIONAL_BUS, 701),
    new Mapping(TransmodelTransportSubmode.EXPRESS_BUS, 702),
    new Mapping(TransmodelTransportSubmode.NIGHT_BUS, 705),
    new Mapping(TransmodelTransportSubmode.SIGHTSEEING_BUS, 710),
    new Mapping(TransmodelTransportSubmode.SHUTTLE_BUS, 711),
    new Mapping(TransmodelTransportSubmode.SCHOOL_BUS, 713),
    new Mapping(TransmodelTransportSubmode.RAIL_REPLACEMENT_BUS, 714),
    new Mapping(TransmodelTransportSubmode.AIRPORT_LINK_BUS, 711),
    new Mapping(TransmodelTransportSubmode.INTERNATIONAL_COACH, 201),
    new Mapping(TransmodelTransportSubmode.NATIONAL_COACH, 202),
    new Mapping(TransmodelTransportSubmode.CHARTER_TAXI, 704),
    new Mapping(TransmodelTransportSubmode.COMMUNAL_TAXI, 704),
    new Mapping(TransmodelTransportSubmode.LOCAL_TRAM, 902),
    new Mapping(TransmodelTransportSubmode.CITY_TRAM, 901),
    new Mapping(TransmodelTransportSubmode.METRO, 401),
    new Mapping(TransmodelTransportSubmode.LOCAL, 109),
    new Mapping(TransmodelTransportSubmode.REGIONAL_RAIL, 103),
    new Mapping(TransmodelTransportSubmode.INTERREGIONAL_RAIL, 103),
    new Mapping(TransmodelTransportSubmode.LONG_DISTANCE, 102),
    new Mapping(TransmodelTransportSubmode.INTERNATIONAL, 100),
    new Mapping(TransmodelTransportSubmode.TOURIST_RAILWAY, 107),
    new Mapping(TransmodelTransportSubmode.NIGHT_RAIL, 105),
    new Mapping(TransmodelTransportSubmode.AIRPORT_LINK_RAIL, 108),
    new Mapping(TransmodelTransportSubmode.INTERNATIONAL_CAR_FERRY, 1001),
    new Mapping(TransmodelTransportSubmode.NATIONAL_CAR_FERRY, 1002),
    new Mapping(TransmodelTransportSubmode.LOCAL_CAR_FERRY, 1004),
    new Mapping(TransmodelTransportSubmode.LOCAL_PASSENGER_FERRY, 1008),
    new Mapping(TransmodelTransportSubmode.SIGHTSEEING_SERVICE, 1016),
    new Mapping(TransmodelTransportSubmode.HIGH_SPEED_VEHICLE_SERVICE, 1014),
    new Mapping(TransmodelTransportSubmode.HIGH_SPEED_PASSENGER_SERVICE, 1015),
    new Mapping(TransmodelTransportSubmode.INTERNATIONAL_FLIGHT, 1102),
    new Mapping(TransmodelTransportSubmode.DOMESTIC_FLIGHT, 1103),
    new Mapping(TransmodelTransportSubmode.HELICOPTER_SERVICE, 1110),
    new Mapping(TransmodelTransportSubmode.TELECABIN, 1300),
    new Mapping(TransmodelTransportSubmode.FUNICULAR, 1400),
  };

  static final ExtraMapping[] extraMappings = {
    // Finnish state railway VR speciality
    new ExtraMapping("replacementRailService", 714),
  };
  Map<Integer, SubMode> mappingToTransmodel;
  Map<SubMode, Integer> mappingToGtfs;

  public TransmodelSubmodeMapper() {
    mappingToTransmodel = new HashMap<>();
    mappingToGtfs = new HashMap<>();
    for (Mapping mapping : mappings) {
      mappingToTransmodel.put(mapping.gtfsMode, SubMode.of(mapping.submode.getValue()));
      mappingToGtfs.put(SubMode.of(mapping.submode.getValue()), mapping.gtfsMode);
    }
    for (ExtraMapping extraMapping : extraMappings) {
      mappingToTransmodel.put(extraMapping.gtfsMode, SubMode.of(extraMapping.submode));
      mappingToGtfs.put(SubMode.of(extraMapping.submode), extraMapping.gtfsMode);
    }
  }

  public Optional<SubMode> map(@Nullable Integer submode) {
    return Optional.ofNullable(mappingToTransmodel.get(submode));
  }

  public Optional<Integer> map(@Nullable SubMode submode) {
    return Optional.ofNullable(mappingToGtfs.get(submode));
  }
}
