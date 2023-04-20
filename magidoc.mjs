export default {
  introspection: {
    type: 'sdl',
    paths: ['src/ext/resources/legacygraphqlapi/schema.graphqls'],
  },
  website: {
    template: 'carbon-multi-page',
    output: 'target/mkdocs/api/graphql-gtfs/v1/',
    options: {
      siteRoot: '/api/graphql-gtfs/v1',
      pages: [{
        title: 'Introduction',
        content: `
# Static documentation

This is the static documentation of the OTP GraphQL GTFS API v1.

Please also check out the interactive API explorer built into every instance and available
at http://localhost:8080/graphiql 
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
