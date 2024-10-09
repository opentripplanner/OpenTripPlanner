package org.opentripplanner.netex.mapping;

import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.transit.model.organization.Branding;

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
      builder.withShortName(branding.getShortName().getValue());
    }

    if (branding.getName() != null) {
      builder.withName(branding.getName().getValue());
    }

    if (branding.getDescription() != null) {
      builder.withDescription(branding.getDescription().getValue());
    }

    builder.withUrl(branding.getUrl());
    builder.withImage(branding.getImage());

    return builder.build();
  }
}
