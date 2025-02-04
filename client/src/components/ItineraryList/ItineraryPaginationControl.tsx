import { Button } from 'react-bootstrap';

export function ItineraryPaginationControl({
  previousPageCursor,
  nextPageCursor,
  onPagination,
  loading,
}: {
  previousPageCursor?: string | null;
  nextPageCursor?: string | null;
  onPagination: (cursor: string) => void;
  loading: boolean;
}) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-evenly' }}>
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
      </Button>{' '}
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
