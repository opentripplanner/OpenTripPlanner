package org.opentripplanner.netex.loader.parser;

import java.util.ArrayList;
import java.util.Collection;
import org.entur.netex2.model.MobilityJourney_VersionFrameStructure;
import org.entur.netex2.model.SingleJourney;
import org.entur.netex2.model.SingleJourneyPath;
import org.opentripplanner.netex.index.NetexEntityIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MobilityJourneyFrameParser extends NetexParser<MobilityJourney_VersionFrameStructure> {

  private static final Logger LOG = LoggerFactory.getLogger(MobilityJourneyFrameParser.class);

  private final Collection<SingleJourneyPath> singleJourneyPaths = new ArrayList<>();
  private final Collection<SingleJourney> singleJourneys = new ArrayList<>();

  @Override
  void parse(MobilityJourney_VersionFrameStructure frame) {
    if (frame.getSingleJourneyPaths() != null) {
      singleJourneyPaths.addAll(frame.getSingleJourneyPaths().getSingleJourneyPath());
    }
    if (frame.getSingleJourneys() != null) {
      singleJourneys.addAll(frame.getSingleJourneys().getSingleJourney());
    }

    // Data about travellers and their use of vehicles is not relevant for routing
    informOnElementIntentionallySkipped(LOG, frame.getIndividualTravellers());
    informOnElementIntentionallySkipped(LOG, frame.getParkingLogEntries());
    informOnElementIntentionallySkipped(LOG, frame.getVehicleAccessCredentials());

    warnOnMissingMapping(LOG, frame.getGroupsOfSingleJourneys());

    verifyCommonUnusedPropertiesIsNotSet(LOG, frame);
  }

  @Override
  void setResultOnIndex(NetexEntityIndex index) {
    index.singleJourneyPathById.addAll(singleJourneyPaths);
    index.singleJourneyById.addAll(singleJourneys);
  }
}
