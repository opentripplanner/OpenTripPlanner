export default {
  introspection: {
    type: 'sdl',
    paths: ['src/ext/resources/legacygraphqlapi/schema.graphqls'],
  },
  website: {
    template: 'carbon-multi-page',
    output: 'target/magidoc/api/graphql-gtfs/',
    options: {
      siteRoot: '/api/dev-2.x/graphql-gtfs',
      appLogo: 'https://docs.opentripplanner.org/en/dev-2.x/images/otp-logo.svg',
      pages: [{
        title: 'Introduction',
        content: `
# OTP GTFS GraphQL API documentation

This is the static documentation of the OTP GraphQL GTFS API.

The GraphQL endpoint of your instance, which you should point your tooling to, is 
\`http://localhost:8080/otp/routers/default/index/graphql\`

Please also check out the interactive API explorer built into every instance and available
at http://localhost:8080/graphiql 

![GraphiQL screenshot](https://docs.opentripplanner.org/en/dev-2.x/images/graphiql.png)

## Activation

This API used to be off by default but since June '23 that has changed.

To learn how to deactivate it, read the
[documentation](https://docs.opentripplanner.org/en/dev-2.x/sandbox/GtfsGraphQlApi/).
`,
      }],
      appTitle: 'OTP GTFS GraphQL API',
      queryGenerationFactories: {
        'Polyline': '<>',
        'GeoJson': '<>'
      },
    }
  },
}
