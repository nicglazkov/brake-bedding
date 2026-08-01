import Image from "next/image";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";
import shotReady from "@/public/img/ios-ready.png";
import shotBrake from "@/public/img/ios-brake.png";
import shotDrive from "@/public/img/ios-drive.png";

const phases = [
  { name: "INCREASE SPEED", color: "bg-phase-up", text: "Go to the start speed of the stage" },
  { name: "HOLD SPEED", color: "bg-phase-hold", text: "Stay at the speed until the bar is empty" },
  { name: "BRAKE", color: "bg-phase-brake", text: "Apply the brakes. Release at the target speed" },
  { name: "DRIVE", color: "bg-phase-drive", text: "Cover the gap distance. The brakes become more cool" },
  { name: "COOLDOWN", color: "bg-phase-cool", text: "Drive the last miles with minimum braking" },
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
    a: "Use the instructions from your pad manufacturer if you have them — they have priority. The presets in the app are usual community procedures, and you can edit each value.",
  },
  {
    q: "What does brake bedding do?",
    a: "Bedding puts a thin, equal layer of pad material on the rotor surface and heats the pads in controlled cycles. Correct bedding gives brakes that are quiet and consistent.",
  },
];

export default function Home() {
  return (
    <main className="mx-auto max-w-5xl px-5">
      {/* Hero */}
      <section className="grid items-center gap-10 py-10 md:grid-cols-2 md:py-16">
        <div>
          <div className="label mb-4">For Android and iOS</div>
          <h1 className="text-4xl font-extrabold leading-tight tracking-tight md:text-5xl">
            Bed in new brakes with clear, spoken instructions.
          </h1>
          <p className="mt-5 text-lg text-muted-fg">
            New pads and rotors need a bedding procedure: a series of controlled
            stops at set speeds and distances. Brake Bedding reads your speed from
            GPS and gives you one instruction at a time — on the screen, in your
            ears, and through vibration.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Button asChild size="lg" className="bg-ember text-white hover:bg-ember-deep">
              <a href="https://github.com/nicglazkov/brake-bedding/releases/latest">
                Download for Android
              </a>
            </Button>
            <Button asChild size="lg" className="bg-ember text-white hover:bg-ember-deep">
              <a href="https://testflight.apple.com/join/KDtbBckw">Join the iOS beta</a>
            </Button>
            <Button asChild size="lg" variant="outline" className="border-line">
              <a href="https://github.com/nicglazkov/brake-bedding">Source code</a>
            </Button>
          </div>
          <p className="label mt-6">No network · No ads · No tracking</p>
        </div>
        <div className="grid grid-cols-3 gap-3">
          <Image src={shotReady} alt="The start screen. It shows the procedure and a Start button." className="rounded-2xl border border-line" />
          <Image src={shotBrake} alt="The brake instruction: a red screen with the speed to brake to." className="rounded-2xl border border-line" />
          <Image src={shotDrive} alt="The drive instruction between stops: the distance that remains." className="rounded-2xl border border-line" />
        </div>
      </section>

      {/* The instrument idea */}
      <section className="py-12">
        <div className="label mb-2">The instrument</div>
        <h2 className="text-2xl font-bold tracking-tight md:text-3xl">
          The full screen is the instruction.
        </h2>
        <p className="mt-3 max-w-2xl text-muted-fg">
          No menus and no gauges during a run. One color, one command, one number.
          Each phase also has its own symbol, so you do not identify an instruction
          only by its color.
        </p>
        <div className="mt-8 grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
          {phases.map((p) => (
            <div key={p.name} className={`rounded-2xl p-4 text-white ${p.color}`}>
              <div className="font-mono text-xs font-semibold tracking-widest">{p.name}</div>
              <div className="mt-2 text-sm opacity-90">{p.text}</div>
            </div>
          ))}
        </div>
      </section>

      {/* Features */}
      <section className="py-12">
        <div className="label mb-2">Functions</div>
        <h2 className="text-2xl font-bold tracking-tight md:text-3xl">
          Made for the driver seat, not for the couch.
        </h2>
        <div className="mt-8 grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {features.map((f) => (
            <Card key={f.title} className="border-line bg-surface">
              <CardContent className="pt-6">
                <h3 className="font-bold">{f.title}</h3>
                <p className="mt-2 text-sm text-muted-fg">{f.text}</p>
              </CardContent>
            </Card>
          ))}
        </div>
      </section>

      {/* Safety */}
      <section className="py-12">
        <Card className="border-line bg-surface">
          <CardContent className="pt-6">
            <div className="label mb-2">Safety</div>
            <p className="text-muted-fg">
              Do the procedure only on an empty and safe road, and obey all speed
              limits. The app is a guide: it cannot see the road, and the
              instructions from your pad manufacturer have priority. The full
              safety notes are in the app.
            </p>
          </CardContent>
        </Card>
      </section>

      {/* FAQ */}
      <section className="py-12">
        <div className="label mb-2">Questions</div>
        <h2 className="text-2xl font-bold tracking-tight md:text-3xl">Common questions</h2>
        <Accordion type="single" collapsible className="mt-6">
          {faq.map((item) => (
            <AccordionItem key={item.q} value={item.q}>
              <AccordionTrigger className="text-left">{item.q}</AccordionTrigger>
              <AccordionContent className="text-muted-fg">{item.a}</AccordionContent>
            </AccordionItem>
          ))}
        </Accordion>
      </section>

      {/* Final CTA */}
      <section className="py-12 text-center">
        <h2 className="text-2xl font-bold tracking-tight md:text-3xl">
          New brakes? Bed them in correctly.
        </h2>
        <div className="mt-6 flex flex-wrap justify-center gap-3">
          <Button asChild size="lg" className="bg-ember text-white hover:bg-ember-deep">
            <a href="https://github.com/nicglazkov/brake-bedding/releases/latest">
              Download for Android
            </a>
          </Button>
          <Button asChild size="lg" className="bg-ember text-white hover:bg-ember-deep">
            <a href="https://testflight.apple.com/join/KDtbBckw">Join the iOS beta</a>
          </Button>
        </div>
      </section>
    </main>
  );
}
