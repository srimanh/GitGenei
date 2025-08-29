"use client"

import { motion } from "framer-motion"
import Link from "next/link"

export default function ProductPage() {
  return (
    <main className="min-h-screen bg-[#0b0f14] text-white">
      <div className="mx-auto w-[min(1100px,92vw)] pt-28">
        <motion.h1
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-3xl md:text-4xl font-semibold text-white"
        >
          Product
        </motion.h1>
        <motion.p
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.1 }}
          className="mt-3 text-white/80"
        >
          Detailed docs coming soon. Explore features on the homepage.
        </motion.p>
        <Link href="/" className="text-[#22d3ee] hover:text-[#22d3ee]/80 mt-6 inline-block">
          ← Back home
        </Link>
      </div>
    </main>
  )
}
