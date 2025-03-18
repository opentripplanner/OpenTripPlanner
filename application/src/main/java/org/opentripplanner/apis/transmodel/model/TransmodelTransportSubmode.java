package org.opentripplanner.apis.transmodel.model;

import java.util.Arrays;
import org.opentripplanner.transit.model.basic.SubMode;

public enum TransmodelTransportSubmode {
  UNKNOWN("unknown"),
  UNDEFINED("undefined"),
  // Air
  INTERNATIONAL_FLIGHT("internationalFlight"),
  DOMESTIC_FLIGHT("domesticFlight"),
  INTERCONTINENTAL_FLIGHT("intercontinentalFlight"),
  DOMESTIC_SCHEDULED_FLIGHT("domesticScheduledFlight"),
  SHUTTLE_FLIGHT("shuttleFlight"),
  INTERCONTINENTAL_CHARTER_FLIGHT("intercontinentalCharterFlight"),
  INTERNATIONAL_CHARTER_FLIGHT("internationalCharterFlight"),
  ROUND_TRIP_CHARTER_FLIGHT("roundTripCharterFlight"),
  SIGHTSEEING_FLIGHT("sightseeingFlight"),
  HELICOPTER_SERVICE("helicopterService"),
  DOMESTIC_CHARTER_FLIGHT("domesticCharterFlight"),
  SCHENGEN_AREA_FLIGHT("SchengenAreaFlight"),
  AIRSHIP_SERVICE("airshipService"),
  SHORT_HAUL_INTERNATIONAL_FLIGHT("shortHaulInternationalFlight"),
  CANAL_BARGE("canalBarge"),
  // Bus
  LOCAL_BUS("localBus"),
  REGIONAL_BUS("regionalBus"),
  EXPRESS_BUS("expressBus"),
  NIGHT_BUS("nightBus"),
  POST_BUS("postBus"),
  SPECIAL_NEEDS_BUS("specialNeedsBus"),
  MOBILITY_BUS("mobilityBus"),
  MOBILITY_BUS_FOR_REGISTERED_DISABLED("mobilityBusForRegisteredDisabled"),
  SIGHTSEEING_BUS("sightseeingBus"),
  SHUTTLE_BUS("shuttleBus"),
  HIGH_FREQUENCY_BUS("highFrequencyBus"),
  DEDICATED_LANE_BUS("dedicatedLaneBus"),
  SCHOOL_BUS("schoolBus"),
  SCHOOL_AND_PUBLIC_SERVICE_BUS("schoolAndPublicServiceBus"),
  RAIL_REPLACEMENT_BUS("railReplacementBus"),
  DEMAND_AND_RESPONSE_BUS("demandAndResponseBus"),
  AIRPORT_LINK_BUS("airportLinkBus"),
  // Coach
  INTERNATIONAL_COACH("internationalCoach"),
  NATIONAL_COACH("nationalCoach"),
  SHUTTLE_COACH("shuttleCoach"),
  REGIONAL_COACH("regionalCoach"),
  SPECIAL_COACH("specialCoach"),
  SCHOOL_COACH("schoolCoach"),
  SIGHTSEEING_COACH("sightseeingCoach"),
  TOURIST_COACH("touristCoach"),
  COMMUTER_COACH("commuterCoach"),
  // Funicular
  FUNICULAR("funicular"),
  STREET_CABLE_CAR("streetCableCar"),
  ALL_FUNICULAR_SERVICES("allFunicularServices"),
  UNDEFINED_FUNICULAR("undefinedFunicular"),
  // Metro
  METRO("metro"),
  TUBE("tube"),
  URBAN_RAILWAY("urbanRailway"),
  // Tram
  CITY_TRAM("cityTram"),
  LOCAL_TRAM("localTram"),
  REGIONAL_TRAM("regionalTram"),
  SIGHTSEEING_TRAM("sightseeingTram"),
  SHUTTLE_TRAM("shuttleTram"),
  TRAIN_TRAM("trainTram"),
  // Telecabin
  TELECABIN("telecabin"),
  CABLE_CAR("cableCar"),
  LIFT("lift"),
  CHAIR_LIFT("chairLift"),
  DRAG_LIFT("dragLift"),
  TELECABIN_LINK("telecabinLink"),
  // Rail
  LOCAL("local"),
  HIGH_SPEED_RAIL("highSpeedRail"),
  SUBURBAN_RAILWAY("suburbanRailway"),
  REGIONAL_RAIL("regionalRail"),
  INTERREGIONAL_RAIL("interregionalRail"),
  LONG_DISTANCE("longDistance"),
  INTERNATIONAL("international"),
  SLEEPER_RAIL_SERVICE("sleeperRailService"),
  NIGHT_RAIL("nightRail"),
  CAR_TRANSPORT_RAIL_SERVICE("carTransportRailService"),
  TOURIST_RAILWAY("touristRailway"),
  AIRPORT_LINK_RAIL("airportLinkRail"),
  RAIL_SHUTTLE("railShuttle"),
  REPLACEMENT_RAIL_SERVICE("replacementRailService"),
  SPECIAL_TRAIN("specialTrain"),
  CROSS_COUNTRY_RAIL("crossCountryRail"),
  RACK_AND_PINION_RAILWAY("rackAndPinionRailway"),
  // Water
  INTERNATIONAL_CAR_FERRY("internationalCarFerry"),
  NATIONAL_CAR_FERRY("nationalCarFerry"),
  REGIONAL_CAR_FERRY("regionalCarFerry"),
  LOCAL_CAR_FERRY("localCarFerry"),
  INTERNATIONAL_PASSENGER_FERRY("internationalPassengerFerry"),
  NATIONAL_PASSENGER_FERRY("nationalPassengerFerry"),
  REGIONAL_PASSENGER_FERRY("regionalPassengerFerry"),
  LOCAL_PASSENGER_FERRY("localPassengerFerry"),
  POST_BOAT("postBoat"),
  TRAIN_FERRY("trainFerry"),
  ROAD_FERRY_LINK("roadFerryLink"),
  AIRPORT_BOAT_LINK("airportBoatLink"),
  HIGH_SPEED_VEHICLE_SERVICE("highSpeedVehicleService"),
  HIGH_SPEED_PASSENGER_SERVICE("highSpeedPassengerService"),
  SIGHTSEEING_SERVICE("sightseeingService"),
  SCHOOL_BOAT("schoolBoat"),
  CABLE_FERRY("cableFerry"),
  RIVER_BUS("riverBus"),
  SCHEDULED_FERRY("scheduledFerry"),
  SHUTTLE_FERRY_SERVICE("shuttleFerryService"),
  // Taxi
  COMMUNAL_TAXI("communalTaxi"),
  CHARTER_TAXI("charterTaxi"),
  WATER_TAXI("waterTaxi"),
  RAIL_TAXI("railTaxi"),
  BIKE_TAXI("bikeTaxi"),
  BLACK_CAB("blackCab"),
  MINI_CAB("miniCab"),
  ALL_TAXI_SERVICES("allTaxiServices"),
  // Self drive
  HIRE_CAR("hireCar"),
  HIRE_VAN("hireVan"),
  HIRE_MOTORBIKE("hireMotorbike"),
  HIRE_CYCLE("hireCycle"),
  ALL_HIRE_VEHICLES("allHireVehicles");

  private final String value;

  TransmodelTransportSubmode(String value) {
    this.value = value;
  }

  public static TransmodelTransportSubmode fromValue(SubMode value) {
    return Arrays.stream(TransmodelTransportSubmode.values())
      .filter(tp -> tp.getValue().equals(value.name()))
      .findFirst()
      .orElse(null);
  }

  public String getValue() {
    return value;
  }
}
