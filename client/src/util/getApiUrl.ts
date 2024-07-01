const endpoint = import.meta.env.VITE_API_URL;

export const getApiUrl = () => {
  try {
    // the URL constructor will throw exception if endpoint is not a valid URL,
    // e.g. if it is a relative path
    new URL(endpoint);
    return endpoint;
  } catch (e) {
    return `${window.location.origin}${endpoint}`;
  }
};
