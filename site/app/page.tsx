import Image from "next/image";
import shotReady from "@/public/img/ios-ready.png";
import shotBrake from "@/public/img/ios-brake.png";
import shotDrive from "@/public/img/ios-drive.png";

const ANDROID = "https://github.com/nicglazkov/brake-bedding/releases/latest";
const IOS = "https://testflight.apple.com/join/KDtbBckw";
const GITHUB = "https://github.com/nicglazkov/brake-bedding";

const phases = [
  { name: "INCREASE SPEED", color: "var(--phase-up)", text: "Go to the start speed of the stage" },
  { name: "HOLD SPEED", color: "var(--phase-hold)", text: "Stay at the speed until the bar is empty" },
  { name: "BRAKE", color: "var(--phase-brake)", text: "Apply the brakes. Release at the target speed" },
  { name: "DRIVE", color: "var(--phase-drive)", text: "Cover the gap distance. The brakes become more cool" },
  { name: "COOLDOWN", color: "var(--phase-cool)", text: "Drive the last miles with minimum braking" },
];

const features = [
  {
    title: "One instruction at a time",
    text: "Each phase fills the screen with its own color and its own symbol. You can read it in a glance, from the edge of your vision.",
  },
  {
    title: "Spoken cues",
    text: "The app speaks each instruction, so your eyes stay on the road. Music becomes quieter for a cue, as for navigation.",
  },
  {
    title: "Runs in the background",
    text: "A call or a dark screen does not stop the run. On iOS, a Live Activity shows the instruction in the Dynamic Island. On Android, it stays in the notification.",
  },
  {
    title: "Your procedure",
    text: "Edit each stage: the stops, the speeds, the distances, and the brake force. Start from presets. Select mph or km/h.",
  },
  {
    title: "Private by construction",
    text: "The app has no network access. Your location becomes a speed on your device and goes no further. The source code is public.",
  },
  {
    title: "One codebase, two platforms",
    text: "The Android app and the iOS app come from one Kotlin Multiplatform codebase. The engine, the screens, and the texts are identical.",
  },
];

const faq = [
  {
    q: "Why does the app ask for my location?",
    a: "Your location is the speed source. The app calculates your speed from GPS and compares it with the targets in your procedure. Give the app precise location access; the approximate option cannot supply a usable speed.",
  },
  {
    q: "Does the run stop when the screen goes off?",
    a: "No. The run continues. The instruction stays in the notification on Android, and in a Live Activity on iOS. The spoken cues continue in the two conditions.",
  },
  {
    q: "Which procedure do I use for my pads?",
    a: "Use the instructions from your pad manufacturer if you have them; they have priority. The presets in the app are usual community procedures, and you can edit each value.",
  },
  {
    q: "What does brake bedding do?",
    a: "Bedding puts a thin, equal layer of pad material on the rotor surface and heats the pads in controlled cycles. Correct bedding gives brakes that are quiet and consistent.",
  },
];

const DownloadIcon = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M12 4v11m0 0-4.5-4.5M12 15l4.5-4.5M5 19.5h14" />
  </svg>
);
const AppleIcon = () => (
  <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
    <path d="M16.4 12.6c0-2.4 2-3.5 2-3.6-1.1-1.6-2.8-1.8-3.4-1.8-1.4-.2-2.8.8-3.5.8-.7 0-1.8-.8-3-.8-1.5 0-3 .9-3.8 2.3-1.6 2.8-.4 7 1.2 9.3.8 1.1 1.7 2.4 2.9 2.3 1.2 0 1.6-.7 3-.7s1.8.7 3 .7c1.3 0 2.1-1.1 2.8-2.3.9-1.3 1.3-2.6 1.3-2.6s-2.5-1-2.5-3.6zM14.1 5.6c.6-.8 1.1-1.8 1-2.9-.9 0-2.1.6-2.7 1.4-.6.7-1.1 1.8-1 2.8 1 .1 2.1-.5 2.7-1.3z" />
  </svg>
);

// A speedometer drawn once at build time: 0 to 120 over a 270 degree sweep.
function Gauge() {
  const cx = 200;
  const cy = 200;
  const angle = (v: number) => ((-135 + (270 * v) / 120 - 90) * Math.PI) / 180;
  const pt = (v: number, r: number) => [cx + r * Math.cos(angle(v)), cy + r * Math.sin(angle(v))];
  const ticks = Array.from({ length: 25 }, (_, i) => i * 5);
  return (
    <div className="gauge" aria-hidden="true">
      <svg viewBox="0 0 400 400">
        <circle className="face" cx={cx} cy={cy} r={176} />
        <circle className="bezel" cx={cx} cy={cy} r={184} />
        {ticks.map((v) => {
          const major = v % 20 === 0;
          const [x1, y1] = pt(v, major ? 140 : 150);
          const [x2, y2] = pt(v, 162);
          return <line key={v} className={major ? "tick tick--major" : "tick"} x1={x1} y1={y1} x2={x2} y2={y2} />;
        })}
        {ticks
          .filter((v) => v % 20 === 0)
          .map((v) => {
            const [x, y] = pt(v, 116);
            return (
              <text key={v} className="num" x={x} y={y}>
                {v}
              </text>
            );
          })}
        <text className="unit" x={cx} y={cy - 58}>MPH</text>
        <g className="needle">
          <line x1={cx} y1={cy + 22} x2={cx} y2={cy - 150} />
        </g>
        <circle className="hub" cx={cx} cy={cy} r={12} />
      </svg>
      <div className="phase">
        <span className="p-up">INCREASE SPEED</span>
        <span className="p-hold">HOLD SPEED</span>
        <span className="p-brake">BRAKE</span>
        <span className="p-drive">DRIVE</span>
      </div>
    </div>
  );
}

export default function Home() {
  return (
    <main>
      {/* Hero */}
      <section className="hero nd-hero" id="get">
        <div className="nd-streak" aria-hidden="true" />
        <div className="nd-wrap hero__grid">
          <div>
            <p className="nd-eyebrow">For Android and iOS</p>
            <h1>Bed in new brakes with clear, spoken instructions.</h1>
            <p className="nd-lede">
              New pads and rotors need a bedding procedure: a series of controlled
              stops at set speeds and distances. Brake Bedding reads your speed from
              GPS and gives you one instruction at a time: on the screen, in your
              ears, and through vibration.
            </p>
            <div className="nd-actions">
              <a className="nd-btn" href={ANDROID}>
                <DownloadIcon />
                Download for Android
              </a>
              <a className="nd-btn" href={IOS}>
                <AppleIcon />
                Join the iOS beta
              </a>
              <a className="nd-btn nd-btn--ghost" href={GITHUB}>
                Source code
              </a>
            </div>
            <p className="meta">NO NETWORK · NO ADS · NO TRACKING</p>
          </div>
          <Gauge />
        </div>
      </section>

      {/* The instrument idea */}
      <section className="nd-section">
        <div className="nd-wrap phases-grid">
          <div>
            <div className="nd-section__head">
              <p className="nd-eyebrow">The instrument</p>
              <h2>The full screen is the instruction.</h2>
              <p>
                No menus and no gauges during a run. One color, one command, one number.
                Each phase also has its own symbol, so you do not identify an instruction
                only by its color.
              </p>
            </div>
            <ol className="phases">
              {phases.map((p) => (
                <li key={p.name}>
                  <span
                    className={p.name === "DRIVE" ? "chip chip--drive" : "chip"}
                    style={{ background: p.color }}
                  >
                    {p.name}
                  </span>
                  <p>{p.text}</p>
                </li>
              ))}
            </ol>
          </div>
          <div className="shots">
            <Image src={shotReady} alt="The start screen. It shows the procedure and a Start button." />
            <Image src={shotBrake} alt="The brake instruction: a red screen with the speed to brake to." />
            <Image src={shotDrive} alt="The drive instruction between stops: the distance that remains." />
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="nd-section">
        <div className="nd-wrap">
          <div className="nd-section__head">
            <p className="nd-eyebrow">Functions</p>
            <h2>Made for the driver seat, not for the couch.</h2>
          </div>
          <ul className="features">
            {features.map((f) => (
              <li key={f.title}>
                <h3>{f.title}</h3>
                <p>{f.text}</p>
              </li>
            ))}
          </ul>

          {/* Safety */}
          <div className="callout">
            <strong>Safety.</strong> Do the procedure only on an empty and safe road, and
            obey all speed limits. The app is a guide: it cannot see the road, and the
            instructions from your pad manufacturer have priority. The full safety notes
            are in the app.
          </div>
        </div>
      </section>

      {/* FAQ */}
      <section className="nd-section">
        <div className="nd-wrap split">
          <div>
            <p className="nd-eyebrow">Questions</p>
            <h2>Common questions</h2>
          </div>
          <div className="faq">
            {faq.map((item, i) => (
              <details key={item.q} open={i === 0}>
                <summary>{item.q}</summary>
                <p>{item.a}</p>
              </details>
            ))}
          </div>
        </div>
      </section>

      {/* Final CTA */}
      <section className="closer nd-hero">
        <div className="nd-streak" aria-hidden="true" />
        <div className="nd-wrap">
          <h2>New brakes? Bed them in correctly.</h2>
          <div className="nd-actions">
            <a className="nd-btn" href={ANDROID}>
              <DownloadIcon />
              Download for Android
            </a>
            <a className="nd-btn" href={IOS}>
              <AppleIcon />
              Join the iOS beta
            </a>
          </div>
        </div>
      </section>
    </main>
  );
}
