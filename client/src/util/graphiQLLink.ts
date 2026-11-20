/**
 * The link to GraphiQL that opens it with the given query and variables. The URL may already have
 * query parameters, such as the API to use, which is the case for the default one.
 */
export function graphiQLLink(graphiQLUrl: string, query: string, variables: unknown): string {
  const separator = graphiQLUrl.includes('?') ? '&' : '?';
  return `${graphiQLUrl}${separator}query=${encodeURIComponent(query)}&variables=${encodeURIComponent(JSON.stringify(variables))}`;
}
