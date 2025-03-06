package org.opentripplanner.apis.support.graphql.injectdoc;

import org.opentripplanner.framework.doc.DocumentedEnum;

public enum ApiDocumentationProfile implements DocumentedEnum<ApiDocumentationProfile> {
  DEFAULT,
  ENTUR;

  private static final String TYPE_DOC =
    """
    List of available custom documentation profiles. A profile is used to inject custom
    documentation like type and field description or a deprecated reason.

    Currently, ONLY the Transmodel API supports this feature.
    """;

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
