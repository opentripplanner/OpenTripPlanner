export default {
  introspection: {
    type: 'sdl',
    paths: ['src/ext/resources/legacygraphqlapi/schema.graphqls'],
  },
  website: {
    template: 'carbon-multi-page',
    output: 'target/magidoc/api/graphql-gtfs/v1/',
    options: {
      siteRoot: '/api/dev-2.x/graphql-gtfs/v1',
      appLogo: 'https://docs.opentripplanner.org/en/dev-2.x/images/otp-logo.svg',
      pages: [{
        title: 'Introduction',
        content: `
# OTP GTFS GraphQL API v1 documentation

This is the static documentation of the OTP GraphQL GTFS API v1.

The GraphQL endpoint of your instance, which you should point your tooling to, is 
\`http://localhost:8080/otp/routers/default/index/graphql\`

Please also check out the interactive API explorer built into every instance and available
at http://localhost:8080/graphiql 

![GraphiQL screenshot](https://docs.opentripplanner.org/en/dev-2.x/images/graphiql.png)
`,
      }],
      appTitle: 'OTP GTFS GraphQL API v1',
      queryGenerationFactories: {
        'Polyline': '<>',
        'GeoJson': '<>'
      },
    }
  },
}
