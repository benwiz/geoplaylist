import { styled } from '@stitches/react';

const Container = styled('div', {
  padding: '0',
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

export default GoogleTutorial;
