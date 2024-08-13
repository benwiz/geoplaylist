import { styled } from '@stitches/react';
import withContainer from '../HOC/withContainer';

const Container = styled('div', {
  // padding: '40px',
  img: {
    width: '100%',
  },
});

function SpotifyTutorial() {
  return (
    <Container>
      <img
        src='/spotifyTutorial.gif'
        alt='spotify-dl-instructions-gif'
      />
    </Container>
  );
}

export default withContainer(SpotifyTutorial);
