import { styled } from '@stitches/react';

const withContainer = (WrappedComponent) => {
  //build these styles from 'containerStyles'
  const ContainerDiv = styled('div', {
    display: 'flex',
    justifyContent: 'center',
    borderStyle: 'solid',
    borderColor: 'grey',
    borderWidth: '2px',
    borderRadius: '5px',
    marginLeft: '5px',
    marginRight: '5px',
    padding: '40px',
    // objectFit: 'contain',
  });

  return (props) => (
    <ContainerDiv>
      <WrappedComponent {...props} />
    </ContainerDiv>
  );
};

export default withContainer;
