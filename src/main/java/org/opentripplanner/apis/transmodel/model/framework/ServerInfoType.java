package org.opentripplanner.apis.transmodel.model.framework;

import static org.opentripplanner.framework.application.OtpFileNames.BUILD_CONFIG_FILENAME;
import static org.opentripplanner.framework.application.OtpFileNames.OTP_CONFIG_FILENAME;
import static org.opentripplanner.framework.application.OtpFileNames.ROUTER_CONFIG_FILENAME;
import static org.opentripplanner.model.projectinfo.OtpProjectInfo.projectInfo;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;

public class ServerInfoType {

  public static GraphQLOutputType create() {
    return GraphQLObjectType
      .newObject()
      .name("ServerInfo")
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("version")
          .description("Maven version")
          .type(Scalars.GraphQLString)
          .dataFetcher(e -> projectInfo().version)
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("buildTime")
          .description("OTP Build timestamp")
          .type(Scalars.GraphQLString)
          .dataFetcher(e -> projectInfo().versionControl.buildTime)
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("gitBranch")
          .description("")
          .type(Scalars.GraphQLString)
          .dataFetcher(e -> projectInfo().versionControl.branch)
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("gitCommit")
          .description("")
          .type(Scalars.GraphQLString)
          .dataFetcher(e -> projectInfo().versionControl.commit)
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("gitCommitTime")
          .description("")
          .type(Scalars.GraphQLString)
          .dataFetcher(e -> projectInfo().versionControl.commitTime)
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("otpConfigVersion")
          .description("The 'configVersion' of the " + OTP_CONFIG_FILENAME + " file.")
          .type(Scalars.GraphQLString)
          .dataFetcher(e -> projectInfo().otpConfigVersion)
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("buildConfigVersion")
          .description("The 'configVersion' of the " + BUILD_CONFIG_FILENAME + " file.")
          .type(Scalars.GraphQLString)
          .dataFetcher(e -> projectInfo().buildConfigVersion)
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("routerConfigVersion")
          .description("The 'configVersion' of the " + ROUTER_CONFIG_FILENAME + " file.")
          .type(Scalars.GraphQLString)
          .dataFetcher(e -> projectInfo().routerConfigVersion)
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("otpSerializationVersionId")
          .description(
            "The otp-serialization-version-id used to check graphs for compatibility with current version of OTP."
          )
          .type(Scalars.GraphQLString)
          .dataFetcher(e -> projectInfo().getOtpSerializationVersionId())
          .build()
      )
      .build();
  }
}
