interface ErrorDisplayProps {
  error: unknown;
  onDismiss?: () => void;
}

export function ErrorDisplay({ error, onDismiss }: ErrorDisplayProps) {
  if (!error) return null;

  const getErrorInfo = (error: unknown) => {
    try {
      const errorObj = error as Record<string, unknown>;
      const errorMessage = String(errorObj.message || '').toLowerCase();
      const errorCode = errorObj.code as string;

      if (errorMessage.includes('fetch') && errorMessage.includes('failed')) {
        return {
          userMessage: 'Connection failed. Please check your internet connection and try again.',
          showRaw: false,
        };
      }

      if (errorMessage.includes('network error') || errorCode === 'NETWORK_ERROR') {
        return {
          userMessage: 'Network error occurred. Please verify your internet connection and try again.',
          showRaw: false,
        };
      }

      if (errorMessage.includes('enotfound') || errorMessage.includes('getaddrinfo')) {
        return {
          userMessage: 'Unable to reach the server. Please check your internet connection or try again later.',
          showRaw: false,
        };
      }

      if (errorMessage.includes('timeout')) {
        return {
          userMessage: 'Request timed out. The server may be busy, please try again in a moment.',
          showRaw: true,
        };
      }

      const errorResponse = errorObj.response as Record<string, unknown>;
      if (errorResponse && typeof errorResponse.status === 'number') {
        const status = errorResponse.status as number;
        if (status >= 500) {
          return {
            userMessage: 'Server error occurred. Please try again later.',
            showRaw: true,
          };
        } else if (status === 404) {
          return {
            userMessage: 'Service not found. Please contact support if this issue persists.',
            showRaw: true,
          };
        } else if (status >= 400) {
          return {
            userMessage: 'Request failed. Please check your search parameters and try again.',
            showRaw: true,
          };
        }
      }

      if (errorResponse && errorResponse.errors) {
        return {
          userMessage: 'Server returned an error response.',
          showRaw: true,
        };
      }

      // Default case
      return {
        userMessage: 'An unexpected error occurred. Please try again.',
        showRaw: true,
      };
    } catch (_) {
      return {
        userMessage: 'An unexpected error occurred. Please try again.',
        showRaw: true,
      };
    }
  };

  const { userMessage, showRaw } = getErrorInfo(error);

  let rawContent;
  try {
    const errorObj = error as Record<string, unknown>;
    const errorResponse = errorObj.response as Record<string, unknown>;

    if (errorResponse && errorResponse.errors) {
      rawContent = JSON.stringify(errorResponse.errors, null, 2);
    } else if (errorResponse && errorResponse.data) {
      rawContent = JSON.stringify(errorResponse.data, null, 2);
    } else {
      rawContent = JSON.stringify(error, null, 2);
    }
  } catch (_) {
    rawContent = 'Unable to display error details';
  }

  return (
    <div
      style={{
        backgroundColor: '#f8d7da',
        color: '#721c24',
        border: '1px solid #f5c6cb',
        borderRadius: '4px',
        padding: '12px',
        margin: '8px 0',
        fontSize: '14px',
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div style={{ flex: 1 }}>
          <div style={{ fontWeight: 'bold', marginBottom: '8px' }}>Search Error</div>
          <div style={{ marginBottom: '12px', color: '#721c24', lineHeight: '1.4' }}>{userMessage}</div>
          {showRaw && (
            <>
              <div style={{ fontWeight: 'bold', marginBottom: '8px' }}>Technical Details:</div>
              <pre
                style={{
                  fontSize: '12px',
                  margin: 0,
                  whiteSpace: 'pre-wrap',
                  wordBreak: 'break-word',
                  width: '100%',
                }}
              >
                {rawContent}
              </pre>
            </>
          )}
        </div>
        {onDismiss && (
          <button
            onClick={onDismiss}
            style={{
              background: 'none',
              border: 'none',
              fontSize: '18px',
              cursor: 'pointer',
              color: '#721c24',
              marginLeft: '8px',
            }}
            aria-label="Dismiss error"
          >
            ×
          </button>
        )}
      </div>
    </div>
  );
}
