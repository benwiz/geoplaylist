import { styled } from '@stitches/react';
import withContainer from '../HOC/withContainer';

const Container = styled('div', {
  // padding: '40px',
  img: {
    width: '100%',
  },
});

function GoogleTutorial() {
  return (
    <Container>
      <img
        src='/googleLocationDataTutorial.gif'
        alt='spotify-dl-personal-data-gif'
      />
    </Container>
  );
}

export default withContainer(GoogleTutorial);
