package org.opentripplanner.gtfs.mapping;

import static org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_BOARDING_AREA;
import static org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_ENTRANCE_EXIT;
import static org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_NODE;
import static org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_STATION;
import static org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_STOP;

import java.util.Collection;
import java.util.function.Function;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.services.GtfsDao;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.ext.fares.model.FareRulesData;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopTransferPriority;

/**
 * This class is responsible for mapping between GTFS DAO objects and into OTP Transit model.
 * General mapping code or reusable business logic should be moved into the Builder; hence reusable
 * for other import modules.
 */
public class GTFSToOtpTransitServiceMapper {

  private final AgencyMapper agencyMapper;

  private final StationMapper stationMapper;

  private final StopMapper stopMapper;

  private final EntranceMapper entranceMapper;

  private final PathwayNodeMapper pathwayNodeMapper;

  private final BoardingAreaMapper boardingAreaMapper;

  private final LocationMapper locationMapper;

  private final LocationGroupMapper locationGroupMapper;

  private final FareAttributeMapper fareAttributeMapper;

  private final ServiceCalendarDateMapper serviceCalendarDateMapper;

  private final FeedInfoMapper feedInfoMapper;

  private final ShapePointMapper shapePointMapper;

  private final ServiceCalendarMapper serviceCalendarMapper;

  private final PathwayMapper pathwayMapper;

  private final RouteMapper routeMapper;

  private final TripMapper tripMapper;

  private final BookingRuleMapper bookingRuleMapper;

  private final StopTimeMapper stopTimeMapper;

  private final FrequencyMapper frequencyMapper;

  private final FareRuleMapper fareRuleMapper;

  private final FareProductMapper fareProductMapper;

  private final FareLegRuleMapper fareLegRuleMapper;

  private final FareTransferRuleMapper fareTransferRuleMapper;

  private final DirectionMapper directionMapper;

  private final DataImportIssueStore issueStore;

  private final OtpTransitServiceBuilder builder;

  private final FareRulesData fareRulesBuilder = new FareRulesData();

  private final TranslationHelper translationHelper;
  private final boolean discardMinTransferTimes;

  public GTFSToOtpTransitServiceMapper(
    OtpTransitServiceBuilder builder,
    String feedId,
    DataImportIssueStore issueStore,
    boolean discardMinTransferTimes,
    StopTransferPriority stationTransferPreference
  ) {
    var idFactory = new IdFactory(feedId);
    this.issueStore = issueStore;
    this.builder = builder;
    // Create callbacks for mappers to retrieve stop and stations
    Function<FeedScopedId, Station> stationLookup = id -> builder.getStations().get(id);
    Function<FeedScopedId, RegularStop> stopLookup = id -> builder.getStops().get(id);

    this.discardMinTransferTimes = discardMinTransferTimes;
    serviceCalendarMapper = new ServiceCalendarMapper(idFactory);
    serviceCalendarDateMapper = new ServiceCalendarDateMapper(idFactory);
    shapePointMapper = new ShapePointMapper(idFactory);
    translationHelper = new TranslationHelper();
    feedInfoMapper = new FeedInfoMapper(feedId);
    agencyMapper = new AgencyMapper(idFactory);
    stationMapper = new StationMapper(idFactory, translationHelper, stationTransferPreference);
    stopMapper = new StopMapper(
      idFactory,
      translationHelper,
      stationLookup,
      builder.siteRepository()
    );
    entranceMapper = new EntranceMapper(idFactory, translationHelper, stationLookup);
    pathwayNodeMapper = new PathwayNodeMapper(idFactory, translationHelper, stationLookup);
    boardingAreaMapper = new BoardingAreaMapper(idFactory, translationHelper, stopLookup);
    locationMapper = new LocationMapper(idFactory, builder.siteRepository(), issueStore);
    locationGroupMapper = new LocationGroupMapper(
      idFactory,
      stopMapper,
      locationMapper,
      builder.siteRepository()
    );
    pathwayMapper = new PathwayMapper(
      idFactory,
      stopMapper,
      entranceMapper,
      pathwayNodeMapper,
      boardingAreaMapper
    );
    routeMapper = new RouteMapper(idFactory, agencyMapper, issueStore, translationHelper);
    directionMapper = new DirectionMapper(issueStore);
    tripMapper = new TripMapper(idFactory, routeMapper, directionMapper, translationHelper);
    bookingRuleMapper = new BookingRuleMapper();
    stopTimeMapper = new StopTimeMapper(
      stopMapper,
      locationMapper,
      locationGroupMapper,
      tripMapper,
      bookingRuleMapper,
      translationHelper
    );
    frequencyMapper = new FrequencyMapper(tripMapper);
    fareAttributeMapper = new FareAttributeMapper(idFactory);
    fareRuleMapper = new FareRuleMapper(routeMapper, fareAttributeMapper);
    fareProductMapper = new FareProductMapper(idFactory);
    fareLegRuleMapper = new FareLegRuleMapper(idFactory, fareProductMapper, issueStore);
    fareTransferRuleMapper = new FareTransferRuleMapper(idFactory, fareProductMapper);
  }

  public OtpTransitServiceBuilder getBuilder() {
    return builder;
  }

  public FareRulesData fareRulesData() {
    return fareRulesBuilder;
  }

  public void mapStopTripAndRouteDataIntoBuilder(GtfsRelationalDao data) {
    translationHelper.importTranslations(data.getAllTranslations(), data.getAllFeedInfos());

    builder.getAgenciesById().addAll(agencyMapper.map(data.getAllAgencies()));
    builder.getCalendarDates().addAll(serviceCalendarDateMapper.map(data.getAllCalendarDates()));
    builder.getCalendars().addAll(serviceCalendarMapper.map(data.getAllCalendars()));
    builder.getFeedInfos().addAll(feedInfoMapper.map(data.getAllFeedInfos()));
    builder.getFrequencies().addAll(frequencyMapper.map(data.getAllFrequencies()));
    builder.getRoutes().addAll(routeMapper.map(data.getAllRoutes()));
    var shapes = shapePointMapper.map(data.getAllShapePoints());
    builder.getShapePoints().putAll(shapes);
    // shape points is a large collection, so after mapping it can be cleared
    data.getAllShapePoints().clear();

    mapGtfsStopsToOtpTypes(data.getAllStops());

    if (OTPFeature.FlexRouting.isOn()) {
      // Stop areas and Stop groups are only used in FLEX routes
      builder.siteRepository().withAreaStops(locationMapper.map(data.getAllLocations()));
      builder.siteRepository().withGroupStops(locationGroupMapper.map(data.getAllLocationGroups()));
    }

    builder.getPathways().addAll(pathwayMapper.map(data.getAllPathways()));
    builder.getStopTimesSortedByTrip().addAll(stopTimeMapper.map(data.getAllStopTimes()));
    builder.getFlexTimePenalty().putAll(tripMapper.flexSafeTimePenalties());
    builder.getTripsById().addAll(tripMapper.map(data.getAllTrips()));

    fareRulesBuilder.fareAttributes().addAll(fareAttributeMapper.map(data.getAllFareAttributes()));
    fareRulesBuilder.fareRules().addAll(fareRuleMapper.map(data.getAllFareRules()));

    // we don't want to store the list of fare products if they are not required by a fare rule
    // or a fare transfer rule, that's why this is not added to the builder
    fareProductMapper.map(data.getAllFareProducts());
    fareRulesBuilder.fareLegRules().addAll(fareLegRuleMapper.map(data.getAllFareLegRules()));
    fareRulesBuilder
      .fareTransferRules()
      .addAll(fareTransferRuleMapper.map(data.getAllFareTransferRules()));
  }

  private void mapGtfsStopsToOtpTypes(Collection<Stop> stops) {
    // Map station first, so we can link to them
    for (Stop it : stops) {
      if (it.getLocationType() == LOCATION_TYPE_STATION) {
        builder.siteRepository().withStation(stationMapper.map(it));
      }
    }

    // Map Stop, Entrance and PathwayNode, link to station
    for (Stop it : stops) {
      if (it.getLocationType() == LOCATION_TYPE_STOP) {
        builder.siteRepository().withRegularStop(stopMapper.map(it));
      } else if (it.getLocationType() == LOCATION_TYPE_ENTRANCE_EXIT) {
        builder.getEntrances().add(entranceMapper.map(it));
      } else if (it.getLocationType() == LOCATION_TYPE_NODE) {
        builder.getPathwayNodes().add(pathwayNodeMapper.map(it));
      }
    }

    // Map BoardingArea, link to stop
    for (Stop it : stops) {
      if (it.getLocationType() == LOCATION_TYPE_BOARDING_AREA) {
        builder.getBoardingAreas().add(boardingAreaMapper.map(it));
      }
    }
  }

  /**
   * Note! Trip-pattens must be added BEFORE mapping transfers
   */
  public void mapAndAddTransfersToBuilder(GtfsDao data) {
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
