import { Button } from 'react-bootstrap';

export function ItineraryPaginationControl({
  previousPageCursor,
  nextPageCursor,
  onPagination,
  loading,
  comparisonSelectedIndexes,
  onCompare,
}: {
  previousPageCursor?: string | null;
  nextPageCursor?: string | null;
  onPagination: (cursor: string) => void;
  loading: boolean;
  comparisonSelectedIndexes?: number[];
  onCompare?: () => void;
}) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
      <Button
        variant="outline-primary"
        size="sm"
        disabled={!previousPageCursor || loading}
        onClick={() => {
          if (previousPageCursor) {
            onPagination(previousPageCursor);
          }
        }}
      >
        Previous page
      </Button>
      <div style={{ minWidth: '120px', textAlign: 'center' }}>
        {comparisonSelectedIndexes?.length === 2 && onCompare && (
          <Button
            variant="primary"
            size="sm"
            onClick={onCompare}
          >
            Compare Selected
          </Button>
        )}
      </div>
      <Button
        variant="outline-primary"
        size="sm"
        disabled={!nextPageCursor || loading}
        onClick={() => {
          if (nextPageCursor) {
            onPagination(nextPageCursor);
          }
        }}
      >
        Next page
      </Button>
    </div>
  );
}
