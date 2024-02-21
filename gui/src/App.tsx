// import React, { useState, useEffect } from 'react';
import { effect, signal } from '@preact/signals-react';
import { asciiArt } from './components/AsciiThing/AsciiThing';
import Loading from './components/Loading/Loading';

const App = () => {
  const isLoading = signal(true);

  // const [isLoadingHook, setIsLoadingHook] = useState(true);
  // const [asciiArt, setAsciiArt] = useState('');

  // Simulate a 2-second loading state when the component first mounts

  setTimeout(() => {
    isLoading.value = false;
    console.log('bad?', isLoading.value);
  }, 2000);

  // useEffect(() => {
  //   // Simulate a 2-second loading state when the component first mounts
  //   const initialLoadingTimeout = setTimeout(() => {
  //     setIsLoadingHook(false);
  //   }, 2000);

  //   // Grab ascii art from .txt
  //   const fetchAsciiArt = async () => {
  //     try {
  //       setAsciiArt(await (await fetch('./jurassic_park.txt')).text());
  //     } catch (error) {
  //       console.error('Error fetching ASCII art:', error);
  //     }
  //   };
  //   fetchAsciiArt();

  //   // Clean up timeout
  //   return () => clearTimeout(initialLoadingTimeout);
  // }, []);

  //   return (
  //     <>
  //       {isLoadingHook ? (
  //         <Loading />
  //       ) : (
  //         <div>
  //           Welcome, to jurassic park
  //           {asciiArt && <pre>{asciiArt}</pre>}
  //         </div>
  //       )}
  //     </>
  //   );
  // };

  console.log('ascii:', asciiArt.value);
  return (
    <>
      {isLoading ? (
        <Loading />
      ) : (
        <div>
          Welcome, to jurassic park
          {asciiArt && <pre>{asciiArt.value}</pre>}
        </div>
      )}
    </>
  );
};

export default App;
