import { TripQuery } from '../../gql/graphql.ts';

interface NoResultsDisplayProps {
  hasSearched: boolean;
  tripQueryResult?: TripQuery | null;
}

export function NoResultsDisplay({ hasSearched, tripQueryResult }: NoResultsDisplayProps) {
  if (!hasSearched || !tripQueryResult) return null;

  return (
    <div
      style={{
        backgroundColor: '#fff3cd',
        color: '#856404',
        border: '1px solid #ffeaa7',
        borderRadius: '4px',
        padding: '12px',
        margin: '8px 0',
        fontSize: '14px',
      }}
    >
      <div className="flex-space-between">
        <div className="flex-1">
          <div className="info-title">No Results Found</div>
          <div className="info-message">
            No matching trips were found for your search criteria.
          </div>
          <div className="info-title">Raw Response:</div>
          <pre
            style={{
              fontSize: '12px',
              margin: 0,
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-word',
              width: '100%',
            }}
          >
            {JSON.stringify(tripQueryResult, null, 2)}
          </pre>
        </div>
      </div>
    </div>
  );
}
