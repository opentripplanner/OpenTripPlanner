package org.opentripplanner.netex.validation;

import jakarta.xml.bind.JAXBElement;
import javax.annotation.Nullable;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.netex.issues.ObjectNotFound;
import org.rutebanken.netex.model.DatedServiceJourney;
import org.rutebanken.netex.model.JourneyRefStructure;

class DSJServiceJourneyNotFound extends AbstractHMapValidationRule<String, DatedServiceJourney> {

  @Override
  public Status validate(DatedServiceJourney dsj) {
    var ref = getServiceJourneyRef(dsj);
    var sj = index.getServiceJourneyById().lookup(ref);
    return sj == null ? Status.DISCARD : Status.OK;
  }

  @Override
  public DataImportIssue logMessage(String dsjId, DatedServiceJourney dsj) {
    String ref = getServiceJourneyRef(dsj);

    return new ObjectNotFound("DatedServiceJourney", dsj.getId(), "ServiceJourneyRef", ref);
  }

  @Nullable
  private String getServiceJourneyRef(DatedServiceJourney dsj) {
    JAXBElement<? extends JourneyRefStructure> journeyRef = dsj.getJourneyRef();

    if (journeyRef == null) {
      return null;
    }
    JourneyRefStructure ref = journeyRef.getValue();
    return ref == null ? null : ref.getRef();
  }
}
