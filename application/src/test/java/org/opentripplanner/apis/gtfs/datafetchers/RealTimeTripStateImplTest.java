package org.opentripplanner.apis.gtfs.datafetchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.apis.support.graphql.DataFetchingSupport.dataFetchingEnvironment;

import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.gtfs.model.RealTimeTripStateModel;

class RealTimeTripStateImplTest {

  private static final RealTimeTripStateImpl SUBJECT = new RealTimeTripStateImpl();

  // All flags false – the baseline "unmodified scheduled trip" case
  private static final RealTimeTripStateModel ALL_FALSE = new RealTimeTripStateModel(
    false,
    false,
    false,
    false,
    false,
    false
  );

  // All flags true – sanity-check that every field is wired independently
  private static final RealTimeTripStateModel ALL_TRUE = new RealTimeTripStateModel(
    true,
    true,
    true,
    true,
    true,
    true
  );

  @Test
  void allFlagsAreFalse_whenNoUpdatesPresent() throws Exception {
    var env = dataFetchingEnvironment(ALL_FALSE);
    assertEquals(false, SUBJECT.added().get(env));
    assertEquals(false, SUBJECT.canceled().get(env));
    assertEquals(false, SUBJECT.deleted().get(env));
    assertEquals(false, SUBJECT.timesModified().get(env));
    assertEquals(false, SUBJECT.tripPatternModified().get(env));
    assertEquals(false, SUBJECT.updated().get(env));
  }

  @Test
  void allFlagsAreTrue_whenAllUpdatesPresent() throws Exception {
    var env = dataFetchingEnvironment(ALL_TRUE);
    assertEquals(true, SUBJECT.added().get(env));
    assertEquals(true, SUBJECT.canceled().get(env));
    assertEquals(true, SUBJECT.deleted().get(env));
    assertEquals(true, SUBJECT.timesModified().get(env));
    assertEquals(true, SUBJECT.tripPatternModified().get(env));
    assertEquals(true, SUBJECT.updated().get(env));
  }

  // Individual flag tests verify that each field is wired to the correct record component
  // (guards against copy-paste errors in the six nearly-identical DataFetcher methods).

  @Test
  void addedFlagIsIsolated() throws Exception {
    var model = new RealTimeTripStateModel(true, false, false, false, false, true);
    var env = dataFetchingEnvironment(model);
    assertEquals(true, SUBJECT.added().get(env));
    assertEquals(false, SUBJECT.canceled().get(env));
    assertEquals(false, SUBJECT.deleted().get(env));
    assertEquals(false, SUBJECT.timesModified().get(env));
    assertEquals(false, SUBJECT.tripPatternModified().get(env));
    assertEquals(true, SUBJECT.updated().get(env));
  }

  @Test
  void canceledFlagIsIsolated() throws Exception {
    var model = new RealTimeTripStateModel(false, true, false, false, false, true);
    var env = dataFetchingEnvironment(model);
    assertEquals(false, SUBJECT.added().get(env));
    assertEquals(true, SUBJECT.canceled().get(env));
    assertEquals(false, SUBJECT.deleted().get(env));
  }

  @Test
  void deletedFlagIsIsolated() throws Exception {
    var model = new RealTimeTripStateModel(false, false, true, false, false, true);
    var env = dataFetchingEnvironment(model);
    assertEquals(false, SUBJECT.canceled().get(env));
    assertEquals(true, SUBJECT.deleted().get(env));
    assertEquals(false, SUBJECT.timesModified().get(env));
    assertEquals(true, SUBJECT.updated().get(env));
  }

  @Test
  void timesModifiedFlagIsIsolated() throws Exception {
    var model = new RealTimeTripStateModel(false, false, false, true, false, true);
    var env = dataFetchingEnvironment(model);
    assertEquals(false, SUBJECT.deleted().get(env));
    assertEquals(true, SUBJECT.timesModified().get(env));
    assertEquals(false, SUBJECT.tripPatternModified().get(env));
  }

  @Test
  void tripPatternModifiedFlagIsIsolated() throws Exception {
    var model = new RealTimeTripStateModel(false, false, false, false, true, true);
    var env = dataFetchingEnvironment(model);
    assertEquals(false, SUBJECT.timesModified().get(env));
    assertEquals(true, SUBJECT.tripPatternModified().get(env));
    assertEquals(true, SUBJECT.updated().get(env));
  }

  @Test
  void updatedFlagIsIsolated() throws Exception {
    var model = new RealTimeTripStateModel(false, false, false, false, false, true);
    var env = dataFetchingEnvironment(model);
    assertEquals(false, SUBJECT.tripPatternModified().get(env));
    assertEquals(true, SUBJECT.updated().get(env));
  }
}
