package org.opentripplanner.apis.support.graphql.injectdoc;

import org.opentripplanner.framework.doc.DocumentedEnum;

public enum ApiDocumentationProfile implements DocumentedEnum<ApiDocumentationProfile> {
  DEFAULT,
  ENTUR;

  private static final String TYPE_DOC =
    "List of available custom documentation profiles. " +
    "The default should be used in most cases. A profile may be used to deprecate part of the " +
    "API in case it is not supported.";

  @Override
  public String typeDescription() {
    return TYPE_DOC;
  }

  @Override
  public String enumValueDescription() {
    return switch (this) {
      case DEFAULT -> "Default documentation is used.";
      case ENTUR -> "Entur specific documentation. This deprecate features not supported at Entur," +
      " Norway.";
    };
  }
}
