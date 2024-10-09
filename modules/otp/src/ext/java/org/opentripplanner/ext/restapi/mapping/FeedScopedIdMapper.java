package org.opentripplanner.ext.restapi.mapping;

import jakarta.ws.rs.BadRequestException;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class FeedScopedIdMapper {

  private static final String SEPARATOR = ":";

  /** @throws BadRequestException if id is not parsable. */
  public static FeedScopedId mapToDomain(String name, String api) {
    try {
      if (api == null) {
        return null;
      }

      String[] parts = api.split(SEPARATOR, 2);
      return new FeedScopedId(parts[0], parts[1]);
    } catch (IndexOutOfBoundsException e) {
      throw new BadRequestException(
        "'" + name + "' is not a valid id on the form: FEED_ID" + SEPARATOR + "ENTITY_ID"
      );
    }
  }

  public static String mapToApi(FeedScopedId arg) {
    if (arg == null) {
      return null;
    }
    return arg.getFeedId() + SEPARATOR + arg.getId();
  }

  public static String mapIdToApi(AbstractTransitEntity entity) {
    return entity == null ? null : mapToApi(entity.getId());
  }
}
