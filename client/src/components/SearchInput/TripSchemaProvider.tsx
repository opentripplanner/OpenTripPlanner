import React, { useEffect, useState } from 'react';
import { TripSchemaContext, TripSchemaContextValue } from './TripSchemaContext';
import { fetchTripArgs, TripArgsRepresentation } from './useTripArgs';

interface TripSchemaProviderProps {
  endpoint: string;
  children: React.ReactNode;
}

export function TripSchemaProvider({ endpoint, children }: TripSchemaProviderProps) {
  const [tripArgs, setTripArgs] = useState<TripArgsRepresentation | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;

    async function loadSchema() {
      setLoading(true);
      setError(null);
      try {
        const result = await fetchTripArgs(endpoint);
        if (isMounted) {
          setTripArgs(result);
        }
      } catch (err) {
        console.error('Error loading trip arguments:', err);
        if (isMounted) {
          setError('Failed to load trip schema');
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }

    loadSchema();
    return () => {
      isMounted = false;
    };
  }, [endpoint]);

  const value: TripSchemaContextValue = { tripArgs, loading, error };

  return <TripSchemaContext.Provider value={value}>{children}</TripSchemaContext.Provider>;
}
