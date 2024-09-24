import { useEffect, useState } from 'react';

const serverInfoUrl = import.meta.env.VITE_SERVER_INFO_URL;

type ServerInfo = {
  transitTimeZone: string;
};

const fetchServerInfo = (): Promise<ServerInfo> => fetch(serverInfoUrl).then((r) => r.json());

/**
 * Fetch the transit model's time zone from the server and use the browser's as the fallback.
 */
export const useTimeZone = () => {
  const [data, setData] = useState<ServerInfo | null>(null);
  useEffect(() => {
    const fetchData = async () => {
      setData(await fetchServerInfo());
    };
    fetchData();
  }, []);

  return data?.transitTimeZone || Intl.DateTimeFormat().resolvedOptions().timeZone;
};
