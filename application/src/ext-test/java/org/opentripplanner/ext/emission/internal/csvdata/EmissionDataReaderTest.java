package org.opentripplanner.ext.emission.internal.csvdata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.emission.EmissionRepository;
import org.opentripplanner.ext.emission.EmissionTestData;
import org.opentripplanner.ext.emission.internal.DefaultEmissionRepository;
import org.opentripplanner.ext.emission.internal.csvdata.trip.TripHopMapper;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;

class EmissionDataReaderTest implements EmissionTestData {

  private TimetableRepositoryForTest data = TimetableRepositoryForTest.of();
  private final EmissionRepository repository = new DefaultEmissionRepository();
  private DataImportIssueStore issueStore = new DefaultDataImportIssueStore();

  private StopLocation STOP_A = data.stop("A").build();
  private StopLocation STOP_B = data.stop("B").build();
  private StopLocation STOP_C = data.stop("C").build();

  private Map<FeedScopedId, List<StopLocation>> stopsByTripId = Map.of(
    new FeedScopedId("em", "T1"),
    List.of(STOP_A, STOP_B, STOP_C),
    new FeedScopedId("em", "T2"),
    List.of(STOP_A, STOP_B, STOP_C)
  );

  private TripHopMapper tripHopMapper = new TripHopMapper(stopsByTripId, issueStore);

  private final EmissionDataReader subject = new EmissionDataReader(
    repository,
    tripHopMapper,
    issueStore
  );

  @Test
  void readEmissionsFromGtfsZip() {
    subject.read(gtfsWithEmissionZip(), "gz");
    assertEquals(
      "Emission Summary - route: 3 / trip: 0 / total: 3",
      repository.summary().toString()
    );
  }

  @Test
  void readEmissionsFromGtfsDirectory() {
    subject.read(gtfsWithEmissionDir(), "gd");
    assertEquals(
      "Emission Summary - route: 3 / trip: 0 / total: 3",
      repository.summary().toString()
    );
  }

  @Test
  void readEmissionOnTripHops() {
    subject.read(emissionOnTripHops(), "em");
    assertEquals(
      "Emission Summary - route: 0 / trip: 2 / total: 2",
      repository.summary().toString()
    );
  }

  @Test
  void readGtfsDirectoryFeedWithoutEmissions() {
    subject.read(gtfsDirectoryDataSourceWithoutEmissions(), "F");
    assertEquals(
      "Emission Summary - route: 0 / trip: 0 / total: 0",
      repository.summary().toString()
    );
  }

  @Test
  void readGtfsZipFeedWithoutEmissions() {
    subject.read(gtfsZipDataSourceWithoutEmissions(), "F");
    assertEquals(
      "Emission Summary - route: 0 / trip: 0 / total: 0",
      repository.summary().toString()
    );
  }
}
