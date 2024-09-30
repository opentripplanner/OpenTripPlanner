import { Button } from 'react-bootstrap';
import { TripQueryVariables } from '../../gql/graphql.ts';
import { queryAsString } from '../../static/query/tripQuery.tsx';
import graphqlIcon from '../../static/img/graphql-solid.svg';

const graphiQLUrl = import.meta.env.VITE_GRAPHIQL_URL;

function GraphiQLRouteButton({ tripQueryVariables }: { tripQueryVariables: TripQueryVariables }) {
  const formattedVariables = encodeURIComponent(JSON.stringify(tripQueryVariables));
  const formattedQuery = encodeURIComponent(queryAsString);

  const hint = 'Open in GraphiQL';

  return (
    <Button
      title={hint}
      href={graphiQLUrl + '&query=' + formattedQuery + '&variables=' + formattedVariables}
      target={'_blank'}
    >
      <img alt={hint} title={hint} src={graphqlIcon} width="20" height="20" className="d-inline-block align-middle" />
    </Button>
  );
}

export default GraphiQLRouteButton;
