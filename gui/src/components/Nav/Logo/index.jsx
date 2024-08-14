import { useRef, Suspense } from "react";
import { Canvas, useFrame, useLoader } from "@react-three/fiber";
// import { OrbitControls } from '@react-three/drei';
import TextMesh from "./TextMesh";
import { FontLoader } from "three/examples/jsm/Addons.js";

function Logo() {
  const font = useLoader(FontLoader, "/font.data");
  return (
    <Suspense fallback={<h1>loading...</h1>}>
      <Canvas dpr={[1, 2]} fallback={<div>Sorry no WebGL supported!</div>}>
        <ambientLight intensity={0.3} />
        {/* <spotLight position={[2, 2, 2]} angle={0.15} penumbra={0.5} intensity={1} shadow-mapSize={2048} castShadow={true} /> */}
        <pointLight castShadow position={[0, 0, -2]} />
        <spotLight castShadow position={[0, 0, 2]} penumbra={1} />

        <TextMesh position={[-1, 0, 0]} character={"G"} font={font} />
        <TextMesh position={[1, 0, 0]} character={"P"} font={font} />
      </Canvas>
    </Suspense>
  );
}

export default Logo;
