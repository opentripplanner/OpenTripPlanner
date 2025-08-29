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
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div style={{ flex: 1 }}>
          <div style={{ fontWeight: 'bold', marginBottom: '8px' }}>No Results Found</div>
          <div style={{ marginBottom: '12px', color: '#6c757d' }}>
            No matching trips were found for your search criteria.
          </div>
          <div style={{ fontWeight: 'bold', marginBottom: '8px' }}>Raw Response:</div>
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
