import { styled } from "@stitches/react";

const ContainerDiv = styled("div", {
  display: "flex",
  justifyContent: "center",
  borderStyle: "solid",
  borderColor: "grey",
  borderWidth: "2px",
  borderRadius: "5px",
  marginLeft: "5px",
  marginRight: "5px",
  padding: "40px",
  // objectFit: 'contain',
});

const withContainer = (WrappedComponent: React.FC) => {
  //build these styles from 'containerStyles'

  return (props) => (
    <ContainerDiv>
      <WrappedComponent {...props} />
    </ContainerDiv>
  );
};

export default withContainer;
