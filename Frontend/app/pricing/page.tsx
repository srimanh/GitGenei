"use client"

import { motion } from "framer-motion"
import { Button } from "@/components/ui/button"
import Link from "next/link"

export default function PricingPage() {
  return (
    <main className="min-h-screen bg-[#0b0f14] text-white">
      <div className="mx-auto w-[min(1100px,92vw)] pt-28">
        <motion.h1
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-3xl md:text-4xl font-semibold text-white"
        >
          Pricing
        </motion.h1>
        <p className="mt-3 text-white/80">Launch special: Free for hackathons. Team plans coming soon.</p>
        <div className="mt-6">
          <Link href="/signup">
            <Button className="bg-gradient-to-r from-[#22d3ee] to-[#60a5fa] text-black">Start free</Button>
          </Link>
        </div>
      </div>
    </main>
  )
}
