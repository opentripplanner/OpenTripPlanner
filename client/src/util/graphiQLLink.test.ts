import { describe, expect, it } from 'vitest';
import { graphiQLLink } from './graphiQLLink.ts';

describe('graphiQLLink', () => {
  const query = '{ line(id: $id) { name } }';
  const variables = { id: 'NSB:Line:1' };
  const encoded = `query=${encodeURIComponent(query)}&variables=${encodeURIComponent(JSON.stringify(variables))}`;

  it('appends to a URL that already has query parameters', () => {
    expect(graphiQLLink('/graphiql?flavor=transmodel', query, variables)).toBe(
      `/graphiql?flavor=transmodel&${encoded}`,
    );
  });

  it('starts the query string when the URL has none', () => {
    expect(graphiQLLink('/graphiql', query, variables)).toBe(`/graphiql?${encoded}`);
  });

  it('works for a full URL', () => {
    expect(graphiQLLink('https://example.com/gw/graphiql', query, variables)).toBe(
      `https://example.com/gw/graphiql?${encoded}`,
    );
  });
});
