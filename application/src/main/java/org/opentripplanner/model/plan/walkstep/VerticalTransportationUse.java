package org.opentripplanner.model.plan.walkstep;

import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;

/**
 * Represents information about vertical transportation equipment stored in
 * {@WalkStep}.
 */
public record VerticalTransportationUse(
  @Nullable Double toLevel,
  @Nullable I18NString name,
  @Nullable Double fromLevel,
  VerticalTransportationType type
) {}
