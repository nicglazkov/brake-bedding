import type { Metadata, Viewport } from "next";
import Link from "next/link";
import Image from "next/image";
import localFont from "next/font/local";
import "./globals.css";
import { themeScript } from "./theme-script";
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

// theme-color is set by the theme script, so it follows the visitor's choice.
export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
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

// The sun/moon switch. The click is handled by the inline theme script, so the
// markup is identical on the server and the client.
function ThemeButton() {
  return (
    <button className="nd-theme" type="button" aria-label="Switch between light and dark theme" title="Light or dark theme">
      <svg className="nd-theme__moon" viewBox="0 0 24 24" aria-hidden="true">
        <path d="M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5z" />
      </svg>
      <svg className="nd-theme__sun" viewBox="0 0 24 24" aria-hidden="true">
        <circle cx="12" cy="12" r="4" />
        <path d="M12 2.5v2M12 19.5v2M4.6 4.6 6 6M18 18l1.4 1.4M2.5 12h2M19.5 12h2M4.6 19.4 6 18M18 6l1.4-1.4" />
      </svg>
    </button>
  );
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className={manrope.variable} suppressHydrationWarning>
      <head>
        <script dangerouslySetInnerHTML={{ __html: themeScript }} />
      </head>
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
              <ThemeButton />
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
