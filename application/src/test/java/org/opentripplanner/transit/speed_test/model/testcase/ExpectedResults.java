package org.opentripplanner.transit.speed_test.model.testcase;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.transit.speed_test.model.SpeedTestProfile;

/**
 * This class contains the results for a given test case. A set of results
 * for each {@link SpeedTestProfile} is kept. A default set is also available,
 * which can be used if there is not set for a given profile.
 */
public class ExpectedResults {

  private final List<Result> defaults = new ArrayList<>();
  private final Multimap<SpeedTestProfile, Result> map = ArrayListMultimap.create();

  /**
   * Add the given result to the default set of results.
   */
  public void addDefault(Result result) {
    defaults.add(result);
  }

  /**
   * Add the given result to the set of results for the given profile.
   */
  public void add(SpeedTestProfile profile, Result result) {
    map.put(profile, result);
  }

  /**
   * Retrive a set of results for the given profile. If not set exist for the
   * profile, the default set is returned.
   */
  public Collection<Result> get(SpeedTestProfile profile) {
    var c = map.get(profile);
    return c.isEmpty() ? defaults : c;
  }
}
