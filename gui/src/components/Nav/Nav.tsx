import { styled } from "@stitches/react";
// import Logo from './Logo';
import Logo from "./Logo";

const CanvasContainer = styled("div", {
  position: "absolute",
  top: 0,
  left: 0,
  pointerEvents: "none",
  width: "240px",
  height: "240px",
});

const Container = styled("div", {
  display: "flex",
  justifyContent: "flex-start",
  height: "60px",
  marginBottom: "620px",
  img: {
    height: "100%",
  },
});

function Nav() {
  return (
    <>
      <CanvasContainer>
        <Logo />
      </CanvasContainer>
      <Container>
        <a
          // link back to homepage
          href="./"
          target="_blank"
        ></a>
        <div>
          <p>oh wow, what a nav cmpt</p>
        </div>
      </Container>
    </>
  );
}

export default Nav;
