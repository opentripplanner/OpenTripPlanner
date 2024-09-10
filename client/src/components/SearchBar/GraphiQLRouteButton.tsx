import { Button } from 'react-bootstrap';
import { TripQueryVariables } from '../../gql/graphql.ts';
import { queryAsString } from '../../static/query/tripQuery.tsx';
const graphiQLUrl = import.meta.env.VITE_GRAPHIQL_URL;

function GraphiQLRouteButton({ tripQueryVariables }: { tripQueryVariables: TripQueryVariables }) {
  const formattedVariables = encodeURIComponent(JSON.stringify(tripQueryVariables));
  const formattedQuery = encodeURIComponent(queryAsString);

  return (
    <div className="search-bar-route-button-wrapper">
      <Button href={graphiQLUrl + '&query=' + formattedQuery + '&variables=' + formattedVariables} target={'_blank'}>
        GraphiQL
      </Button>
    </div>
  );
}
export default GraphiQLRouteButton;
