package org.opentripplanner.transit.transfer.regular;

import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.spi.TransferRequestMapper;

/**
 * Test double: the request is the walk preferences themselves, so a test can use the preferences
 * as the routing request.
 */
public class FakeRequestMapper implements TransferRequestMapper<WalkPreferences> {

  public static final FakeRequestMapper INSTANCE = new FakeRequestMapper();

  private FakeRequestMapper() {}

  @Override
  public WalkPreferences toRequest(
    AbstractUserPreferences<?> userPreferences,
    WalkPreferences base
  ) {
    return (WalkPreferences) userPreferences;
  }

  @Override
  public AbstractUserPreferences<?> toUserPreferences(WalkPreferences request) {
    return request;
  }
}
