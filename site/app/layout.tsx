import type { Metadata } from "next";
import Link from "next/link";
import Image from "next/image";
import "./globals.css";
import icon from "@/public/icon.png";

export const metadata: Metadata = {
  title: "Brake Bedding — a brake bedding coach for Android and iOS",
  description:
    "Brake Bedding helps you bed in new brake pads and rotors. It reads your speed from GPS and speaks each instruction. No network, no ads, no tracking.",
  icons: { icon: "/brake-bedding/favicon.png" },
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body className="min-h-dvh antialiased">
        <header className="mx-auto flex max-w-5xl items-center justify-between px-5 py-6">
          <Link href="/" className="flex items-center gap-3">
            <Image src={icon} alt="The Brake Bedding icon" width={40} height={40} className="rounded-[10px]" />
            <span className="text-lg font-extrabold tracking-tight">Brake Bedding</span>
          </Link>
          <nav className="flex items-center gap-5 text-sm text-muted-fg">
            <Link href="/support" className="hover:text-ember">Support</Link>
            <Link href="/privacy" className="hover:text-ember">Privacy</Link>
            <a href="https://github.com/nicglazkov/brake-bedding" className="hover:text-ember">GitHub</a>
          </nav>
        </header>
        {children}
        <footer className="mx-auto mt-20 max-w-5xl border-t border-line px-5 py-10 text-sm text-muted-fg">
          <div className="flex flex-wrap items-center gap-x-6 gap-y-2">
            <Link href="/privacy" className="hover:text-ember">Privacy policy</Link>
            <Link href="/support" className="hover:text-ember">Support</Link>
            <a href="https://github.com/nicglazkov/brake-bedding" className="hover:text-ember">Source code</a>
            <span className="ml-auto">Apache 2.0 · © 2026 Nic Glazkov</span>
          </div>
        </footer>
      </body>
    </html>
  );
}
