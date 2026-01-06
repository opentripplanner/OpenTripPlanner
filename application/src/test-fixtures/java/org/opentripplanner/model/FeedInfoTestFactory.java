package org.opentripplanner.model;

public class FeedInfoTestFactory {

  public static FeedInfo dummyForTest(String id) {
    return new FeedInfo(id, "publisher", "www.z.org", "en", null, null, null);
  }
}
