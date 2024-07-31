import { styled } from '@stitches/react';
import GoogleTutorial from './GoogleTutorial';
import SpotifyTutorial from './SpotifyTutorial';

const Container = styled('div', {
  display: 'flex',
  justifyContent: 'space-around',
  padding: '0',
  width: '100%',
});

function Tutorial() {
  // const [count, setCount] = useState(0);

  return (
    <Container>
      <SpotifyTutorial />
      <GoogleTutorial />
    </Container>
  );
}

export default Tutorial;
