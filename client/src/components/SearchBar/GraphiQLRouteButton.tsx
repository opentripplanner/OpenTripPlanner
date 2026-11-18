import { Button } from 'react-bootstrap';
import { TripQueryVariables } from '../../gql/graphql.ts';
import graphqlIcon from '../../static/img/graphql-solid.svg';
import { createPrunedQuery, createPrunedVariables } from '../../util/queryPruning.ts';
import { graphiQLLink } from '../../util/graphiQLLink.ts';

const graphiQLUrl = import.meta.env.VITE_GRAPHIQL_URL;

function GraphiQLRouteButton({ tripQueryVariables }: { tripQueryVariables: TripQueryVariables }) {
  if (!graphiQLUrl) {
    return null;
  }

  const prunedVariables = createPrunedVariables(tripQueryVariables);
  const prunedQuery = createPrunedQuery(tripQueryVariables);

  const hint = 'Open in GraphiQL';

  const fullUrl = graphiQLLink(graphiQLUrl, prunedQuery, prunedVariables);

  return (
    <Button title={hint} href={fullUrl} target={'_blank'}>
      <img alt={hint} title={hint} src={graphqlIcon} width="20" height="20" className="d-inline-block align-middle" />
    </Button>
  );
}

export default GraphiQLRouteButton;
