package org.opentripplanner.ext.transmodelapi.model.framework;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import org.opentripplanner.common.MavenVersion;

public class ServerInfoType {
  public static GraphQLOutputType create() {
    return GraphQLObjectType
        .newObject()
        .name("ServerInfo")
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("version")
            .description("Maven version")
            .type(Scalars.GraphQLString)
            .dataFetcher(e -> MavenVersion.VERSION.version)
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("buildTime")
            .description("OTP Build timestamp")
            .type(Scalars.GraphQLString)
            .dataFetcher(e -> MavenVersion.VERSION.buildTime)
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("gitBranch")
            .description("")
            .type(Scalars.GraphQLString)
            .dataFetcher(e -> MavenVersion.VERSION.branch)
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("gitCommit")
            .description("")
            .type(Scalars.GraphQLString)
            .dataFetcher(e -> MavenVersion.VERSION.commit)
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("gitCommitTime")
            .description("")
            .type(Scalars.GraphQLString)
            .dataFetcher(e -> MavenVersion.VERSION.commitTime)
            .build())
        .build();
  }
}
