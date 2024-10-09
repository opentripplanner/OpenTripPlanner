package org.opentripplanner.standalone.config.framework.json;

import static org.opentripplanner.standalone.config.framework.json.ConfigType.ARRAY;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.ENUM;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.ENUM_MAP;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.ENUM_SET;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.MAP;
import static org.opentripplanner.standalone.config.framework.json.NodeInfo.EXPERIMENTAL_FEATURE;

import java.util.EnumSet;

@SuppressWarnings("UnusedReturnValue")
class NodeInfoBuilder {

  private String name;
  private ConfigType type;
  private Class<? extends Enum<?>> enumType;
  private ConfigType elementType;
  private OtpVersion since = OtpVersion.NA;
  private String summary = "TODO: Add short summary.";
  private String description = null;
  private boolean experimentalFeature = false;
  private String defaultValue = null;
  private boolean required = true;
  private boolean skipChildren = false;

  public String name() {
    return name;
  }

  NodeInfoBuilder withName(String name) {
    this.name = name;
    return this;
  }

  NodeInfoBuilder withType(ConfigType type) {
    if (EnumSet.of(ARRAY, MAP, ENUM_MAP, ENUM_SET, ENUM).contains(type)) {
      throw new IllegalArgumentException(
        "Use type specific build methods for this type like 'withArray'. Type : " + type
      );
    }
    this.type = type;
    return this;
  }

  NodeInfoBuilder withSince(OtpVersion since) {
    this.since = since;
    return this;
  }

  /**
   * Allow parent to set the `since` property of children without version.
   */
  OtpVersion since() {
    return since;
  }

  public NodeInfoBuilder withSummary(String summary) {
    this.summary = summary;
    return this;
  }

  NodeInfoBuilder withDescription(String description) {
    this.description = description;
    return this;
  }

  String description() {
    if (!experimentalFeature) {
      return description;
    }
    if (description == null) {
      return EXPERIMENTAL_FEATURE;
    }
    return description + "\n\n" + EXPERIMENTAL_FEATURE;
  }

  NodeInfoBuilder withExperimentalFeature() {
    this.experimentalFeature = true;
    return this;
  }

  NodeInfoBuilder withOptional(String defaultValue) {
    this.defaultValue = defaultValue;
    return withOptional();
  }

  NodeInfoBuilder withOptional() {
    this.required = false;
    return this;
  }

  NodeInfoBuilder withRequired() {
    this.required = true;
    return this;
  }

  NodeInfoBuilder withEnum(Class<? extends Enum<?>> enumType) {
    this.type = ENUM;
    this.enumType = enumType;
    return this;
  }

  NodeInfoBuilder withArray(ConfigType elementType) {
    this.type = ARRAY;
    this.elementType = elementType;
    return this;
  }

  NodeInfoBuilder withMap(ConfigType elementType) {
    this.type = MAP;
    this.elementType = elementType;
    return this;
  }

  NodeInfoBuilder withEnumMap(Class<? extends Enum<?>> enumType, ConfigType elementType) {
    this.type = ENUM_MAP;
    this.enumType = enumType;
    this.elementType = elementType;
    return this;
  }

  NodeInfoBuilder withEnumSet(Class<? extends Enum<?>> enumType) {
    this.type = ENUM_SET;
    this.elementType = ENUM;
    this.enumType = enumType;
    return this;
  }

  NodeInfoBuilder withSkipChild() {
    this.skipChildren = true;
    return this;
  }

  NodeInfo build() {
    return new NodeInfo(
      name,
      summary,
      description(),
      type,
      enumType,
      elementType,
      since,
      defaultValue,
      required,
      skipChildren
    );
  }
}
