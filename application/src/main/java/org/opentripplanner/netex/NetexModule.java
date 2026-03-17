package org.opentripplanner.netex;

import java.util.List;
import org.opentripplanner.core.framework.deduplicator.DeduplicatorService;
import org.opentripplanner.core.model.time.LocalDateInterval;
import org.opentripplanner.ext.flex.FlexTripsMapper;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.graph_builder.module.AddTransitEntitiesToGraph;
import org.opentripplanner.graph_builder.module.AddTransitEntitiesToTimetable;
import org.opentripplanner.graph_builder.module.TransitWithFutureDateValidator;
import org.opentripplanner.graph_builder.module.ValidateAndInterpolateStopTimesForEachTrip;
import org.opentripplanner.model.TransitDataImport;
import org.opentripplanner.model.TripStopTimes;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.impl.TransitDataImportBuilder;
import org.opentripplanner.service.streetdetails.StreetDetailsRepository;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.linking.VehicleParkingHelper;
import org.opentripplanner.transit.service.TimetableRepository;

/**
 * This module is used for importing the NeTEx CEN Technical Standard for exchanging Public
 * Transport schedules and related data (<a href="http://netex-cen.eu/">http://netex-cen.eu/</a>).
 * Currently it only supports the Norwegian profile (<a href="https://enturas.atlassian.net/wiki/spaces/PUBLIC/">https://enturas.atlassian.net/wiki/spaces/PUBLIC/</a>),
 * but it is intended to be updated later to support other profiles.
 */
public class NetexModule implements GraphBuilderModule {

  private final int subwayAccessTime;

  private final Graph graph;
  private final DeduplicatorService deduplicator;
  private final TimetableRepository timetableRepository;
  private final VehicleParkingRepository parkingRepository;
  private final StreetDetailsRepository streetDetailsRepository;
  private final DataImportIssueStore issueStore;

  /**
   * @see BuildConfig#transitServiceStart
   * @see BuildConfig#transitServiceEnd
   */
  private final LocalDateInterval transitPeriodLimit;

  private final List<NetexBundle> netexBundles;

  public NetexModule(
    Graph graph,
    DeduplicatorService deduplicator,
    TimetableRepository timetableRepository,
    VehicleParkingRepository parkingRepository,
    StreetDetailsRepository streetDetailsRepository,
    DataImportIssueStore issueStore,
    int subwayAccessTime,
    LocalDateInterval transitPeriodLimit,
    List<NetexBundle> netexBundles
  ) {
    this.graph = graph;
    this.deduplicator = deduplicator;
    this.timetableRepository = timetableRepository;
    this.parkingRepository = parkingRepository;
    this.streetDetailsRepository = streetDetailsRepository;
    this.issueStore = issueStore;
    this.subwayAccessTime = subwayAccessTime;
    this.transitPeriodLimit = transitPeriodLimit;
    this.netexBundles = netexBundles;
  }

  @Override
  public void buildGraph() {
    try {
      var calendarServiceData = new CalendarServiceData();

      for (NetexBundle netexBundle : netexBundles) {
        netexBundle.checkInputs();

        TransitDataImportBuilder transitBuilder = netexBundle.loadBundle(deduplicator, issueStore);
        transitBuilder.limitServiceDays(transitPeriodLimit);
        calendarServiceData.add(transitBuilder.buildCalendarServiceData());

        if (OTPFeature.FlexRouting.isOn()) {
          transitBuilder
            .getFlexTripsById()
            .addAll(FlexTripsMapper.createFlexTrips(transitBuilder, issueStore));
        }

        validateStopTimesForEachTrip(transitBuilder.getStopTimesSortedByTrip());

        TransitDataImport otpService = transitBuilder.build();

        AddTransitEntitiesToTimetable.addToTimetable(otpService, timetableRepository);
        AddTransitEntitiesToGraph.addToGraph(
          otpService,
          subwayAccessTime,
          graph,
          streetDetailsRepository
        );

        var lots = transitBuilder.vehicleParkings();
        parkingRepository.updateVehicleParking(lots, List.of());
        var linker = new VehicleParkingHelper(graph);
        lots.forEach(linker::linkVehicleParkingToGraph);
      }

      timetableRepository.updateCalendarServiceData(calendarServiceData);

      TransitWithFutureDateValidator.validate(
        calendarServiceData,
        issueStore,
        timetableRepository.getTimeZone()
      );
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void validateStopTimesForEachTrip(TripStopTimes stopTimesByTrip) {
    new ValidateAndInterpolateStopTimesForEachTrip(stopTimesByTrip, false, issueStore).run();
  }

  @Override
  public void checkInputs() {
    netexBundles.forEach(NetexBundle::checkInputs);
  }
}
