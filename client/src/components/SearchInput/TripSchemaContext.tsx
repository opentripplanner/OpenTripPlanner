import { createContext } from 'react';
import type { TripArgsRepresentation } from './useTripArgs';

export interface TripSchemaContextValue {
  tripArgs: TripArgsRepresentation | null;
  loading: boolean;
  error: string | null;
}

export const TripSchemaContext = createContext<TripSchemaContextValue | undefined>(undefined);
