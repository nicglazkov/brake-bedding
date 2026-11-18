import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Support: Brake Bedding",
  description: "Help and support for the Brake Bedding app.",
};

const faq = [
  {
    q: "The app asks for my location. Why?",
    a: "Your location is the speed source. The app calculates your speed from GPS and compares it with the targets in your procedure. Give the app precise location access; the approximate option cannot supply a usable speed.",
  },
  {
    q: 'The screen shows "There is no GPS signal yet."',
    a: "GPS needs a view of the sky. The signal usually arrives in some seconds outdoors. In a garage or a tunnel there is no signal, and the run waits until the signal comes back.",
  },
  {
    q: "Does the run stop when the screen goes off or a call comes in?",
    a: "No. The run continues. On Android, the instruction stays in the notification. On iOS, it stays in a Live Activity on the Lock Screen and in the Dynamic Island.",
  },
  {
    q: "Which procedure do I use for my pads?",
    a: "Use the instructions from your pad manufacturer if you have them; they have priority. The presets in the app are usual community procedures, and you can edit each value in the procedure editor.",
  },
];

export default function Support() {
  return (
    <main className="doc nd-hero">
      <div className="nd-wrap">
      <p className="nd-eyebrow">Support</p>
      <h1>Support</h1>

      <div className="callout">
        <p>
          <strong>To get help:</strong> open an issue on{" "}
          <a href="https://github.com/nicglazkov/brake-bedding/issues">
            GitHub
          </a>
          . On iOS, you can also send feedback directly from TestFlight.
        </p>
      </div>

      <h2>Common questions</h2>
      <div>
        {faq.map((item) => (
          <div key={item.q}>
            <h3>{item.q}</h3>
            <p>{item.a}</p>
          </div>
        ))}
      </div>

      <h2>How do I install the app?</h2>
      <p>
        Android: download the APK from the{" "}
        <a href="https://github.com/nicglazkov/brake-bedding/releases/latest">
          latest release
        </a>
        . iOS: join through{" "}
        <a href="https://testflight.apple.com/join/KDtbBckw">
          TestFlight
        </a>
        .
      </p>

      <h2>Feedback and defects</h2>
      <p>
        A clear report helps: the device, the app version (in Settings → About),
        what you did, and what the app did. On iOS, you can also send feedback with
        a screenshot directly from TestFlight.
      </p>
      </div>
    </main>
  );
}
