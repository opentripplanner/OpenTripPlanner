import { authorityQueryAsString } from '../../static/query/authorityQuery.tsx';
import { Maybe } from '../../gql/graphql.ts';
import { graphiQLLink } from '../../util/graphiQLLink.ts';
const graphiQLUrl = import.meta.env.VITE_GRAPHIQL_URL;

export function ItineraryGraphiQLAuthorityLink({
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
    <a href={graphiQLLink(graphiQLUrl, authorityQueryAsString, queryID)} target={'_blank'} rel={'noreferrer'}>
      {legName}
    </a>
  );
}
