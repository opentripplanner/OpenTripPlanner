import { useEffect, useLayoutEffect, useRef, useState } from 'react';

export function useContainerWidth() {
  const [containerWidth, setContainerWidth] = useState(0);
  const containerRef = useRef<HTMLElement>(null);

  useLayoutEffect(() => {
    if (containerRef.current) {
      setContainerWidth(containerRef.current.getBoundingClientRect().width);
    }
  }, []);

  useEffect(() => {
    const listener = () => {
      if (containerRef.current) {
        setContainerWidth(containerRef.current.getBoundingClientRect().width);
      }
    };

    window.addEventListener('resize', listener);

    return () => {
      window.removeEventListener('resize', listener);
    };
  }, []);

  return { containerRef, containerWidth };
}
