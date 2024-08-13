import { useRef } from "react";
import { FontLoader, TextGeometry } from "three/examples/jsm/Addons.js";
import { useLoader, useFrame, extend } from "@react-three/fiber";

extend({ TextGeometry });

function TextMesh({ character, position }: { character: string; position: number[] }) {
  const textMesh = useRef();
  const font = useLoader(FontLoader, "/font.data");
  // console.log('props', props);
  useFrame((state, delta) => (textMesh.current.rotation.x = textMesh.current.rotation.y += delta));
  return (
    <mesh ref={textMesh} position={position}>
      <textGeometry args={[character, { font: font, size: 1, depth: 0.1 }]} />
      <meshStandardMaterial color="orange" />
    </mesh>
  );
}

export default TextMesh;
