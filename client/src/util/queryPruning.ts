import { TripQueryVariables } from '../gql/graphql.ts';
import { queryAsString } from '../static/query/tripQuery.tsx';

function parseVariableDeclarations(queryString: string): Record<string, string> {
  const variableTypes: Record<string, string> = {};

  const queryMatch = queryString.match(/query\s+trip\s*\(([\s\S]*?)\)\s*\{/);
  if (!queryMatch) return variableTypes;

  const variableSection = queryMatch[1];

  const variableMatches = variableSection.match(/\$(\w+):\s*([^,\n\r]+)/g);
  if (!variableMatches) return variableTypes;

  variableMatches.forEach((match) => {
    const parts = match.match(/\$(\w+):\s*(.+)/);
    if (parts) {
      const varName = parts[1].trim();
      variableTypes[varName] = parts[2].trim();
    }
  });

  return variableTypes;
}

function extractQueryBody(queryString: string): string {
  const match = queryString.match(/query\s+trip\s*\([^)]*\)\s*(\{[\s\S]*})/);
  return match ? match[1] : '{ trip { __typename } }';
}

export function createPrunedQuery(variables: TripQueryVariables): string {
  const variableTypes = parseVariableDeclarations(queryAsString);
  const queryBody = extractQueryBody(queryAsString);

  const providedVariables = Object.keys(variables).filter(
    (key) => variables[key as keyof TripQueryVariables] !== undefined,
  );

  const variableDeclarations = providedVariables
    .map((varName) => `  $${varName}: ${variableTypes[varName]}`)
    .filter((declaration) => !declaration.includes('undefined'))
    .join('\n');

  const variableUsages = providedVariables
    .filter((varName) => variableTypes[varName])
    .map((varName) => `    ${varName}: $${varName}`)
    .join('\n');

  const modifiedQueryBody = queryBody.replace(
    /trip\s*\([^)]*\)/,
    `trip(
${variableUsages}
  )`,
  );

  return `query trip(
${variableDeclarations}
) ${modifiedQueryBody}`;
}

export function createPrunedVariables(variables: TripQueryVariables): TripQueryVariables {
  // Create a new object with only the defined variables
  const prunedVariables: Partial<TripQueryVariables> = {};

  Object.keys(variables).forEach((key) => {
    const value = variables[key as keyof TripQueryVariables];
    if (value !== undefined) {
      prunedVariables[key as keyof TripQueryVariables] = value;
    }
  });

  return prunedVariables as TripQueryVariables;
}
