import { Button } from 'react-bootstrap';

export function ItineraryPaginationControl({
  previousPageCursor,
  nextPageCursor,
  onPagination,
}: {
  previousPageCursor?: string | null;
  nextPageCursor?: string | null;
  onPagination: (cursor: string) => void;
}) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-evenly', margin: '1rem 0 ' }}>
      <Button
        disabled={!previousPageCursor}
        onClick={() => {
          previousPageCursor && onPagination(previousPageCursor);
        }}
      >
        Previous page
      </Button>{' '}
      <Button
        disabled={!nextPageCursor}
        onClick={() => {
          nextPageCursor && onPagination(nextPageCursor);
        }}
      >
        Next page
      </Button>
    </div>
  );
}
