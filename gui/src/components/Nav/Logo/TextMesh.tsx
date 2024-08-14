import { useRef } from "react";
import { TextGeometry } from "three/examples/jsm/Addons.js";
import { useFrame, extend } from "@react-three/fiber";

extend({ TextGeometry });

function TextMesh({ character, position, font }: { character: string; position: number[]; font: any }) {
  const textMesh = useRef();
  // const font = useLoader(FontLoader, "/font.data");
  useFrame((state, delta) => (textMesh.current.rotation.x = textMesh.current.rotation.y += delta));
  return (
    <mesh castShadow receiveShadow ref={textMesh} position={position}>
      <textGeometry args={[character, { font: font, size: 1, depth: 0.2 }]} />
      <meshStandardMaterial attach="material" color="orange" />
    </mesh>
  );
}

export default TextMesh;
