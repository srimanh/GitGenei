"use client"

import { AnimatedBackground } from "@/components/animated-background"
import { Navbar } from "@/components/navbar"
import { Hero } from "@/components/sections/hero"
import { ProblemSolution } from "@/components/sections/problem-solution"
import { Features } from "@/components/sections/features"
import { HowItWorks } from "@/components/sections/how-it-works"
import { CTA } from "@/components/sections/cta"
import { Footer } from "@/components/footer"

export default function Page() {
  return (
    <main className="relative min-h-screen bg-[#0b0f14] text-white">
      <AnimatedBackground />
      <Navbar />
      <Hero />
      <ProblemSolution />
      <Features />
      <HowItWorks />
      <CTA />
      <Footer />
    </main>
  )
}
