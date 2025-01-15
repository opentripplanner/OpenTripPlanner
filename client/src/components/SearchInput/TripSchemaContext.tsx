import React, { createContext, useContext, useEffect, useState } from 'react';
import { fetchTripArgs } from './useTripArgs';
// 'fetchTripArgs' is the function that does the introspection and returns the in-memory representation

// This is the shape of the data we'll store in context:
import type { TripArgsRepresentation } from './useTripArgs';

// 1. Create the context
interface TripSchemaContextValue {
    tripArgs: TripArgsRepresentation | null;
    loading: boolean;
    error: string | null;
}

// Make sure to allow null or partial if you plan to do lazy initialization
const TripSchemaContext = createContext<TripSchemaContextValue | undefined>(undefined);

// 2. Create a Provider component
interface TripSchemaProviderProps {
    endpoint: string;     // the GraphQL URL for introspection
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
            } catch (err: any) {
                console.error('Error loading trip arguments:', err);
                if (isMounted) {
                    setError(err.message ?? 'Failed to load trip schema');
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

    const value: TripSchemaContextValue = {
        tripArgs,
        loading,
        error,
    };

    return (
        <TripSchemaContext.Provider value={value}>
            {children}
        </TripSchemaContext.Provider>
    );
}

// 3. Create a custom hook to consume the context
export function useTripSchema() {
    const context = useContext(TripSchemaContext);
    if (!context) {
        throw new Error('useTripSchema must be used within a TripSchemaProvider');
    }
    return context;
}
