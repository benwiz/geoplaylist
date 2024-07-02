import { styled } from '@stitches/react';
import GoogleTutorial from './GoogleTutorial';

const Container = styled('div', {
  padding: '0',
});

function Tutorial() {
  // const [count, setCount] = useState(0);

  return (
    <Container>
      <GoogleTutorial />
    </Container>
  );
}

export default Tutorial;
