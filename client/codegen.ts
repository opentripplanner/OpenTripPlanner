import type { CodegenConfig } from '@graphql-codegen/cli';

const config: CodegenConfig = {
  overwrite: true,
  schema: '../application/src/main/resources/org/opentripplanner/apis/transmodel/schema.graphql',
  documents: 'src/**/*.{ts,tsx}',
  generates: {
    'src/gql/': {
      preset: 'client',
      plugins: [],
    },
    'src/gql/types.generated.ts': {
      plugins: ['typescript'],
    },
  },
};

export default config;
