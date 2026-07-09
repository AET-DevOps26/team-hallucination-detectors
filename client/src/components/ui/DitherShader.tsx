import { CSSProperties, useEffect, useRef } from "react";

type Variant = "square" | "circle" | "triangle" | "diamond";

type DitherShaderProps = {
  enabled?: boolean;
  variant?: Variant;
  /** Base color when the gradient is disabled. */
  color?: string;
  pixelSize?: number;
  opacity?: number;
  edgeFade?: number;
  patternScale?: number;
  patternDensity?: number;
  pixelSizeJitter?: number;
  noiseAmount?: number;
  speed?: number;
  rotationSpeed?: number;
  /** Click-to-ripple. Requires the canvas to receive pointer events. */
  enableRipples?: boolean;
  rippleIntensityScale?: number;
  rippleThickness?: number;
  rippleSpeed?: number;
  enableGradient?: boolean;
  gradientColor1?: string;
  gradientColor2?: string;
  colorPulse?: boolean;
  pulseSpeed?: number;
  pulseIntensity?: number;
  /** Radius (in aspect-corrected UV units) of the color pools anchored in the
   *  two bottom corners. 0 disables them and keeps the plain vertical gradient. */
  cornerGlow?: number;
  /** Color of the bottom-left / bottom-right pools. Default to the gradient. */
  cornerColorLeft?: string;
  cornerColorRight?: string;
  /** How much the corner pools brighten the dither density (more dots). */
  cornerIntensity?: number;
  /** Upper bound on device pixel ratio to keep the fill-rate sane. */
  maxDpr?: number;
  className?: string;
  style?: CSSProperties;
};

const SHAPE_MAP: Record<Variant, number> = {
  square: 0,
  circle: 1,
  triangle: 2,
  diamond: 3,
};
const MAX_CLICKS = 10;

const VERTEX_SHADER = `#version 300 es
in vec2 aPos;
void main() {
  gl_Position = vec4(aPos, 0.0, 1.0);
}`;

// Fragment shader ported verbatim from the Framer "DitherShader" module
// (framer.com/m/DitherShader-RQNs.js). Only the GLSL ES 3.00 header and the
// single output declaration differ from the original Three.js material.
const FRAGMENT_SHADER = `#version 300 es
precision highp float;
precision highp int;
out vec4 fragColor;

uniform vec2 uResolution;
uniform float uTime;
uniform float uPixelSize;
uniform vec3 uColor;
uniform float uPatternScale;
uniform float uPatternDensity;
uniform float uPixelSizeJitter;
uniform float uNoiseAmount;
uniform int uShapeType;
const int SHAPE_SQUARE   = 0;
const int SHAPE_CIRCLE   = 1;
const int SHAPE_TRIANGLE = 2;
const int SHAPE_DIAMOND  = 3;
uniform bool uEnableRipples;
uniform float uRippleIntensity;
uniform float uRippleThickness;
uniform float uRippleSpeed;
const int MAX_CLICKS = 10;
uniform vec2 uClickPos[MAX_CLICKS];
uniform float uClickTimes[MAX_CLICKS];
uniform bool uEnableGradient;
uniform vec3 uGradientColor1;
uniform vec3 uGradientColor2;
uniform float uGradientSpeed;
uniform bool uColorPulse;
uniform float uPulseSpeed;
uniform float uPulseIntensity;
uniform vec2 uRotSC;
uniform float uEdgeFade;
uniform float uAspect;
uniform float uMaxRippleTime;
uniform float uCornerGlow;
uniform vec3 uCornerColorL;
uniform vec3 uCornerColorR;
uniform float uCornerIntensity;

float hash21(vec2 p){ return fract(sin(dot(p, vec2(12.9898,78.233))) * 43758.5453); }
float Bayer2(vec2 a){
    a=floor(a);
    return fract(a.x/2. + a.y * a.y * .75);
}
#define Bayer4(a) (Bayer2(.5*(a))*0.25 + Bayer2(a))
#define Bayer8(a) (Bayer4(.5*(a))*0.25 + Bayer2(a))
float noise(vec2 p){
    vec2 i=floor(p);
    vec2 f=fract(p);
    float a=hash21(i);
    float b=hash21(i + vec2(1.0,0.0));
    float c=hash21(i + vec2(0.0,1.0));
    float d=hash21(i + vec2(1.0,1.0));
    vec2 u = f*f*(3.0-2.0*f);
    return mix(a,b,u.x) + (c-a)*u.y*(1.0-u.x) + (d-b)*u.x*u.y;
}
float maskCircle(vec2 p, float cov){
    float r = sqrt(cov) * 0.25;
    float d = length(p - 0.5) - r;
    float aa = 0.5 * fwidth(d);
    return cov * (1.0 - smoothstep(-aa, aa, d * 2.0));
}
float maskTriangle(vec2 p, vec2 id, float cov){
    bool flip = mod(id.x + id.y, 2.0) > 0.5;
    if (flip) p.x = 1.0 - p.x;
    float r = sqrt(cov);
    float d  = p.y - r*(1.0 - p.x);
    float aa = fwidth(d);
    return cov * clamp(0.5 - d/aa, 0.0, 1.0);
}
float maskDiamond(vec2 p, float cov){
    float r = sqrt(cov) * 0.564;
    return step(abs(p.x - 0.49) + abs(p.y - 0.49), r);
}

void main(){
    vec2 uv = gl_FragCoord.xy / uResolution.xy;
    vec2 centered = (gl_FragCoord.xy - 0.5 * uResolution.xy) / uResolution.y;
    if(uRotSC.x != 0.0 || uRotSC.y != 1.0){
        mat2 rot = mat2(uRotSC.y, -uRotSC.x, uRotSC.x, uRotSC.y);
        centered = rot * centered;
    }
    float px = max(1.0, uPixelSize);
    if(uPixelSizeJitter > 0.0){
        float jitter = hash21(floor(gl_FragCoord.xy / (px*10.0))) * uPixelSizeJitter;
        px += jitter;
    }
    vec2 pcell = floor(gl_FragCoord.xy / px) * px;
    vec2 pUv = (pcell - 0.5 * uResolution.xy) / uResolution.y;
    if(uRotSC.x != 0.0 || uRotSC.y != 1.0){
        mat2 rot = mat2(uRotSC.y, -uRotSC.x, uRotSC.x, uRotSC.y);
        pUv = rot * pUv;
    }
    vec2 pixelId = floor(gl_FragCoord.xy / px);
    vec2 pixelUV = fract(gl_FragCoord.xy / px);
    float cellPixelSize = 8.0 * uPixelSize;
    vec2 cellId = floor(gl_FragCoord.xy / cellPixelSize);
    vec2 cellCoord = cellId * cellPixelSize;
    vec2 cellUv = cellCoord / uResolution.xy;
    vec2 aspectUv = cellUv * vec2(uAspect, 1.0);
    float base = noise(aspectUv * uPatternScale * 8.0 + uTime * 0.05);
    base = base * 0.5 - 0.65;
    float feed = base + (uPatternDensity - 0.5) * 0.3;
    // Color pools anchored in the two bottom corners. Distance is aspect-
    // corrected so the pools stay circular on a wide canvas. gL/gR are reused
    // below to tint the base color; here they lift the dither density so dots
    // actually render in the corners.
    float gL = 0.0;
    float gR = 0.0;
    if(uCornerGlow > 0.0){
        vec2 ar = vec2(uAspect, 1.0);
        vec2 p = uv * ar;
        gL = smoothstep(uCornerGlow, 0.0, distance(p, vec2(0.0, 0.0)));
        gR = smoothstep(uCornerGlow, 0.0, distance(p, vec2(uAspect, 0.0)));
        feed += max(gL, gR) * uCornerIntensity;
    }
    if(uEnableRipples){
        float speed = uRippleSpeed;
        float thickness = uRippleThickness;
        const float dampT = 2.5;
        const float dampR = 10.0;
        for(int i = 0; i < MAX_CLICKS; ++i){
            vec2 pos = uClickPos[i];
            if(pos.x < 0.0) continue;
            float t = max(uTime - uClickTimes[i], 0.0);
            if(t > uMaxRippleTime) continue;
            vec2 cuv = (pos / uResolution) * vec2(uAspect, 1.0);
            float r = distance(aspectUv, cuv);
            float waveR = speed * t;
            float ring = exp(-pow((r - waveR) / thickness, 2.0));
            float atten = exp(-dampT * t) * exp(-dampR * r);
            feed = max(feed, ring * atten * uRippleIntensity);
        }
    }
    if(uColorPulse){
        feed += sin(uTime * uPulseSpeed) * uPulseIntensity * 0.1;
    }
    float bayer = Bayer8(gl_FragCoord.xy / px) - 0.5;
    float bw = step(0.5, feed + bayer);
    float h = hash21(floor(gl_FragCoord.xy / px));
    float jitterScale = 1.0 + (h - 0.5) * uPixelSizeJitter * 0.1;
    float coverage = bw * jitterScale;
    float M;
    if      (uShapeType == SHAPE_CIRCLE)   M = maskCircle(pixelUV, coverage);
    else if (uShapeType == SHAPE_TRIANGLE) M = maskTriangle(pixelUV, pixelId, coverage);
    else if (uShapeType == SHAPE_DIAMOND)  M = maskDiamond(pixelUV, coverage);
    else                                   M = coverage;
    vec3 baseColor = uColor;
    if(uEnableGradient){
        // Static gradient from bottom (Color 1) to top (Color 2)
        float g = uv.y;
        baseColor = mix(uGradientColor1, uGradientColor2, g);
        // Blend the corner pools over the vertical gradient.
        baseColor = mix(baseColor, uCornerColorL, gL);
        baseColor = mix(baseColor, uCornerColorR, gR);
    }
    vec3 col = baseColor * M;
    float alpha = M;
    if(uEdgeFade > 0.0){
        float d = length(uv - 0.5);
        float fade = smoothstep(0.8 - uEdgeFade, 0.8, d);
        col *= (1.0 - fade);
        alpha *= (1.0 - fade);
    }
    fragColor = vec4(col, alpha);
}`;

function hexToRgb(hex: string): [number, number, number] {
  const value = hex.replace("#", "");
  const full =
    value.length === 3
      ? value
          .split("")
          .map((c) => c + c)
          .join("")
      : value;
  const int = parseInt(full, 16);
  if (Number.isNaN(int)) return [0, 0, 0];
  return [((int >> 16) & 255) / 255, ((int >> 8) & 255) / 255, (int & 255) / 255];
}

function compile(gl: WebGL2RenderingContext, type: number, source: string) {
  const shader = gl.createShader(type);
  if (!shader) return null;
  gl.shaderSource(shader, source);
  gl.compileShader(shader);
  if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
    console.error("DitherShader compile error:", gl.getShaderInfoLog(shader));
    gl.deleteShader(shader);
    return null;
  }
  return shader;
}

/**
 * WebGL2 port of the Framer "DitherShader" — animated Bayer-matrix dithering
 * with an optional vertical gradient, click ripples, rotation, and edge fade.
 * Decorative and self-contained: degrades to nothing without WebGL2, renders a
 * single static frame under prefers-reduced-motion, and cleans up on unmount.
 */
export function DitherShader({
  enabled = true,
  variant = "circle",
  color = "#000000",
  pixelSize = 4,
  opacity = 1,
  edgeFade = 0,
  patternScale = 1,
  patternDensity = 2,
  pixelSizeJitter = 0,
  noiseAmount = 0.7,
  speed = 0.1,
  rotationSpeed = 0,
  enableRipples = false,
  rippleIntensityScale = 1,
  rippleThickness = 0.12,
  rippleSpeed = 0.4,
  enableGradient = true,
  gradientColor1 = "#FF0000",
  gradientColor2 = "#0810FF",
  colorPulse = false,
  pulseSpeed = 1,
  pulseIntensity = 0.2,
  cornerGlow = 0,
  cornerColorLeft,
  cornerColorRight,
  cornerIntensity = 0.35,
  maxDpr = 2,
  className = "",
  style,
}: DitherShaderProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    if (!enabled) return;
    const canvasEl = canvasRef.current;
    if (!canvasEl) return;
    const canvas = canvasEl;

    const context = canvas.getContext("webgl2", {
      antialias: false,
      alpha: true,
      premultipliedAlpha: false,
    });
    if (!context) return;
    const gl = context;

    const vertex = compile(gl, gl.VERTEX_SHADER, VERTEX_SHADER);
    const fragment = compile(gl, gl.FRAGMENT_SHADER, FRAGMENT_SHADER);
    if (!vertex || !fragment) return;

    const program = gl.createProgram();
    if (!program) return;
    gl.attachShader(program, vertex);
    gl.attachShader(program, fragment);
    gl.linkProgram(program);
    if (!gl.getProgramParameter(program, gl.LINK_STATUS)) return;
    gl.useProgram(program);

    const buffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
    gl.bufferData(
      gl.ARRAY_BUFFER,
      new Float32Array([-1, -1, 1, -1, -1, 1, -1, 1, 1, -1, 1, 1]),
      gl.STATIC_DRAW,
    );
    const posLoc = gl.getAttribLocation(program, "aPos");
    gl.enableVertexAttribArray(posLoc);
    gl.vertexAttribPointer(posLoc, 2, gl.FLOAT, false, 0, 0);

    const u = (name: string) => gl.getUniformLocation(program, name);
    const loc = {
      resolution: u("uResolution"),
      time: u("uTime"),
      pixelSize: u("uPixelSize"),
      color: u("uColor"),
      patternScale: u("uPatternScale"),
      patternDensity: u("uPatternDensity"),
      pixelSizeJitter: u("uPixelSizeJitter"),
      noiseAmount: u("uNoiseAmount"),
      shapeType: u("uShapeType"),
      enableRipples: u("uEnableRipples"),
      rippleIntensity: u("uRippleIntensity"),
      rippleThickness: u("uRippleThickness"),
      rippleSpeed: u("uRippleSpeed"),
      clickPos: u("uClickPos[0]"),
      clickTimes: u("uClickTimes[0]"),
      enableGradient: u("uEnableGradient"),
      gradientColor1: u("uGradientColor1"),
      gradientColor2: u("uGradientColor2"),
      gradientSpeed: u("uGradientSpeed"),
      colorPulse: u("uColorPulse"),
      pulseSpeed: u("uPulseSpeed"),
      pulseIntensity: u("uPulseIntensity"),
      rotSC: u("uRotSC"),
      edgeFade: u("uEdgeFade"),
      aspect: u("uAspect"),
      maxRippleTime: u("uMaxRippleTime"),
      cornerGlow: u("uCornerGlow"),
      cornerColorL: u("uCornerColorL"),
      cornerColorR: u("uCornerColorR"),
      cornerIntensity: u("uCornerIntensity"),
    };

    // Static appearance uniforms — set once; the loop only touches time/rotation.
    gl.uniform1f(loc.pixelSize, pixelSize);
    gl.uniform3fv(loc.color, hexToRgb(color));
    gl.uniform1f(loc.patternScale, patternScale);
    gl.uniform1f(loc.patternDensity, patternDensity);
    gl.uniform1f(loc.pixelSizeJitter, pixelSizeJitter);
    gl.uniform1f(loc.noiseAmount, noiseAmount);
    gl.uniform1i(loc.shapeType, SHAPE_MAP[variant] ?? 0);
    gl.uniform1i(loc.enableRipples, enableRipples ? 1 : 0);
    gl.uniform1f(loc.rippleIntensity, rippleIntensityScale);
    gl.uniform1f(loc.rippleThickness, rippleThickness);
    gl.uniform1f(loc.rippleSpeed, rippleSpeed);
    gl.uniform1i(loc.enableGradient, enableGradient ? 1 : 0);
    gl.uniform3fv(loc.gradientColor1, hexToRgb(gradientColor1));
    gl.uniform3fv(loc.gradientColor2, hexToRgb(gradientColor2));
    gl.uniform1f(loc.gradientSpeed, 0.5);
    gl.uniform1i(loc.colorPulse, colorPulse ? 1 : 0);
    gl.uniform1f(loc.pulseSpeed, pulseSpeed);
    gl.uniform1f(loc.pulseIntensity, pulseIntensity);
    gl.uniform1f(loc.edgeFade, edgeFade);
    gl.uniform1f(loc.maxRippleTime, 3);
    gl.uniform1f(loc.cornerGlow, cornerGlow);
    gl.uniform3fv(loc.cornerColorL, hexToRgb(cornerColorLeft ?? gradientColor1));
    gl.uniform3fv(loc.cornerColorR, hexToRgb(cornerColorRight ?? gradientColor2));
    gl.uniform1f(loc.cornerIntensity, cornerIntensity);

    // Ripple ring buffer, mirrored to the GPU each frame while active.
    const clickPos = new Float32Array(MAX_CLICKS * 2).fill(-1);
    const clickTimes = new Float32Array(MAX_CLICKS);
    let clickIndex = 0;
    let uTime = 0;

    gl.enable(gl.BLEND);
    gl.blendFunc(gl.ONE, gl.ONE_MINUS_SRC_ALPHA);

    let dpr = Math.min(window.devicePixelRatio || 1, maxDpr);
    function resize() {
      const w = Math.max(1, canvas.clientWidth);
      const h = Math.max(1, canvas.clientHeight);
      const width = Math.floor(w * dpr);
      const height = Math.floor(h * dpr);
      if (canvas.width !== width || canvas.height !== height) {
        canvas.width = width;
        canvas.height = height;
      }
      gl.viewport(0, 0, canvas.width, canvas.height);
      gl.uniform2f(loc.resolution, canvas.width, canvas.height);
      gl.uniform1f(loc.aspect, canvas.width / canvas.height);
    }
    resize();

    const reduceMotion =
      window.matchMedia?.("(prefers-reduced-motion: reduce)").matches ?? false;

    function draw() {
      // Match the source's rotation matrix (sin/cos of the rotation angle).
      if (rotationSpeed !== 0) {
        const angle = uTime * rotationSpeed;
        gl.uniform2f(loc.rotSC, Math.sin(angle), Math.cos(angle));
      } else {
        gl.uniform2f(loc.rotSC, 0, 1);
      }
      gl.uniform1f(loc.time, uTime);
      if (enableRipples) {
        gl.uniform2fv(loc.clickPos, clickPos);
        gl.uniform1fv(loc.clickTimes, clickTimes);
      }
      gl.drawArrays(gl.TRIANGLES, 0, 6);
    }

    let frame = 0;
    const start = performance.now();
    function render(now: number) {
      // Source advances time as elapsedSeconds * speed * 5.
      uTime = ((now - start) / 1000) * speed * 5;
      // Expire ripples past their lifetime so stale clicks stop contributing.
      if (enableRipples) {
        for (let i = 0; i < MAX_CLICKS; i++) {
          if (clickTimes[i] > 0 && uTime - clickTimes[i] > 3) {
            clickPos[i * 2] = -1;
            clickPos[i * 2 + 1] = -1;
            clickTimes[i] = 0;
          }
        }
      }
      draw();
      if (!reduceMotion) frame = requestAnimationFrame(render);
    }
    frame = requestAnimationFrame(render);

    const onResize = () => {
      dpr = Math.min(window.devicePixelRatio || 1, maxDpr);
      resize();
    };
    window.addEventListener("resize", onResize);
    const ro = new ResizeObserver(resize);
    ro.observe(canvas);

    // Pause the loop while fully offscreen to save the GPU.
    let visible = true;
    const io = new IntersectionObserver(
      (entries) => {
        const wasVisible = visible;
        visible = entries[0]?.isIntersecting ?? true;
        if (visible && !wasVisible && !reduceMotion) {
          cancelAnimationFrame(frame);
          frame = requestAnimationFrame(render);
        } else if (!visible) {
          cancelAnimationFrame(frame);
        }
      },
      { threshold: 0 },
    );
    io.observe(canvas);

    function onPointerDown(event: PointerEvent) {
      const rect = canvas.getBoundingClientRect();
      const scaleX = canvas.width / rect.width;
      const scaleY = canvas.height / rect.height;
      const fx = (event.clientX - rect.left) * scaleX;
      const fy = (rect.height - (event.clientY - rect.top)) * scaleY;
      clickPos[clickIndex * 2] = fx;
      clickPos[clickIndex * 2 + 1] = fy;
      clickTimes[clickIndex] = uTime;
      clickIndex = (clickIndex + 1) % MAX_CLICKS;
    }
    if (enableRipples) {
      canvas.addEventListener("pointerdown", onPointerDown, { passive: true });
    }

    return () => {
      cancelAnimationFrame(frame);
      window.removeEventListener("resize", onResize);
      ro.disconnect();
      io.disconnect();
      if (enableRipples) canvas.removeEventListener("pointerdown", onPointerDown);
      gl.deleteProgram(program);
      gl.deleteShader(vertex);
      gl.deleteShader(fragment);
      gl.deleteBuffer(buffer);
      // NB: don't force-lose the context here — React StrictMode remounts on the
      // same <canvas>, and a lost context would make the remount's shaders fail.
    };
  }, [
    enabled,
    variant,
    color,
    pixelSize,
    edgeFade,
    patternScale,
    patternDensity,
    pixelSizeJitter,
    noiseAmount,
    speed,
    rotationSpeed,
    enableRipples,
    rippleIntensityScale,
    rippleThickness,
    rippleSpeed,
    enableGradient,
    gradientColor1,
    gradientColor2,
    colorPulse,
    pulseSpeed,
    pulseIntensity,
    cornerGlow,
    cornerColorLeft,
    cornerColorRight,
    cornerIntensity,
    maxDpr,
  ]);

  if (!enabled) return null;

  return (
    <canvas
      aria-hidden="true"
      className={className}
      ref={canvasRef}
      style={{
        display: "block",
        height: "100%",
        width: "100%",
        opacity,
        ...style,
      }}
    />
  );
}
