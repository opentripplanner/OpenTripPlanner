import { it } from 'vitest';
import { render } from '@testing-library/react';
import { SearchBar } from './SearchBar.tsx';
import { TripQueryVariables } from '../../gql/graphql.ts';

const variables: TripQueryVariables = {
  from: { coordinates: { longitude: 9.795206, latitude: 60.13776 } },
  to: { coordinates: { longitude: 11.50907, latitude: 59.85208 } },
  dateTime: new Date().toISOString(),
};

it('renders without crashing', () => {
  render(<SearchBar loading onRoute={() => {}} setTripQueryVariables={() => {}} tripQueryVariables={variables} />);
});
