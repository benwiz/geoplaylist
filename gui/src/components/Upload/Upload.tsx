import { useState } from 'react';
import './Upload.css';

const containerStyles = {
  borderStyle: 'solid',
  borderRadius: '1rem',
  borderWidth: '2px',
  borderColor: 'azure',
  padding: '8px',
};

function Upload() {
  // const [locData, setlocData] = useState();
  // const [musicData, setmusicData] = useState();

  return (
    <>
      <div style={containerStyles}>
        <h1>Get Started:</h1>
        <form className='itmes'>
          <div className='item'>
            <label>Google Loc Data</label>
            <input
              id='fileSelect'
              type='file'
              accept='.csv'
            />
          </div>
          <div className='item'>
            <label>
              Music History
              <input
                type='radio'
                value={'lastFM'}
                id='LastFM'
                name='musicProvider'
              />
              <input
                type='radio'
                value={'spotify'}
                id='Spotify'
                name='musicProvider'
              />
            </label>
            <input
              id='fileSelect'
              type='file'
              accept='.csv'
            />
          </div>
          <button>Submit</button>
        </form>
      </div>
    </>
  );
}

export default Upload;
