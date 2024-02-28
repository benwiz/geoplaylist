import React, { useState, useEffect } from 'react';
import Loading from './components/Loading/Loading';
// import { fetchAsciiArt } from './components/AsciiThing/AsciiThing';
const App = () => {
  const [isLoading, setIsLoading] = useState(true);
  const [asciiArt, setAsciiArt] = useState('');

  useEffect(() => {
    // Simulate a 2-second loading state when the component first mounts
    const initialLoadingTimeout = setTimeout(() => {
      setIsLoading(false);
    }, 2000);

    // Grab ascii art from .txt
    const fetchAsciiArt = async () => {
      try {
        setAsciiArt(await (await fetch('./jurassic_park.txt')).text());
      } catch (error) {
        console.error('Error fetching ASCII art:', error);
      }
    };
    fetchAsciiArt();

    // Clean up timeout
    return () => clearTimeout(initialLoadingTimeout);
  }, []);

  return (
    <>
      {isLoading ? (
        <Loading />
      ) : (
        <div>
          Welcome, to jurassic park
          {asciiArt && <pre>{asciiArt}</pre>}
        </div>
      )}
    </>
  );
};

export default App;
