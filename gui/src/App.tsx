// import React, { useState, useEffect } from 'react';
// import { useEffect } from 'react';

// import { effect, signal } from '@preact/signals-react';
import { useSignalEffect, useSignal } from '@preact/signals-react';
import { asciiArt } from './components/AsciiThing/AsciiThing';
import Loading from './components/Loading/Loading';

const App = () => {
  const isLoading = useSignal<boolean>(true);
  useSignalEffect(() => {
    console.log('loading?', isLoading.value);
  });

  // let loading = null;

  // Simulate a 2-second loading state when the component first mounts

  useSignalEffect(() => {
    if (isLoading.value) {
      setTimeout(() => {
        isLoading.value = !isLoading.peek();
        console.log('bad?', isLoading.value);
      }, 2000);
    }
    // loading = isLoading.value;
  });

  // console.log('ascii:', asciiArt.value);
  return (
    <>
      <span>sup?{isLoading.value.toString()}</span>
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

// const [isLoadingHook, setIsLoadingHook] = useState(true);
// const [asciiArt, setAsciiArt] = useState('');

// useEffect(() => {
//   // Simulate a 2-second loading state when the component first mounts
//   const initialLoadingTimeout = setTimeout(() => {
//     isLoading.value = false;
//   }, 2000);

// Clean up timeout
//   return () => clearTimeout(initialLoadingTimeout);
// }, []);
