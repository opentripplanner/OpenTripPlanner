package org.opentripplanner.transit.model.framework;

import static org.opentripplanner.utils.lang.StringUtils.assertHasValue;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.utils.lang.StringUtils;

public final class FeedScopedId implements Serializable, Comparable<FeedScopedId> {

  /**
   * One Bus Away uses the underscore as a scope separator between Agency and ID. In OTP we use feed
   * IDs instead of agency IDs as scope, and they are separated with a colon when represented
   * together in String form.
   */
  private static final char ID_SEPARATOR = ':';

  private final String feedId;

  private final String id;

  public FeedScopedId(String feedId, String id) {
    this.feedId = assertHasValue(feedId, "Missing mandatory feedId on FeedScopeId");
    this.id = assertHasValue(id, "Missing mandatory id on FeedScopeId");
  }

  /**
   * Return a new FeedId if the id is not {@code null}, an empty string or contains whitespace
   * only.
   */
  @Nullable
  public static FeedScopedId ofNullable(String feedId, @Nullable String id) {
    return id == null || id.isBlank() ? null : new FeedScopedId(feedId, id);
  }

  /**
   * Given an id of the form "feedId:entityId", parses into a {@link FeedScopedId} id object.
   *
   * @param value id of the form "feedId:entityId"
   * @return an id object
   * @throws IllegalArgumentException if the id cannot be parsed
   */
  public static FeedScopedId parse(String value) throws IllegalArgumentException {
    if (value == null || value.isEmpty()) {
      return null;
    }
    int index = value.indexOf(ID_SEPARATOR);
    if (index == -1) {
      throw new IllegalArgumentException("invalid feed-scoped-id: " + value);
    } else {
      return new FeedScopedId(value.substring(0, index), value.substring(index + 1));
    }
  }

  /**
   * Parses a string consisting of concatenated FeedScopedIds to a List
   */
  public static List<FeedScopedId> parseList(String s) {
    if (StringUtils.containsInvisibleCharacters(s)) {
      throw new IllegalArgumentException(
        "The input string '%s' contains invisible characters which is not allowed.".formatted(s)
      );
    }
    return Arrays.stream(s.split(","))
      .map(String::strip)
      .filter(i -> !i.isBlank())
      .map(FeedScopedId::parse)
      .toList();
  }

  public static boolean isValidString(String value) throws IllegalArgumentException {
    return value != null && value.indexOf(ID_SEPARATOR) > -1;
  }

  /**
   * Concatenate feedId and id into a string.
   */
  private static String concatenateId(String feedId, String id) {
    return feedId + ID_SEPARATOR + id;
  }

  public String getFeedId() {
    return feedId;
  }

  public String getId() {
    return id;
  }

  /**
   * @deprecated Do not depend on the sort order of the ids.
   */
  @Deprecated
  @Override
  public int compareTo(FeedScopedId o) {
    int c = this.feedId.compareTo(o.feedId);
    if (c == 0) {
      c = this.id.compareTo(o.id);
    }
    return c;
  }

  @Override
  public int hashCode() {
    return feedId.hashCode() ^ id.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof FeedScopedId other)) {
      return false;
    }

    return feedId.equals(other.feedId) && id.equals(other.id);
  }

  /**
   * DO NOT CHANGE THIS - It is used in Serialization of the id.
   */
  @Override
  public String toString() {
    return concatenateId(feedId, id);
  }
}
