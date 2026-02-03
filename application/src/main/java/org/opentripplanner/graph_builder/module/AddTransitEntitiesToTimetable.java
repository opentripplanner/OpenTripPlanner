package org.opentripplanner.graph_builder.module;

import java.util.Collection;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.TransitDataImport;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.service.TimetableRepository;

public class AddTransitEntitiesToTimetable {

  private final TransitDataImport dataImport;

  private AddTransitEntitiesToTimetable(TransitDataImport dataImport) {
    this.dataImport = dataImport;
  }

  public static void addToTimetable(
    TransitDataImport dataImport,
    TimetableRepository timetableRepository
  ) {
    new AddTransitEntitiesToTimetable(dataImport).applyToTimetableRepository(timetableRepository);
  }

  private void applyToTimetableRepository(TimetableRepository timetableRepository) {
    timetableRepository.mergeSiteRepositories(dataImport.siteRepository());

    // Netex specific entities
    for (var tripOnServiceDate : dataImport.getTripOnServiceDates()) {
      timetableRepository.addTripOnServiceDate(tripOnServiceDate);
    }
    timetableRepository.addOperators(dataImport.getAllOperators());
    timetableRepository.addNoticeAssignments(dataImport.getNoticeAssignments());
    timetableRepository.addScheduledStopPointMapping(dataImport.stopsByScheduledStopPoint());

    addFeedInfo(timetableRepository);
    addAgencies(timetableRepository);
    addServices(timetableRepository);
    addTripPatterns(timetableRepository);

    /* Interpret the transfers explicitly defined in transfers.txt. */
    addTransfers(timetableRepository);

    if (OTPFeature.FlexRouting.isOn()) {
      addFlexTrips(timetableRepository);
    }
  }

  private void addFeedInfo(TimetableRepository timetableRepository) {
    for (FeedInfo info : dataImport.getAllFeedInfos()) {
      timetableRepository.addFeedInfo(info);
    }
  }

  private void addAgencies(TimetableRepository timetableRepository) {
    for (Agency agency : dataImport.getAllAgencies()) {
      timetableRepository.addAgency(agency);
    }
  }

  private void addTransfers(TimetableRepository timetableRepository) {
    timetableRepository.getConstrainedTransferService().addAll(dataImport.getAllTransfers());
  }

  private void addServices(TimetableRepository timetableRepository) {
    /* Assign 0-based numeric codes to all GTFS service IDs. */
    for (FeedScopedId serviceId : dataImport.getAllServiceIds()) {
      timetableRepository
        .getServiceCodes()
        .put(serviceId, timetableRepository.getServiceCodes().size());
    }
  }

  private void addTripPatterns(TimetableRepository timetableRepository) {
    Collection<TripPattern> tripPatterns = dataImport.getTripPatterns();

    /* Loop over all new TripPatterns setting the service codes. */
    for (TripPattern tripPattern : tripPatterns) {
      // TODO this could be more elegant
      tripPattern.getScheduledTimetable().setServiceCodes(timetableRepository.getServiceCodes());

      // Store the tripPattern in the timetable repository so it will be serialized and usable in routing.
      timetableRepository.addTripPattern(tripPattern.getId(), tripPattern);
    }
  }

  private void addFlexTrips(TimetableRepository timetableRepository) {
    for (FlexTrip<?, ?> flexTrip : dataImport.getAllFlexTrips()) {
      timetableRepository.addFlexTrip(flexTrip.getId(), flexTrip);
    }
  }
}
