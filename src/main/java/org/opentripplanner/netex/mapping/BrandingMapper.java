package org.opentripplanner.netex.mapping;

import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.transit.model.organization.Branding;
import org.opentripplanner.transit.model.organization.BrandingBuilder;

/**
 * Responsible for mapping NeTEx Branding into the OTP model.
 */
public class BrandingMapper {

  private final FeedScopedIdFactory idFactory;

  public BrandingMapper(FeedScopedIdFactory idFactory) {
    this.idFactory = idFactory;
  }

  /**
   * Convert NeTEx Branding entity into OTP model.
   *
   * @param branding NeTEx branding entity
   * @return OTP Branding model
   */
  public Branding mapBranding(org.rutebanken.netex.model.Branding branding) {
    var builder = Branding.of(idFactory.createId(branding.getId()));

    if (branding.getShortName() != null) {
      builder.setShortName(branding.getShortName().getValue());
    }

    if (branding.getName() != null) {
      builder.setName(branding.getName().getValue());
    }

    if (branding.getDescription() != null) {
      builder.setDescription(branding.getDescription().getValue());
    }

    builder.setUrl(branding.getUrl());
    builder.setImage(branding.getImage());

    return builder.build();
  }
}
