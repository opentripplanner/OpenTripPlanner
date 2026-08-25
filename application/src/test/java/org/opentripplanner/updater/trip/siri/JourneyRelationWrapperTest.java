package org.opentripplanner.updater.trip.siri;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.LocalTimeParser;
import uk.org.siri.siri21.JourneyRelationStructure;
import uk.org.siri.siri21.JourneyRelationTypeEnumeration;
import uk.org.siri.siri21.RelatedJourney;
import uk.org.siri.siri21.RelatedJourneyPartStructure;
import uk.org.siri.siri21.StopPointRefStructure;

class JourneyRelationWrapperTest {

  private final LocalDate DATE = LocalDate.of(2000, 1, 2);
  private final ZoneId ZONE_ID = ZoneId.of("Europe/Paris");
  private final LocalTimeParser TIME_PARSER = new LocalTimeParser(ZONE_ID, DATE);

  @Test
  void isReplacedBy() {
    var relation = new JourneyRelationStructure();
    relation.setJourneyRelationType(JourneyRelationTypeEnumeration.REPLACED_BY_JOURNEY);
    var wrapper = new JourneyRelationWrapper(relation, List.of());
    assertTrue(wrapper.isReplacedBy());

    relation.setJourneyRelationType(JourneyRelationTypeEnumeration.REPLACEMENT_OF_JOURNEY);
    assertFalse(wrapper.isReplacedBy());
  }

  @Test
  void relatedJourneys() {
    var id = "123";

    var relation = new JourneyRelationStructure();
    var related = new RelatedJourney();
    related.setFramedVehicleJourneyRef(
      new SiriEtBuilder.FramedVehicleRefBuilder()
        .withVehicleJourneyRef(id)
        .withServiceDate(DATE)
        .build()
    );
    relation.getRelatedJourneies().add(related);
    var wrapper = new JourneyRelationWrapper(relation, List.of());

    assertEquals(List.of(new VehicleJourneyIdAndServiceDate(id, DATE)), wrapper.relatedJourneys());
  }

  @Test
  void journeyParts() {
    var calls = calls("a", "b", "c", "d");
    var relation = relationWithPart("b", "c");
    var wrapper = new JourneyRelationWrapper(relation, calls);

    assertThat(wrapper.journeyParts()).containsExactly(new JourneyPartData(1, 2));
  }

  @Test
  void journeyPartsUnmatchedStop() {
    var calls = calls("a", "b", "c", "d");
    var relation = relationWithPart("unmatched", "d");
    var wrapper = new JourneyRelationWrapper(relation, calls);

    assertThat(wrapper.journeyParts()).isEmpty();
  }

  @Test
  void journeyPartsInvalidOrder() {
    var calls = calls("a", "b", "c", "d");
    var relation = relationWithPart("c", "b");
    var wrapper = new JourneyRelationWrapper(relation, calls);

    assertThat(wrapper.journeyParts()).isEmpty();
  }

  private JourneyRelationStructure relationWithPart(String fromStop, String toStop) {
    var part = new RelatedJourneyPartStructure();
    part.setFromStopPointRef(stopPointRef(fromStop));
    part.setToStopPointRef(stopPointRef(toStop));

    var parts = new RelatedJourney.JourneyParts();
    parts.getJourneyPartInfos().add(part);
    var relation = new JourneyRelationStructure();
    relation.setJourneyParts(parts);

    return relation;
  }

  private List<CallWrapper> calls(String... stopRefs) {
    var journey = new SiriEtBuilder(TIME_PARSER)
      .withEstimatedCalls(b -> {
        for (var stop : stopRefs) {
          b.call(stop);
        }
        return b;
      })
      .buildEstimatedVehicleJourney();
    return CallWrapper.of(journey);
  }

  private StopPointRefStructure stopPointRef(String value) {
    var ref = new StopPointRefStructure();
    ref.setValue(value);
    return ref;
  }
}
