package org.opentripplanner.netex.mapping;

import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.transit.model.network.GroupOfRoutes;
import org.rutebanken.netex.model.GroupOfLines;

/**
 * Responsible for mapping NeTEx GroupOfLines into the OTP model.
 */
public class GroupOfRoutesMapper {

  private final FeedScopedIdFactory idFactory;

  public GroupOfRoutesMapper(FeedScopedIdFactory idFactory) {
    this.idFactory = idFactory;
  }

  /**
   * Convert NeTEx GroupOfLines Entity into OTP model.
   *
   * @param gol NeTEx GroupOfLines entity.
   * @return OTP GroupOfRoutes model
   */
  public GroupOfRoutes mapGroupOfRoutes(GroupOfLines gol) {
    return GroupOfRoutes.of(idFactory.createId(gol.getId()))
      .withPrivateCode(gol.getPrivateCode() != null ? gol.getPrivateCode().getValue() : null)
      .withShortName(MultilingualStringMapper.nullableValueOf(gol.getShortName()))
      .withName(MultilingualStringMapper.nullableValueOf(gol.getName()))
      .withDescription(MultilingualStringMapper.nullableValueOf(gol.getDescription()))
      .build();
  }
}
