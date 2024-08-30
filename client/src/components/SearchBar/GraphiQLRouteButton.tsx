import { Button } from 'react-bootstrap';
import { TripQueryVariables } from '../../gql/graphql.ts';
import { queryAsString } from '../../static/query/tripQuery.tsx';

function GraphiQLRouteButton({ tripQueryVariables }: { tripQueryVariables: TripQueryVariables }) {
  const formattedVariables = encodeURIComponent(JSON.stringify(tripQueryVariables));
  const formattedQuery = encodeURIComponent(queryAsString);

  return (
    <div className="search-bar-route-button-wrapper">
      <Button
        href={
          'https://otp2debug.dev.entur.org/graphiql?flavor=transmodel&query=' +
          formattedQuery +
          '&variables=' +
          formattedVariables
        }
        target={'_blank'}
      >
        GraphQL
      </Button>
    </div>
  );
}
export default GraphiQLRouteButton;
