import { useContext } from 'react';
import { TripSchemaContext } from './TripSchemaContext';

export function useTripSchema() {
  const context = useContext(TripSchemaContext);
  if (!context) {
    throw new Error('useTripSchema must be used within a TripSchemaProvider');
  }
  return context;
}
