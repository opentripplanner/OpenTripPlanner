package org.opentripplanner.gtfs.mapping;

import static org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_BOARDING_AREA;
import static org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_ENTRANCE_EXIT;
import static org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_NODE;
import static org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_STATION;
import static org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_STOP;

import java.util.Collection;
import java.util.function.Function;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.ext.fares.model.FareRulesData;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.util.OTPFeature;

/**
 * This class is responsible for mapping between GTFS DAO objects and into OTP Transit model.
 * General mapping code or reusable bussiness logic should be moved into the Builder; hence reusable
 * for other import modules.
 */
public class GTFSToOtpTransitServiceMapper {

  private final AgencyMapper agencyMapper;

  private final StationMapper stationMapper;

  private final StopMapper stopMapper;

  private final EntranceMapper entranceMapper;

  private final PathwayNodeMapper pathwayNodeMapper;

  private final BoardingAreaMapper boardingAreaMapper;

  private final LocationMapper locationMapper = new LocationMapper();

  private final LocationGroupMapper locationGroupMapper;

  private final FareAttributeMapper fareAttributeMapper = new FareAttributeMapper();

  private final ServiceCalendarDateMapper serviceCalendarDateMapper = new ServiceCalendarDateMapper();

  private final FeedInfoMapper feedInfoMapper;

  private final ShapePointMapper shapePointMapper = new ShapePointMapper();

  private final ServiceCalendarMapper serviceCalendarMapper = new ServiceCalendarMapper();

  private final PathwayMapper pathwayMapper;

  private final RouteMapper routeMapper;

  private final TripMapper tripMapper;

  private final BookingRuleMapper bookingRuleMapper;

  private final StopTimeMapper stopTimeMapper;

  private final FrequencyMapper frequencyMapper;

  private final FareRuleMapper fareRuleMapper;

  private final FareProductMapper fareProductMapper;

  private final FareLegRuleMapper fareLegRuleMapper;

  private final DirectionMapper directionMapper;

  private final DataImportIssueStore issueStore;

  private final GtfsRelationalDao data;

  private final OtpTransitServiceBuilder builder = new OtpTransitServiceBuilder();

  private final FareRulesData fareRulesBuilder = new FareRulesData();

  private final TranslationHelper translationHelper;
  private final boolean discardMinTransferTimes;

  public GTFSToOtpTransitServiceMapper(
    String feedId,
    DataImportIssueStore issueStore,
    boolean discardMinTransferTimes,
    GtfsRelationalDao data
  ) {
    // Create callbacks for mappers to retrieve stop and stations
    Function<FeedScopedId, Station> stationLookup = id -> builder.getStations().get(id);
    Function<FeedScopedId, RegularStop> stopLookup = id -> builder.getStops().get(id);

    this.issueStore = issueStore;
    this.data = data;
    this.discardMinTransferTimes = discardMinTransferTimes;
    translationHelper = new TranslationHelper();
    feedInfoMapper = new FeedInfoMapper(feedId);
    agencyMapper = new AgencyMapper(feedId);
    stationMapper = new StationMapper(translationHelper);
    stopMapper = new StopMapper(translationHelper, stationLookup);
    entranceMapper = new EntranceMapper(translationHelper, stationLookup);
    pathwayNodeMapper = new PathwayNodeMapper(translationHelper, stationLookup);
    boardingAreaMapper = new BoardingAreaMapper(translationHelper, stopLookup);
    locationGroupMapper = new LocationGroupMapper(stopMapper, locationMapper);
    pathwayMapper =
      new PathwayMapper(stopMapper, entranceMapper, pathwayNodeMapper, boardingAreaMapper);
    routeMapper = new RouteMapper(agencyMapper, issueStore, translationHelper);
    directionMapper = new DirectionMapper(issueStore);
    tripMapper = new TripMapper(routeMapper, directionMapper);
    bookingRuleMapper = new BookingRuleMapper();
    stopTimeMapper =
      new StopTimeMapper(
        stopMapper,
        locationMapper,
        locationGroupMapper,
        tripMapper,
        bookingRuleMapper
      );
    frequencyMapper = new FrequencyMapper(tripMapper);
    fareRuleMapper = new FareRuleMapper(routeMapper, fareAttributeMapper);
    fareProductMapper = new FareProductMapper();
    fareLegRuleMapper = new FareLegRuleMapper(fareProductMapper, issueStore);
  }

  public OtpTransitServiceBuilder getBuilder() {
    return builder;
  }

  public FareRulesData getFareRulesService() {
    return fareRulesBuilder;
  }

  public void mapStopTripAndRouteDataIntoBuilder() {
    translationHelper.importTranslations(data.getAllTranslations(), data.getAllFeedInfos());

    builder.getAgenciesById().addAll(agencyMapper.map(data.getAllAgencies()));
    builder.getCalendarDates().addAll(serviceCalendarDateMapper.map(data.getAllCalendarDates()));
    builder.getCalendars().addAll(serviceCalendarMapper.map(data.getAllCalendars()));
    builder.getFeedInfos().addAll(feedInfoMapper.map(data.getAllFeedInfos()));
    builder.getFrequencies().addAll(frequencyMapper.map(data.getAllFrequencies()));
    builder.getRoutes().addAll(routeMapper.map(data.getAllRoutes()));
    for (ShapePoint shapePoint : shapePointMapper.map(data.getAllShapePoints())) {
      builder.getShapePoints().put(shapePoint.getShapeId(), shapePoint);
    }

    mapGtfsStopsToOtpTypes(data.getAllStops());

    if (OTPFeature.FlexRouting.isOn()) {
      // Stop areas and Stop groups are only used in FLEX routes
      builder.getAreaStops().addAll(locationMapper.map(data.getAllLocations()));
      builder.getGroupStops().addAll(locationGroupMapper.map(data.getAllLocationGroups()));
    }

    builder.getPathways().addAll(pathwayMapper.map(data.getAllPathways()));
    builder.getStopTimesSortedByTrip().addAll(stopTimeMapper.map(data.getAllStopTimes()));
    builder.getTripsById().addAll(tripMapper.map(data.getAllTrips()));

    fareRulesBuilder.fareAttributes().addAll(fareAttributeMapper.map(data.getAllFareAttributes()));
    fareRulesBuilder.fareRules().addAll(fareRuleMapper.map(data.getAllFareRules()));
    fareRulesBuilder.fareLegRules().addAll(fareLegRuleMapper.map(data.getAllFareLegRules()));

    mapAndAddTransfersToBuilder();
  }

  private void mapGtfsStopsToOtpTypes(Collection<org.onebusaway.gtfs.model.Stop> stops) {
    // Map station first, so we can link to them
    for (org.onebusaway.gtfs.model.Stop it : stops) {
      if (it.getLocationType() == LOCATION_TYPE_STATION) {
        builder.getStations().add(stationMapper.map(it));
      }
    }

    // Map Stop, Entrance and PathwayNode, link to station
    for (org.onebusaway.gtfs.model.Stop it : stops) {
      if (it.getLocationType() == LOCATION_TYPE_STOP) {
        builder.getStops().add(stopMapper.map(it));
      } else if (it.getLocationType() == LOCATION_TYPE_ENTRANCE_EXIT) {
        builder.getEntrances().add(entranceMapper.map(it));
      } else if (it.getLocationType() == LOCATION_TYPE_NODE) {
        builder.getPathwayNodes().add(pathwayNodeMapper.map(it));
      }
    }

    // Map BoardingArea, link to stop
    for (org.onebusaway.gtfs.model.Stop it : stops) {
      if (it.getLocationType() == LOCATION_TYPE_BOARDING_AREA) {
        builder.getBoardingAreas().add(boardingAreaMapper.map(it));
      }
    }
  }

  /**
   * Note! Trip-pattens must be added BEFORE mapping transfers
   */
  private void mapAndAddTransfersToBuilder() {
    TransferMapper transferMapper = new TransferMapper(
      routeMapper,
      stationMapper,
      stopMapper,
      tripMapper,
      builder.getStopTimesSortedByTrip(),
      discardMinTransferTimes,
      issueStore
    );
    var result = transferMapper.map(data.getAllTransfers());
    builder.getTransfers().addAll(result.constrainedTransfers());
    builder.getStaySeatedNotAllowed().addAll(result.staySeatedNotAllowed());
  }
}
