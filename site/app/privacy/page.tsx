import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Privacy policy — Brake Bedding",
  description: "The privacy policy of the Brake Bedding app. The app collects no data.",
};

export default function Privacy() {
  return (
    <main className="mx-auto max-w-3xl px-5">
      <div className="label mb-2 mt-4">Privacy policy</div>
      <h1 className="text-3xl font-extrabold tracking-tight">Privacy policy</h1>
      <p className="mt-2 text-sm text-muted-fg">Effective date: July 31, 2026</p>

      <div className="mt-6 rounded-2xl border border-line bg-surface p-5">
        <p>
          <strong>The short version: the app collects no data.</strong> It has no
          network access, no accounts, no analytics, and no advertising. Nothing
          leaves your device.
        </p>
      </div>

      <h2 className="mt-10 text-xl font-bold">Location</h2>
      <p className="mt-2 text-muted-fg">
        The app reads your location from the GPS receiver in your device. It uses
        the location for one purpose: the calculation of your current speed, which
        drives the on-screen instructions. The location data stays in the memory of
        the app and goes away when the run ends. The app does not store your
        location, does not build a route history, and cannot transmit anything,
        because it has no permission to use the network.
      </p>

      <h2 className="mt-8 text-xl font-bold">Data that stays on your device</h2>
      <p className="mt-2 text-muted-fg">
        The app keeps your procedure and your settings (for example, mph or km/h)
        in the private storage of the app on your device. When you delete the app,
        the operating system deletes this data.
      </p>

      <h2 className="mt-8 text-xl font-bold">Data that the app does not collect</h2>
      <ul className="mt-2 list-disc pl-6 text-muted-fg">
        <li>No personal information, accounts, or identifiers</li>
        <li>No analytics or usage statistics</li>
        <li>No advertising identifiers</li>
        <li>No crash reports from inside the app</li>
      </ul>
      <p className="mt-3 text-muted-fg">
        If you install the iOS app through TestFlight, Apple can collect crash data
        and feedback that you agree to send. That collection follows{" "}
        <a className="text-ember" href="https://www.apple.com/legal/privacy/">
          the privacy policy of Apple
        </a>
        , not this one. The same applies to the store that installs the Android
        app, if you use one.
      </p>

      <h2 className="mt-8 text-xl font-bold">Changes</h2>
      <p className="mt-2 text-muted-fg">
        If a future version of the app changes this policy, the new policy will
        appear on this page before that version ships, with a new effective date.
        The{" "}
        <a className="text-ember" href="https://github.com/nicglazkov/brake-bedding">
          source code
        </a>{" "}
        is public, so you can verify each claim on this page.
      </p>

      <h2 className="mt-8 text-xl font-bold">Contact</h2>
      <p className="mt-2 text-muted-fg">
        Questions about this policy: open an issue on{" "}
        <a className="text-ember" href="https://github.com/nicglazkov/brake-bedding/issues">
          GitHub
        </a>
        .
      </p>
    </main>
  );
}
