const fs = require('fs');
const path = require('path');

/**
 * Plugin to generate GraphQL queries dynamically from schema
 */
const generateQueriesPlugin = async (schema) => {
  const queryType = schema.getQueryType();
  if (!queryType) {
    return '// No Query type found in the schema';
  }

  // Read the content from the input file to replace "replacementContent"
  const inputFilePath = path.join(__dirname, '../static/query/selector.fragment.graphql');
  let replacementContent = '';

  try {
    replacementContent = fs.readFileSync(inputFilePath, 'utf-8').trim();
  } catch (error) {
    console.error(`Failed to read the input file at ${inputFilePath}`, error);
    return '// Error: Failed to read the input file';
  }

  const queryFields = queryType.getFields();
  const queries = [];

  Object.keys(queryFields).forEach((fieldName) => {
    if (fieldName === 'trip') {
      // Only interested in the trip query
      const field = queryFields[fieldName];

      // Filter out deprecated arguments using deprecationReason - isDeprecated does not work
      const validArgs = field.args.filter((arg) => !arg.deprecationReason);

      // Generate the arguments for the query with filtered arguments
      const args = validArgs.map((arg) => `  $${arg.name}: ${arg.type}`).join('\n');

      // Generate the arguments for the query variables with filtered arguments
      const argsForQuery = validArgs.map((arg) => `    ${arg.name}: $${arg.name}`).join('\n');

      const query = `import { graphql } from '../../gql';
import { print } from 'graphql/index';

// Generated trip query based on schema.graphql

export const query = graphql(\`
query ${fieldName}(
${args}
) {
  ${fieldName}(
${argsForQuery}
  ) 
    ${replacementContent}
  }
}\`);

export const queryAsString = print(query);`;
      queries.push(query.trim()); // Trim unnecessary whitespace
    }
  });

  return queries.join('\n\n'); // Separate queries with a blank line
};

module.exports = {
  plugin: generateQueriesPlugin,
};
