package org.opentripplanner.netex.validation;

import java.util.List;
import javax.annotation.Nullable;
import javax.xml.bind.JAXBElement;
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
    List<JAXBElement<? extends JourneyRefStructure>> journeyRef = dsj.getJourneyRef();

    if (journeyRef == null || journeyRef.isEmpty()) {
      return null;
    }
    JourneyRefStructure ref = journeyRef.get(0).getValue();
    return ref == null ? null : ref.getRef();
  }
}
