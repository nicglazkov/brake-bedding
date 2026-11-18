import type { Metadata, Viewport } from "next";
import Link from "next/link";
import Image from "next/image";
import localFont from "next/font/local";
import "./globals.css";
import icon from "@/public/icon.png";

// Manrope, self-hosted (latin subset, variable 400-800, SIL OFL 1.1).
const manrope = localFont({
  src: "./fonts/manrope-latin.woff2",
  weight: "400 800",
  variable: "--font-manrope",
  display: "swap",
});

export const metadata: Metadata = {
  title: "Brake Bedding: a brake bedding coach for Android and iOS",
  description:
    "Brake Bedding helps you bed in new brake pads and rotors. It reads your speed from GPS and speaks each instruction. No network, no ads, no tracking.",
  icons: { icon: "/brake-bedding/favicon.png" },
};

export const viewport: Viewport = {
  themeColor: "#0a0d16",
  colorScheme: "dark",
};

const GITHUB = "https://github.com/nicglazkov/brake-bedding";

// The shared "More from Nic" strip, identical on every Night Drive site.
const more = [
  { name: "CommuteScout", href: "https://commutescout.com", text: "Live road conditions and the best time to leave" },
  { name: "ChainCheck", href: "https://nicglazkov.github.io/chaincheck/", text: "Sierra chain controls and pass weather" },
  { name: "SABRE Plus", href: "https://nicglazkov.github.io/highway-radar-sabre-plus/", text: "California road alerts for Highway Radar" },
  { name: "OutageWatch", href: "https://nicglazkov.github.io/outagewatch/", text: "PG&E power outage alerts" },
  { name: "FiberWatch", href: "https://getfiberwatch.com", text: "Know when fiber reaches your address" },
  { name: "Switchboard", href: "https://nicglazkov.github.io/switchboard/", text: "Claude Code chats on all your computers" },
  { name: "Chalkboard", href: "https://chalkboard.studio", text: "Turn a topic into a narrated explainer video" },
];

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className={manrope.variable}>
      <body>
        <header className="nd-nav">
          <div className="nd-wrap nd-nav__in">
            <Link href="/" className="nd-brand">
              <Image src={icon} alt="" width={28} height={28} />
              Brake Bedding
            </Link>
            <nav className="nd-links" aria-label="Primary">
              <Link href="/support" className="nd-hide">Support</Link>
              <Link href="/privacy" className="nd-hide">Privacy</Link>
              <a href={GITHUB} className="nd-hide">GitHub</a>
              <Link href="/#get" className="nd-btn nd-btn--sm">Get the app</Link>
            </nav>
          </div>
        </header>
        {children}
        <footer className="nd-foot">
          <div className="nd-wrap">
            <div className="nd-foot__top">
              <Link href="/" className="nd-brand">
                <Image src={icon} alt="" width={28} height={28} />
                Brake Bedding
              </Link>
              <nav className="nd-foot__links" aria-label="Project">
                <Link href="/privacy">Privacy policy</Link>
                <Link href="/support">Support</Link>
                <a href={GITHUB}>Source code</a>
              </nav>
            </div>
            <div className="nd-more">
              <div className="nd-more__head">
                <p className="nd-eyebrow">More from Nic</p>
                <a className="nd-more__all" href="https://nicglazkov.github.io/">All projects &rarr;</a>
              </div>
              <ul>
                {more.map((m) => (
                  <li key={m.name}>
                    <a className="nd-more__item" href={m.href}>
                      <b>{m.name}</b>
                      <span>{m.text}</span>
                    </a>
                  </li>
                ))}
              </ul>
            </div>
            <div className="nd-foot__base">
              <span>Apache 2.0 · © 2026 Nic Glazkov</span>
            </div>
          </div>
        </footer>
      </body>
    </html>
  );
}
