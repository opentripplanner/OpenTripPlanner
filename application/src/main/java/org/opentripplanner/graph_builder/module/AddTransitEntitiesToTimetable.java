package org.opentripplanner.graph_builder.module;

import java.util.Collection;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.TransitDataImport;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.repository.TimetableBuildRepository;
import org.opentripplanner.transit.service.TransitRepository;

public class AddTransitEntitiesToTimetable {

  private final TransitDataImport dataImport;
  private final TimetableBuildRepository timetableBuildRepository;

  private AddTransitEntitiesToTimetable(
    TransitDataImport dataImport,
    TimetableBuildRepository timetableBuildRepository
  ) {
    this.dataImport = dataImport;
    this.timetableBuildRepository = timetableBuildRepository;
  }

  public static void addToTimetable(
    TransitDataImport dataImport,
    TransitRepository transitRepository,
    TimetableBuildRepository timetableBuildRepository
  ) {
    new AddTransitEntitiesToTimetable(
      dataImport,
      timetableBuildRepository
    ).applyToTransitRepository(transitRepository);
  }

  private void applyToTransitRepository(TransitRepository transitRepository) {
    transitRepository.mergeSiteRepositories(dataImport.siteRepository());

    // Netex specific entities
    for (var tripOnServiceDate : dataImport.getTripOnServiceDates()) {
      timetableBuildRepository.addTripOnServiceDate(tripOnServiceDate);
    }
    transitRepository.addOperators(dataImport.getAllOperators());
    transitRepository.addNoticeAssignments(dataImport.getNoticeAssignments());
    transitRepository.addScheduledStopPointMapping(dataImport.stopsByScheduledStopPoint());

    addFeedInfo(transitRepository);
    addAgencies(transitRepository);
    addServices();
    addTripPatterns();

    /* Interpret the transfers explicitly defined in transfers.txt. */
    addTransfers(transitRepository);

    if (OTPFeature.FlexRouting.isOn()) {
      addFlexTrips();
    }
  }

  private void addFeedInfo(TransitRepository transitRepository) {
    for (FeedInfo info : dataImport.getAllFeedInfos()) {
      transitRepository.addFeedInfo(info);
    }
  }

  private void addAgencies(TransitRepository transitRepository) {
    for (Agency agency : dataImport.getAllAgencies()) {
      transitRepository.addAgency(agency);
    }
  }

  private void addTransfers(TransitRepository transitRepository) {
    transitRepository.getConstrainedTransferService().addAll(dataImport.getAllTransfers());
  }

  private void addServices() {
    /* Assign 0-based numeric codes to all GTFS service IDs. */
    for (FeedScopedId serviceId : dataImport.getAllServiceIds()) {
      timetableBuildRepository.putServiceCode(
        serviceId,
        timetableBuildRepository.getServiceCodes().size()
      );
    }
  }

  private void addTripPatterns() {
    Collection<TripPattern> tripPatterns = dataImport.getTripPatterns();

    /* Loop over all new TripPatterns setting the service codes. */
    for (TripPattern tripPattern : tripPatterns) {
      // TODO this could be more elegant
      tripPattern
        .getScheduledTimetable()
        .setServiceCodes(timetableBuildRepository.getServiceCodes());

      // Store the tripPattern in the scheduled timetable data so it will be serialized and usable in routing.
      timetableBuildRepository.addTripPattern(tripPattern.getId(), tripPattern);
    }
  }

  private void addFlexTrips() {
    for (FlexTrip<?, ?> flexTrip : dataImport.getAllFlexTrips()) {
      timetableBuildRepository.addFlexTrip(flexTrip.getId(), flexTrip);
    }
  }
}
