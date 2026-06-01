import { Composition } from 'remotion';
import { HomestayDemo, fps, totalFrames } from './HomestayDemo.jsx';

export const RemotionRoot = () => {
  return (
    <Composition
      id="HomestayDemo"
      component={HomestayDemo}
      durationInFrames={totalFrames}
      fps={fps}
      width={1920}
      height={1080}
    />
  );
};
