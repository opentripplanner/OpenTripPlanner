# GraphQL code generator

Build-time tool that generates Java types and data fetcher interfaces for a GraphQL API from its
schema. It replaces the JavaScript [GraphQL Code Generator](https://the-guild.dev/graphql/codegen)
with its `java` and `java-resolvers` plugins, and reads a subset of the same configuration format.

## Usage

Run `org.opentripplanner.graphql.codegen.GraphQLCodegen <config.yml> <output-source-directory>`,
typically with `exec-maven-plugin` in the `generate-sources` phase, and add the output directory as
a source root (`build-helper-maven-plugin`). Write the output under `target/` so that it is not
committed. A file is only rewritten when its content changes. The generated members follow the
schema order, and the code is not formatted for readability.

See the `application` module's `pom.xml` and `application/src/build/gtfs-graphql-codegen.yml` for
an example (the GTFS GraphQL API).

Note: when building only a consuming module (e.g. `mvn -pl application`), add `-am` or install this
module first, since the generator is a build-time plugin dependency.

## Configuration

```yaml
schema: path/to/schema.graphqls # relative to the config file

generates:
  # One entry per generated file, named <className>.java
  MyTypes.java:
    plugins:
      - java # enums, input classes and <Type><Field>Args classes
    config:
      className: MyTypes
      listType: List
  MyDataFetchers.java:
    plugins:
      - java-resolvers # DataFetcher interfaces for object types
    config:
      className: MyDataFetchers

# Shared config; a per-file config overrides these keys
config:
  package: org.example.generated
  typesPrefix: GraphQL
  scalars:
    Duration: java.time.Duration
  mappers:
    Stop: org.example.model.Stop#Stop
```

- `scalars`: maps a GraphQL scalar to a Java type. Unmapped custom scalars become `Object`.
- `mappers`: maps a GraphQL type to a Java type. Use `fully.qualified.Import#TypeExpression` to
  import a class and use a (possibly generic) type expression, e.g.
  `graphql.relay.Edge#Edge<TransitAlert>`. Unmapped types become `Object`.
- `listType`: the Java type used for GraphQL lists (default `Iterable`).
