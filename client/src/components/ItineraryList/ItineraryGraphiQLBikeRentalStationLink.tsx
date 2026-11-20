import { bikeRentalStationQueryAsString } from '../../static/query/bikeRentalStationQuery.tsx';
import { Maybe } from '../../gql/graphql.ts';
import { graphiQLLink } from '../../util/graphiQLLink.ts';
const graphiQLUrl = import.meta.env.VITE_GRAPHIQL_URL;

export function ItineraryGraphiQLBikeRentalStationLink({
  stationId,
  legName,
}: {
  stationId: string | undefined;
  legName: Maybe<string> | undefined;
}) {
  if (!graphiQLUrl) {
    return <>{legName}</>;
  }

  const queryID = { id: stationId };

  return (
    <a href={graphiQLLink(graphiQLUrl, bikeRentalStationQueryAsString, queryID)} target={'_blank'} rel={'noreferrer'}>
      {legName}
    </a>
  );
}
