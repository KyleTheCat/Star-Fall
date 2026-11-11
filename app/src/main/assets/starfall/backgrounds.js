(() => {
  const id = new URLSearchParams(location.search).get('preset') || 'starfield';
  const cv = document.getElementById('c');
  const ctx = cv.getContext('2d');
  const reduce = matchMedia('(prefers-reduced-motion: reduce)').matches;
  const dpr = Math.min(devicePixelRatio || 1, 2);
  const TAU = Math.PI * 2;
  let w = 0, h = 0;

  const rnd = (a, b) => a + Math.random() * (b - a);
  const makeStars = n => Array.from({ length: n }, () => ({ x: Math.random(), y: Math.random(), r: rnd(0.3, 1.4), p: rnd(0, TAU), s: rnd(0.5, 2) }));
  const stars = makeStars(120), sparse = makeStars(30);
  const streaks = Array.from({ length: 14 }, () => ({ x: Math.random(), y: Math.random(), len: rnd(80, 220), v: rnd(0.6, 1.6) }));
  const dust = Array.from({ length: 700 }, () => ({ arm: Math.floor(Math.random() * 2), r: Math.pow(Math.random(), 0.7), j: rnd(-0.35, 0.35) }));

  function drawStars(list, t, alpha = 1) {
    ctx.fillStyle = '#fff';
    for (const s of list) {
      ctx.globalAlpha = alpha * (0.45 + 0.55 * Math.sin(t * s.s + s.p));
      ctx.beginPath(); ctx.arc(s.x * w, s.y * h, s.r * dpr, 0, TAU); ctx.fill();
    }
    ctx.globalAlpha = 1;
  }
  function fillBg(c0, c1) {
    const g = ctx.createLinearGradient(0, 0, 0, h);
    g.addColorStop(0, c0); g.addColorStop(1, c1);
    ctx.fillStyle = g; ctx.fillRect(0, 0, w, h);
  }
  function blob(x, y, r, color) {
    const g = ctx.createRadialGradient(x, y, 0, x, y, r);
    g.addColorStop(0, color); g.addColorStop(1, 'rgba(0,0,0,0)');
    ctx.fillStyle = g; ctx.fillRect(0, 0, w, h);
  }

  const presets = {
    nebula(t) {
      fillBg('#0d0720', '#1a0b2e');
      const m = Math.min(w, h);
      blob(w * (0.3 + 0.1 * Math.sin(t * 0.2)), h * (0.35 + 0.08 * Math.cos(t * 0.17)), m * 0.9, 'rgba(168,85,247,.55)');
      blob(w * (0.7 + 0.1 * Math.cos(t * 0.15)), h * (0.6 + 0.08 * Math.sin(t * 0.21)), m * 0.8, 'rgba(236,72,153,.45)');
      drawStars(stars, t, 0.6);
    },
    starfield(t) { fillBg('#050a1f', '#0b1840'); drawStars(stars, t); },
    aurora(t) {
      fillBg('#03121a', '#06202b');
      const tints = ['rgba(45,212,191,.5)', 'rgba(74,222,128,.4)', 'rgba(56,189,248,.35)'];
      tints.forEach((tint, i) => {
        ctx.beginPath(); ctx.moveTo(0, h);
        for (let x = 0; x <= w; x += 16 * dpr) {
          ctx.lineTo(x, h * (0.35 + i * 0.08) + Math.sin((x * 0.004) / dpr + t * (0.4 + i * 0.15) + i) * h * 0.06);
        }
        ctx.lineTo(w, h); ctx.closePath();
        const g = ctx.createLinearGradient(0, h * 0.2, 0, h);
        g.addColorStop(0, tint); g.addColorStop(1, 'rgba(0,0,0,0)');
        ctx.fillStyle = g; ctx.fill();
      });
      drawStars(sparse, t, 0.5);
    },
    flare(t) {
      fillBg('#2a0a05', '#7c1d0b');
      blob(w * 0.5, h * 0.9, Math.max(w, h) * (0.8 + 0.06 * Math.sin(t * 0.8)), 'rgba(251,146,60,.8)');
      blob(w * 0.2, h * 0.2, Math.min(w, h) * 0.6, 'rgba(239,68,68,.35)');
    },
    eclipse(t) {
      ctx.fillStyle = '#030305'; ctx.fillRect(0, 0, w, h);
      const cx = w / 2, cy = h * 0.38, r = Math.min(w, h) * 0.28;
      const g = ctx.createRadialGradient(cx, cy, r * 0.92, cx, cy, r * 1.35);
      g.addColorStop(0, `rgba(220,38,38,${0.7 + 0.2 * Math.sin(t * 0.6)})`);
      g.addColorStop(1, 'rgba(0,0,0,0)');
      ctx.fillStyle = g; ctx.fillRect(0, 0, w, h);
      ctx.fillStyle = '#000'; ctx.beginPath(); ctx.arc(cx, cy, r, 0, TAU); ctx.fill();
      drawStars(sparse, t, 0.35);
    },
    galaxy(t) {
      ctx.fillStyle = '#04030c'; ctx.fillRect(0, 0, w, h);
      const cx = w / 2, cy = h * 0.42, R = Math.min(w, h) * 0.55;
      blob(cx, cy, R * 0.5, 'rgba(255,214,170,.55)');
      for (const d of dust) {
        const a = d.arm * Math.PI + d.r * 5 + d.j + t * 0.05;
        const x = cx + Math.cos(a) * d.r * R, y = cy + Math.sin(a) * d.r * R * 0.6;
        ctx.fillStyle = `hsla(${210 + d.r * 80},80%,${85 - d.r * 30}%,${0.9 - d.r * 0.5})`;
        ctx.fillRect(x, y, 1.6 * dpr, 1.6 * dpr);
      }
    },
    void(t) { ctx.fillStyle = '#000'; ctx.fillRect(0, 0, w, h); drawStars(sparse, t * 0.5, 0.7); },
    comet(t) {
      fillBg('#022c2e', '#0b5563');
      const span = w + 260 * dpr;
      for (const s of streaks) {
        const hx = ((s.x * w + t * s.v * 90 * dpr) % span), hy = s.y * h * 0.8 + hx * 0.3;
        const tx = hx - s.len * dpr, ty = hy - s.len * dpr * 0.3;
        const g = ctx.createLinearGradient(hx, hy, tx, ty);
        g.addColorStop(0, 'rgba(255,255,255,.9)'); g.addColorStop(1, 'rgba(94,234,212,0)');
        ctx.strokeStyle = g; ctx.lineWidth = 2 * dpr;
        ctx.beginPath(); ctx.moveTo(hx, hy); ctx.lineTo(tx, ty); ctx.stroke();
      }
    },
  };

  const draw = presets[id] || presets.starfield;
  function resize() {
    w = cv.width = Math.round(innerWidth * dpr);
    h = cv.height = Math.round(innerHeight * dpr);
    if (reduce) draw(0);
  }

  const FRAME_MS = 1000 / 30;
  let last = 0, raf = 0;
  function loop(ms) {
    raf = requestAnimationFrame(loop);
    if (ms - last < FRAME_MS) return;
    last = ms; draw(ms / 1000);
  }
  document.addEventListener('visibilitychange', () => {
    cancelAnimationFrame(raf);
    if (!document.hidden && !reduce) raf = requestAnimationFrame(loop);
  });
  addEventListener('resize', resize);
  resize();
  if (!reduce) raf = requestAnimationFrame(loop);
})();
