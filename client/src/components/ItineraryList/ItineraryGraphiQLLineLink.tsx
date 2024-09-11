import { lineQueryAsString } from '../../static/query/lineQuery.tsx';
const graphiQLUrl = import.meta.env.VITE_GRAPHIQL_URL;

export function ItineraryGraphiQLLineLink({ legId, legName }: { legId: string; legName: string }) {
  const queryID = { id: legId };
  const formattedQuery = encodeURIComponent(lineQueryAsString);
  const formattedQueryID = encodeURIComponent(JSON.stringify(queryID));

  return (
    <a
      href={graphiQLUrl + '&query=' + formattedQuery + '&variables=' + formattedQueryID}
      target={'_blank'}
      rel={'noreferrer'}
    >
      {legName}
    </a>
  );
}
