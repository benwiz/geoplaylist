import React, { useState, useEffect } from 'react';
import Loading from './components/Loading/Loading';
import Nav from './components/Nav/Nav';
import fetchAsciiArt from './utility/asciiFetch';

const App: React.FC = () => {
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [asciiArt, setAsciiArt] = useState<string | null>(null);

  useEffect(() => {
    fetch('http://localhost:8008/test/health')
      .then((r) => r.json())
      .then(console.log);
    fetch('http://localhost:8008/test/csv')
      .then((r) => r.text())
      .then(console.log);
    fetch('http://localhost:8008/pretrained') // just an example, a functioning upload is more important than rendering any results right now.
      .then((r) => r.text())
      .then(console.log);
  }, []);

  useEffect(() => {
    // Simulate a 2-second loading state when the component first mounts
    const initialLoadingTimeout = setTimeout(() => {
      setIsLoading(false);
    }, 2000);

    // Grab ascii art from .txt
    // const fetchAsciiAndUpdateState = async () => {
    //   try {
    //     const asciiTxt = await fetchAsciiArt();
    //     setAsciiArt(asciiTxt);
    //   } catch (error) {
    //     console.log('bad ascii request');
    //     setAsciiArt('no dinos here...');
    //   }
    // };
    // fetchAsciiAndUpdateState();

    // Alternative style of the ascii art fetch
    // Not necessarily better, just more traditional and more functional.
    fetchAsciiArt()
      .then(setAsciiArt)
      .catch((e) => {
        console.log('bad ascii request');
        setAsciiArt('no dinos here');
      });

    // Clean up timeout
    return () => clearTimeout(initialLoadingTimeout);
  }, []);

  return (
    <>
      {isLoading ? (
        <Loading />
      ) : (
        <>
          <Nav />
          <div>
            Welcome, to jurassic park
            {asciiArt && <pre>{asciiArt}</pre>}
          </div>
        </>
      )}
    </>
  );
};

export default App;
