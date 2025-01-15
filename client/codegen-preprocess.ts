import type { CodegenConfig } from '@graphql-codegen/cli';

import * as path from 'node:path';



const config: CodegenConfig = {
  overwrite: true,
  schema: 'https://otp2debug.dev.entur.org/otp/transmodel/v3/schema.graphql',
  documents: 'src/**/*.{ts,tsx}',
  generates: {
    'src/static/query/tripQuery.tsx': {
      plugins: [path.resolve(__dirname, './src/util/generate-queries.cjs')],
    },
    'src/gql/query-arguments.json': {
      plugins: [path.resolve(__dirname, './src/util/generate-arguments.cjs')],
      config: {
        excludeDeprecated: true, // Ensure this is set to true
      },
    },
  },
};

export default config;
