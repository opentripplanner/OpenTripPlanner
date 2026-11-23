import { quayQueryAsString } from '../../static/query/quayQuery.tsx';
import { Maybe } from '../../gql/graphql.ts';
import { graphiQLLink } from '../../util/graphiQLLink.ts';
const graphiQLUrl = import.meta.env.VITE_GRAPHIQL_URL;

export function ItineraryGraphiQLQuayLink({
  legId,
  legName,
}: {
  legId: string | undefined;
  legName: Maybe<string> | undefined;
}) {
  if (!graphiQLUrl) {
    return <>{legName}</>;
  }

  const queryID = { id: legId };

  return (
    <a href={graphiQLLink(graphiQLUrl, quayQueryAsString, queryID)} target={'_blank'} rel={'noreferrer'}>
      {legName}
    </a>
  );
}
