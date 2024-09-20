import { useEffect, useState } from 'react';

type ServerInfo = {
  transitTimeZone: string;
};

const fetchServerInfo = (): Promise<ServerInfo> => fetch('http://localhost:8080/otp/').then((r) => r.json());

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
