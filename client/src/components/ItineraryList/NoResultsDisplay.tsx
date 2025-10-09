import { TripQuery } from '../../gql/graphql.ts';

interface NoResultsDisplayProps {
  hasSearched: boolean;
  tripQueryResult?: TripQuery | null;
}

export function NoResultsDisplay({ hasSearched, tripQueryResult }: NoResultsDisplayProps) {
  if (!hasSearched || !tripQueryResult) return null;

  return (
    <div className="no-results-display-container">
      <div className="flex-space-between">
        <div className="flex-1">
          <div className="info-title">No Results Found</div>
          <div className="info-message">No matching trips were found for your search criteria.</div>
          <div className="info-title">Raw Response:</div>
          <pre className="no-results-details">{JSON.stringify(tripQueryResult, null, 2)}</pre>
        </div>
      </div>
    </div>
  );
}
