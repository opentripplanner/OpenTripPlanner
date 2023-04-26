package org.opentripplanner.netex.validation;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.netex.issues.ObjectNotFound;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.ServiceJourneyInterchange;

class InterchangeServiceJourneyNotFound extends AbstractHMapValidationRule<String, ServiceJourneyInterchange> {

  private String missingFromServiceJourneyId;
  private String missingToServiceJourneyId;

  @Override
  public Status validate(ServiceJourneyInterchange interchange) {
    String serviceJourneyFromRef = interchange.getFromJourneyRef().getRef();
    String serviceJourneyToRef = interchange.getToJourneyRef().getRef();
    ServiceJourney fromServiceJourney = index.getServiceJourneyById().lookup(serviceJourneyFromRef);
    ServiceJourney toServiceJourney = index.getServiceJourneyById().lookup(serviceJourneyToRef);
    if (fromServiceJourney == null) {
      missingFromServiceJourneyId = serviceJourneyFromRef;
      return Status.DISCARD;
    } else if (toServiceJourney == null) {
      missingToServiceJourneyId = serviceJourneyToRef;
      return Status.DISCARD;
    }
    return Status.OK;
  }

  @Override
  public DataImportIssue logMessage(String dsjId, ServiceJourneyInterchange interchange) {
    String targetFieldName = missingFromServiceJourneyId != null ? "FromJourneyRef" : "ToJourneyRef";
    String missingServiceJourneyId = missingFromServiceJourneyId != null ? missingFromServiceJourneyId : missingToServiceJourneyId;
    return new ObjectNotFound("ServiceJourneyInterchange", interchange.getId(), targetFieldName, missingServiceJourneyId);
  }


}
