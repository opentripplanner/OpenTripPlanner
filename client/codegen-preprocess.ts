import type { CodegenConfig } from '@graphql-codegen/cli';

import * as path from 'node:path';

const config: CodegenConfig = {
  overwrite: true,
  schema: '../application/src/main/resources/org/opentripplanner/apis/transmodel/schema.graphql',
  documents: 'src/**/*.{ts,tsx}',
  generates: {
    'src/static/query/tripQuery.tsx': {
      plugins: [path.resolve(__dirname, './src/util/generate-queries.cjs')],
    },
  },
};

export default config;
