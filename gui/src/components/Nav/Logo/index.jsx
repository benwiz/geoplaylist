import { useRef, Suspense } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
// import { OrbitControls } from '@react-three/drei';
import TextMesh from './TextMesh';

// function Text({ character }) {
//   const ref = useRef();
//   // useFrame((state) => {
//   //   console.log('hovering text group');
//   // });
//   return (
//     <group ref={ref}>
//       <TextMesh
//         // size={10}
//         // not working??
//         character={character}
//         onClick={(e) => console.log('sup')}
//       />
//     </group>
//   );
// }

function Logo() {
  return (
    <Suspense fallback={<h1>loading...</h1>}>
      <Canvas
        dpr={[1, 2]}
        fallback={<div>Sorry no WebGL supported!</div>}
      >
        <ambientLight intensity={0.5} />
        <spotLight
          position={[10, 10, 10]}
          angle={0.15}
          penumbra={1}
        />
        <pointLight position={[-10, -10, -10]} />
        <TextMesh
          position={[-1, 0, 0]}
          character={'G'}
        />
        <TextMesh
          position={[1, 0, 0]}
          character={'P'}
        />
      </Canvas>
    </Suspense>
  );
}

export default Logo;
