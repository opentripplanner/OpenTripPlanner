package org.opentripplanner.standalone.config.framework;

public class BuildConfigTemplate {

  public String title() {
    return "Build configuration";
  }

  public String introduction() {
    return """
      Nnn nn nnn nnnnnnnnn nnnnn nnn Nnnnn nnnnd nnnnrt nnnnnn nn nnnnnnn nn. Nnn nnnn nnnnnn nn
      nnnnten nnnn nnnn nnnnnnnny. Nn nnn nnnnnnnny nnnnt, nnn nnnnnnnn nnnns nnn nnnnnn. nn nn nnnn
      nnn nnnnn, nn nn nnnnnnn.
      """;
  }

  public String overview() {
    return """
      Nnn nn nnn nnnnnnnnn nnnnn nnn nnnnn nnnnn nnnnnn nnnnnn nn nnnnnn nn.
      """;
  }

  public String parameters() {
    return """
      Nnn nn nn nnnnnnn nnnnn nnn nnnnn, nnn nnnnn nnnnn nnnnnn. Nnn nnnnnn nn nnn nnnnnnnnnnnnnnn
      nnn n nnn nnnnnn.
      """;
  }

  public String parametersDeprecated() {
    return """
      The list of old parameters below are parameters that exist in earlier versions of OTP v2.x.
      The since `2.1 (1.5)` mean that this parameter existed in all versions from 1.5 an up until
      version 2.1 - where the parameter was removed. The Description should give you a hint on how
      to migrate to the new version.
      """;
  }

  public String parameterDetails() {
    return """
      This section explain some of the parameters in more detail. Not all parameters can be
      explained with one sentence, so we document them here. The summary section above list ALL
      parameters, this section only list those with a detail description.
      """;
  }
}
