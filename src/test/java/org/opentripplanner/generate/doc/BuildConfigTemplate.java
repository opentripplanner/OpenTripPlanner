package org.opentripplanner.generate.doc;

public class BuildConfigTemplate {

  static final String TITLE = "Build configuration";

  static final String INTRODUCTION =
    """
      Nnn nn nnn nnnnnnnnn nnnnn nnn Nnnnn nnnnd nnnnrt nnnnnn nn nnnnnnn nn. Nnn nnnn nnnnnn nn
      nnnnten nnnn nnnn nnnnnnnny. Nn nnn nnnnnnnny nnnnt, nnn nnnnnnnn nnnns nnn nnnnnn. nn nn nnnn
      nnn nnnnn, nn nn nnnnnnn.
      """;

  static final String OVERVIEW =
    """
      This table lists all the JSON properties that can be defined in a `build-config.json` file.
      These will be stored in the graph itself, and affect any server that subsequently loads that 
      graph. Sections follow that describe particular settings in more depth.
      """;

  static final String PARAMETERS =
    """
      Nnn nn nn nnnnnnn nnnnn nnn nnnnn, nnn nnnnn nnnnn nnnnnn. Nnn nnnnnn nn nnn nnnnnnnnnnnnnnn
      nnn n nnn nnnnnn.
      """;

  static final String PARAMETERS_DEPRECATED =
    """
      The list of old parameters below are parameters that exist in earlier versions of OTP v2.x.
      The since `2.1 (1.5)` mean that this parameter existed in all versions from 1.5 an up until
      version 2.1 - where the parameter was removed. The Description should give you a hint on how
      to migrate to the new version.
      """;

  static final String PARAMETER_DETAILS =
    """
      This section explain some of the parameters in more detail. Not all parameters can be
      explained with one sentence, so we document them here. The summary section above list ALL
      parameters, this section only list those with a detail description.
      """;
}
