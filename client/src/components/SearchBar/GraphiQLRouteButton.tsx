import { Button } from 'react-bootstrap';
import { TripQueryVariables } from '../../gql/graphql.ts';
import graphqlIcon from '../../static/img/graphql-solid.svg';
import { createPrunedQuery, createPrunedVariables } from '../../util/queryPruning.ts';

const graphiQLUrl = import.meta.env.VITE_GRAPHIQL_URL;

function GraphiQLRouteButton({ tripQueryVariables }: { tripQueryVariables: TripQueryVariables }) {
  if (!graphiQLUrl) {
    return null;
  }

  const prunedVariables = createPrunedVariables(tripQueryVariables);
  const formattedVariables = encodeURIComponent(JSON.stringify(prunedVariables));
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
