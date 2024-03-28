// import { useState } from 'react';
import reactLogo from '../../assets/react.svg';
import './Nav.css';

function Nav() {
  // const [count, setCount] = useState(0);

  return (
    <>
      <div className='container'>
        <a
          // link back to homepage
          href='./'
          target='_blank'
        >
          <img
            src={reactLogo}
            className='logo react'
            alt='Placehoder logo'
          />
        </a>
        <div className='banner_text'>
          <p>oh wow, what a nav cmpt</p>
        </div>
      </div>
    </>
  );
}

export default Nav;
