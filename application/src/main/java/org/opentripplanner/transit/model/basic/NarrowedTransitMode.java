package org.opentripplanner.transit.model.basic;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.service.ReplacementHelper;

public class NarrowedTransitMode {

  TransitMode mode;

  @Nullable
  List<SubMode> subModes;

  @Nullable
  Boolean replacement;

  @Nullable
  List<Integer> allowedExtendedTypes;

  @Nullable
  List<Integer> forbiddenExtendedTypes;

  private static final List<NarrowedTransitMode> ALL = Stream.of(TransitMode.values())
    .map(mode -> new NarrowedTransitMode(mode, Collections.emptyList(), null, null, null))
    .toList();

  public NarrowedTransitMode(
    TransitMode mode,
    @Nullable List<SubMode> subModes,
    @Nullable Boolean replacement,
    @Nullable List<Integer> allowedExtendedType,
    @Nullable List<Integer> forbiddenExtendedType
  ) {
    this.mode = mode;
    this.subModes = subModes;
    this.replacement = replacement;
    this.allowedExtendedTypes = allowedExtendedType;
    this.forbiddenExtendedTypes = forbiddenExtendedType;
  }

  public static NarrowedTransitMode of(MainAndSubMode mode) {
    return new NarrowedTransitMode(mode.mainMode(), submodesOfNullable(mode.subMode()), null, null, null);
  }

  private static List<SubMode> submodesOfNullable(@Nullable SubMode subMode) {
    return subMode != null ? Collections.singletonList(subMode) : Collections.emptyList();
  }

  public static List<NarrowedTransitMode> all() {
    return ALL;
  }

  public boolean isMainModeOnly() {
    return (
      (this.subModes == null || this.subModes.isEmpty()) &&
      this.replacement == null &&
      this.allowedExtendedTypes == null &&
      this.forbiddenExtendedTypes == null
    );
  }

  public MainAndSubMode toMainAndSubMode() {
    if (
      (this.subModes != null && this.subModes.size() > 1) ||
      this.replacement != null ||
      this.allowedExtendedTypes != null ||
      this.forbiddenExtendedTypes != null
    ) {
      throw new IllegalArgumentException("Not convertible to MainAndSubMode");
    }
    return new MainAndSubMode(
      this.mode,
      this.subModes != null && !this.subModes.isEmpty() ? this.subModes.getFirst() : null
    );
  }

  public TransitMode getMode() {
    return mode;
  }

  @Nullable
  public List<SubMode> getSubModes() {
    return subModes;
  }

  @Nullable
  public Boolean isReplacement() {
    return replacement;
  }

  @Nullable
  public List<Integer> getAllowedExtendedTypes() {
    return allowedExtendedTypes;
  }

  @Nullable
  public List<Integer> getForbiddenExtendedTypes() {
    return forbiddenExtendedTypes;
  }
}
