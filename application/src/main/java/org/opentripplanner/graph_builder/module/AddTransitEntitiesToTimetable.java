package org.opentripplanner.graph_builder.module;

import java.util.Collection;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.service.TimetableRepository;

public class AddTransitEntitiesToTimetable {

  private final OtpTransitService otpTransitService;

  private AddTransitEntitiesToTimetable(OtpTransitService otpTransitService) {
    this.otpTransitService = otpTransitService;
  }

  public static void addToTimetable(
    OtpTransitService otpTransitService,
    TimetableRepository timetableRepository
  ) {
    new AddTransitEntitiesToTimetable(otpTransitService).applyToTimetableRepository(
      timetableRepository
    );
  }

  private void applyToTimetableRepository(TimetableRepository timetableRepository) {
    timetableRepository.mergeSiteRepositories(otpTransitService.siteRepository());

    // Netex specific entities
    for (var tripOnServiceDate : otpTransitService.getTripOnServiceDates()) {
      timetableRepository.addTripOnServiceDate(tripOnServiceDate);
    }
    timetableRepository.addOperators(otpTransitService.getAllOperators());
    timetableRepository.addNoticeAssignments(otpTransitService.getNoticeAssignments());
    timetableRepository.addScheduledStopPointMapping(otpTransitService.stopsByScheduledStopPoint());

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
    for (FeedInfo info : otpTransitService.getAllFeedInfos()) {
      timetableRepository.addFeedInfo(info);
    }
  }

  private void addAgencies(TimetableRepository timetableRepository) {
    for (Agency agency : otpTransitService.getAllAgencies()) {
      timetableRepository.addAgency(agency);
    }
  }

  private void addTransfers(TimetableRepository timetableRepository) {
    timetableRepository.getTransferService().addAll(otpTransitService.getAllTransfers());
  }

  private void addServices(TimetableRepository timetableRepository) {
    /* Assign 0-based numeric codes to all GTFS service IDs. */
    for (FeedScopedId serviceId : otpTransitService.getAllServiceIds()) {
      timetableRepository
        .getServiceCodes()
        .put(serviceId, timetableRepository.getServiceCodes().size());
    }
  }

  private void addTripPatterns(TimetableRepository timetableRepository) {
    Collection<TripPattern> tripPatterns = otpTransitService.getTripPatterns();

    /* Loop over all new TripPatterns setting the service codes. */
    for (TripPattern tripPattern : tripPatterns) {
      // TODO this could be more elegant
      tripPattern.getScheduledTimetable().setServiceCodes(timetableRepository.getServiceCodes());

      // Store the tripPattern in the timetable repository so it will be serialized and usable in routing.
      timetableRepository.addTripPattern(tripPattern.getId(), tripPattern);
    }
  }

  private void addFlexTrips(TimetableRepository timetableRepository) {
    for (FlexTrip<?, ?> flexTrip : otpTransitService.getAllFlexTrips()) {
      timetableRepository.addFlexTrip(flexTrip.getId(), flexTrip);
    }
  }
}
