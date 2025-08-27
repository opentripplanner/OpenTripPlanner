import { Button } from 'react-bootstrap';
import { TripQueryVariables } from '../../gql/graphql.ts';
import { queryAsString } from '../../static/query/tripQuery.tsx';
import graphqlIcon from '../../static/img/graphql-solid.svg';

const graphiQLUrl = import.meta.env.VITE_GRAPHIQL_URL;

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

function createPrunedQuery(variables: TripQueryVariables): string {
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

function GraphiQLRouteButton({ tripQueryVariables }: { tripQueryVariables: TripQueryVariables }) {
  const formattedVariables = encodeURIComponent(JSON.stringify(tripQueryVariables));
  const prunedQuery = createPrunedQuery(tripQueryVariables);
  const formattedQuery = encodeURIComponent(prunedQuery);

  const hint = 'Open in GraphiQL';

  const separator = graphiQLUrl.includes('?') ? '&' : '?';
  const fullUrl = `${graphiQLUrl}${separator}query=${formattedQuery}&variables=${formattedVariables}`;

  return (
    <Button title={hint} href={fullUrl} target={'_blank'}>
      <img alt={hint} title={hint} src={graphqlIcon} width="20" height="20" className="d-inline-block align-middle" />
    </Button>
  );
}

export default GraphiQLRouteButton;
