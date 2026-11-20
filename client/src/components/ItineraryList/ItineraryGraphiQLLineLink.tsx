import { lineQueryAsString } from '../../static/query/lineQuery.tsx';
import { graphiQLLink } from '../../util/graphiQLLink.ts';
const graphiQLUrl = import.meta.env.VITE_GRAPHIQL_URL;

export function ItineraryGraphiQLLineLink({ legId, legName }: { legId: string; legName: string }) {
  if (!graphiQLUrl) {
    return <>{legName}</>;
  }

  const queryID = { id: legId };

  return (
    <a href={graphiQLLink(graphiQLUrl, lineQueryAsString, queryID)} target={'_blank'} rel={'noreferrer'}>
      {legName}
    </a>
  );
}
