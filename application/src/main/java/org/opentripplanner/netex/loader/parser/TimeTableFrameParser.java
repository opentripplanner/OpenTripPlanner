package org.opentripplanner.netex.loader.parser;

import java.util.ArrayList;
import java.util.List;
import org.entur.netex2.model.DatedServiceJourney;
import org.entur.netex2.model.Interchange_VersionStructure;
import org.entur.netex2.model.JourneyInterchangesInFrame_RelStructure;
import org.entur.netex2.model.Journey_VersionStructure;
import org.entur.netex2.model.JourneysInFrame_RelStructure;
import org.entur.netex2.model.ServiceJourney;
import org.entur.netex2.model.ServiceJourneyInterchange;
import org.entur.netex2.model.Timetable_VersionFrameStructure;
import org.opentripplanner.netex.index.NetexEntityIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TimeTableFrameParser extends NetexParser<Timetable_VersionFrameStructure> {

  private static final Logger LOG = LoggerFactory.getLogger(TimeTableFrameParser.class);

  private final List<ServiceJourney> serviceJourneys = new ArrayList<>();
  private final List<DatedServiceJourney> datedServiceJourneys = new ArrayList<>();
  private final List<ServiceJourneyInterchange> serviceJourneyInterchanges = new ArrayList<>();

  private final NoticeParser noticeParser = new NoticeParser();

  @Override
  void parse(Timetable_VersionFrameStructure frame) {
    parseJourneys(frame.getVehicleJourneys());
    parseInterchanges(frame.getJourneyInterchanges());

    noticeParser.parseNotices(frame.getNotices());
    noticeParser.parseNoticeAssignments(frame.getNoticeAssignments());

    warnOnMissingMapping(LOG, frame.getNetworkView());
    warnOnMissingMapping(LOG, frame.getLineView());
    warnOnMissingMapping(LOG, frame.getOperatorView());
    warnOnMissingMapping(LOG, frame.getAccessibilityAssessment());

    // Keep list sorted alphabetically
    warnOnMissingMapping(LOG, frame.getBookingTimes());
    warnOnMissingMapping(LOG, frame.getCoupledJourneys());
    warnOnMissingMapping(LOG, frame.getDefaultInterchanges());
    warnOnMissingMapping(LOG, frame.getFlexibleServiceProperties());
    warnOnMissingMapping(LOG, frame.getFrequencyGroups());
    warnOnMissingMapping(LOG, frame.getGroupsOfServices());
    warnOnMissingMapping(LOG, frame.getInterchangeRules());
    warnOnMissingMapping(LOG, frame.getJourneyAccountingRef());
    warnOnMissingMapping(LOG, frame.getJourneyAccountings());
    warnOnMissingMapping(LOG, frame.getJourneyMeetings());
    warnOnMissingMapping(LOG, frame.getJourneyPartCouples());
    warnOnMissingMapping(LOG, frame.getServiceCalendarFrameRef());
    warnOnMissingMapping(LOG, frame.getServiceFacilitySets());
    warnOnMissingMapping(LOG, frame.getTimeDemandTypes());
    warnOnMissingMapping(LOG, frame.getTimeDemandTypeAssignments());
    warnOnMissingMapping(LOG, frame.getTimingLinkGroups());
    warnOnMissingMapping(LOG, frame.getTrainNumbers());
    warnOnMissingMapping(LOG, frame.getTypesOfService());
    warnOnMissingMapping(LOG, frame.getVehicleTypes());

    verifyCommonUnusedPropertiesIsNotSet(LOG, frame);
  }

  @Override
  void setResultOnIndex(NetexEntityIndex netexIndex) {
    netexIndex.serviceJourneyById.addAll(serviceJourneys);
    netexIndex.datedServiceJourneys.addAll(datedServiceJourneys);
    netexIndex.serviceJourneyInterchangeById.addAll(serviceJourneyInterchanges);
    noticeParser.setResultOnIndex(netexIndex);
  }

  private void parseJourneys(JourneysInFrame_RelStructure element) {
    if (element == null) {
      return;
    }
    for (Journey_VersionStructure it : element.getVehicleJourneyOrDatedVehicleJourneyOrNormalDatedVehicleJourney()) {
      if (it instanceof ServiceJourney serviceJourney) {
        serviceJourneys.add(serviceJourney);
      } else if (it instanceof DatedServiceJourney datedServiceJourney) {
        datedServiceJourneys.add(datedServiceJourney);
      } else {
        warnOnMissingMapping(LOG, it);
      }
    }
  }

  private void parseInterchanges(JourneyInterchangesInFrame_RelStructure element) {
    if (element == null) {
      return;
    }
    var list = element.getServiceJourneyPatternInterchangeOrServiceJourneyInterchange();
    for (Interchange_VersionStructure it : list) {
      if (it instanceof ServiceJourneyInterchange serviceJourneyInterchange) {
        serviceJourneyInterchanges.add(serviceJourneyInterchange);
      } else {
        warnOnMissingMapping(LOG, it);
      }
    }
  }
}
