import React, { createContext, useEffect, useState } from 'react';
import type { TripArgsRepresentation } from './useTripArgs';
import { fetchTripArgs } from './useTripArgs';

interface TripSchemaContextValue {
  tripArgs: TripArgsRepresentation | null;
  loading: boolean;
  error: string | null;
}

export const TripSchemaContext = createContext<TripSchemaContextValue | undefined>(undefined);

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
