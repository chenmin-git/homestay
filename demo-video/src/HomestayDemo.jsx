import React from 'react';
import {
  AbsoluteFill,
  Easing,
  Img,
  interpolate,
  staticFile,
  useCurrentFrame
} from 'remotion';
import scenes from './full-demo-scenes.json';
import captions from './full-demo-captions.json';

export const fps = 30;
const sceneSeconds = 4;
const sceneFrames = fps * sceneSeconds;
export const totalFrames = scenes.length * sceneFrames;

const palette = {
  ink: '#14213d',
  muted: '#53606f',
  line: '#d9e2ec',
  paper: '#f8fafc',
  brand: '#0f766e',
  accent: '#eab308',
  warm: '#f97316'
};

const ease = Easing.bezier(0.16, 1, 0.3, 1);

const Pill = ({ children, color = palette.brand }) => (
  <div
    style={{
      border: `2px solid ${color}`,
      borderRadius: 999,
      color,
      fontSize: 30,
      fontWeight: 700,
      padding: '16px 28px',
      backgroundColor: 'rgba(255,255,255,0.84)'
    }}
  >
    {children}
  </div>
);

const RoleScene = ({ scene, caption, localFrame }) => {
  const opacity = interpolate(localFrame, [0, 22], [0, 1], {
    extrapolateRight: 'clamp',
    easing: ease
  });
  const cardY = interpolate(localFrame, [0, 32], [34, 0], {
    extrapolateRight: 'clamp',
    easing: ease
  });
  const roleColors = [palette.brand, palette.warm, palette.accent];

  return (
    <AbsoluteFill style={{ backgroundColor: palette.paper, padding: '72px 88px' }}>
      <div style={{ opacity }}>
        <div style={{ color: palette.brand, fontSize: 32, fontWeight: 900, marginBottom: 18 }}>
          登录后的角色分工
        </div>
        <h1 style={{ color: palette.ink, fontSize: 82, lineHeight: 1.08, margin: 0 }}>
          {scene.title}
        </h1>
        <p style={{ color: palette.muted, fontSize: 34, lineHeight: 1.45, marginTop: 22 }}>
          {scene.subtitle}
        </p>
      </div>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(3, 1fr)',
          gap: 28,
          marginTop: 62,
          transform: `translateY(${cardY}px)`,
          opacity
        }}
      >
        {scene.roles.map((role, index) => (
          <div
            key={role.name}
            style={{
              minHeight: 390,
              border: `2px solid ${palette.line}`,
              borderRadius: 28,
              backgroundColor: '#ffffff',
              padding: 34,
              boxShadow: '0 22px 56px rgba(15, 23, 42, 0.10)'
            }}
          >
            <div
              style={{
                width: 72,
                height: 72,
                borderRadius: 22,
                backgroundColor: roleColors[index],
                color: '#ffffff',
                display: 'grid',
                placeItems: 'center',
                fontSize: 36,
                fontWeight: 950,
                marginBottom: 30
              }}
            >
              {index + 1}
            </div>
            <div style={{ color: palette.ink, fontSize: 42, fontWeight: 900, marginBottom: 16 }}>
              {role.name}
            </div>
            <div
              style={{
                display: 'inline-flex',
                color: roleColors[index],
                border: `2px solid ${roleColors[index]}`,
                borderRadius: 999,
                padding: '10px 18px',
                fontSize: 24,
                fontWeight: 850,
                marginBottom: 28
              }}
            >
              演示账号：{role.account}
            </div>
            <div style={{ color: palette.muted, fontSize: 30, lineHeight: 1.5, fontWeight: 700 }}>
              {role.focus}
            </div>
          </div>
        ))}
      </div>

      <div
        style={{
          position: 'absolute',
          left: 88,
          right: 88,
          bottom: 58,
          borderTop: `2px solid ${palette.line}`,
          paddingTop: 26,
          color: palette.ink,
          fontSize: 34,
          fontWeight: 820,
          opacity
        }}
      >
        {caption}
      </div>
    </AbsoluteFill>
  );
};

const IntroScene = ({ scene, localFrame }) => {
  const titleY = interpolate(localFrame, [0, 34], [32, 0], {
    extrapolateRight: 'clamp',
    easing: ease
  });
  const opacity = interpolate(localFrame, [0, 24], [0, 1], {
    extrapolateRight: 'clamp',
    easing: ease
  });

  return (
    <AbsoluteFill style={{ backgroundColor: palette.paper, padding: 88 }}>
      <div
        style={{
          opacity,
          transform: `translateY(${titleY}px)`,
          width: 980
        }}
      >
        <div style={{ color: palette.brand, fontSize: 34, fontWeight: 800, marginBottom: 22 }}>
          项目演示
        </div>
        <h1 style={{ color: palette.ink, fontSize: 92, lineHeight: 1.05, margin: 0 }}>
          {scene.title}
        </h1>
        <p style={{ color: palette.muted, fontSize: 38, lineHeight: 1.5, marginTop: 28 }}>
          {scene.subtitle}
        </p>
      </div>

      <div style={{ display: 'flex', gap: 18, flexWrap: 'wrap', marginTop: 58, width: 1060 }}>
        {scene.modules.map((item, index) => (
          <Pill key={item} color={index % 2 === 0 ? palette.brand : palette.warm}>
            {item}
          </Pill>
        ))}
      </div>

      <div
        style={{
          position: 'absolute',
          right: 92,
          top: 150,
          width: 590,
          display: 'grid',
          gap: 20
        }}
      >
        {scene.architecture.map((item, index) => (
          <div
            key={item}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 18,
              opacity: interpolate(localFrame, [18 + index * 6, 42 + index * 6], [0, 1], {
                extrapolateRight: 'clamp',
                easing: ease
              })
            }}
          >
            <div
              style={{
                width: 54,
                height: 54,
                borderRadius: 16,
                backgroundColor: index % 2 === 0 ? palette.brand : palette.accent,
                color: 'white',
                fontSize: 28,
                fontWeight: 900,
                display: 'grid',
                placeItems: 'center'
              }}
            >
              {index + 1}
            </div>
            <div
              style={{
                flex: 1,
                border: `2px solid ${palette.line}`,
                backgroundColor: 'white',
                borderRadius: 22,
                padding: '22px 26px',
                color: palette.ink,
                fontSize: 32,
                fontWeight: 800
              }}
            >
              {item}
            </div>
          </div>
        ))}
      </div>
    </AbsoluteFill>
  );
};

const ScreenshotScene = ({ scene, caption, localFrame }) => {
  const imageScale = interpolate(localFrame, [0, sceneFrames], [1.015, 1], {
    extrapolateRight: 'clamp',
    easing: ease
  });
  const titleOpacity = interpolate(localFrame, [0, 24], [0, 1], {
    extrapolateRight: 'clamp',
    easing: ease
  });

  return (
    <AbsoluteFill style={{ backgroundColor: '#ffffff' }}>
      <div
        style={{
          position: 'absolute',
          inset: '44px 204px 186px 204px',
          border: `2px solid ${palette.line}`,
          borderRadius: 28,
          overflow: 'hidden',
          backgroundColor: '#ffffff',
          boxShadow: '0 28px 70px rgba(15, 23, 42, 0.16)'
        }}
      >
        <Img
          src={staticFile(scene.image)}
          style={{
            width: '100%',
            height: '100%',
            objectFit: 'contain',
            transform: `scale(${imageScale})`
          }}
        />
      </div>
      <div
        style={{
          position: 'absolute',
          left: 72,
          right: 72,
          bottom: 48,
          display: 'flex',
          alignItems: 'flex-end',
          justifyContent: 'flex-start',
          opacity: titleOpacity
        }}
      >
        <div style={{ maxWidth: 1640 }}>
          <div style={{ color: palette.brand, fontSize: 28, fontWeight: 900, marginBottom: 12 }}>
            {scene.title}
          </div>
          <div style={{ color: palette.ink, fontSize: 46, fontWeight: 850, lineHeight: 1.2 }}>
            {caption}
          </div>
        </div>
      </div>
    </AbsoluteFill>
  );
};

export const HomestayDemo = () => {
  const frame = useCurrentFrame();
  const sceneIndex = Math.min(scenes.length - 1, Math.floor(frame / sceneFrames));
  const localFrame = frame - sceneIndex * sceneFrames;
  const scene = scenes[sceneIndex];
  const caption = captions[sceneIndex];

  return (
    <AbsoluteFill style={{ fontFamily: 'Arial, "PingFang SC", "Microsoft YaHei", sans-serif' }}>
      {scene.kind === 'intro' ? (
        <IntroScene scene={scene} localFrame={localFrame} />
      ) : scene.kind === 'roles' ? (
        <RoleScene scene={scene} caption={caption} localFrame={localFrame} />
      ) : (
        <ScreenshotScene scene={scene} caption={caption} localFrame={localFrame} />
      )}
      <div
        style={{
          position: 'absolute',
          left: 0,
          right: 0,
          bottom: 0,
          height: 12,
          backgroundColor: palette.line
        }}
      >
        <div
          style={{
            height: '100%',
            width: `${((frame + 1) / totalFrames) * 100}%`,
            backgroundColor: palette.brand
          }}
        />
      </div>
      {scene.kind === 'intro' && (
        <div
          style={{
            position: 'absolute',
            left: 72,
            bottom: 64,
            color: palette.ink,
            fontSize: 32,
            fontWeight: 750,
            letterSpacing: 0,
            opacity: interpolate(localFrame, [0, 18], [0, 1], {
              extrapolateRight: 'clamp',
              easing: ease
            })
          }}
        >
          {caption}
        </div>
      )}
    </AbsoluteFill>
  );
};
