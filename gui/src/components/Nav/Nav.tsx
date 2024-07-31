import reactLogo from '../../assets/react.svg';
import { styled } from '@stitches/react';

const Container = styled('div', {
  display: 'flex',
  justifyContent: 'flex-start',
  height: '40px',
  padding: '5px',
  img: {
    height: '100%',
  },
});

function Nav() {
  return (
    <Container>
      <a
        // link back to homepage
        href='./'
        target='_blank'
      >
        <img
          src={reactLogo}
          alt='Placehoder logo'
        />
      </a>
      <div>
        <p>oh wow, what a nav cmpt</p>
      </div>
    </Container>
  );
}

export default Nav;
