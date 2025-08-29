"use client"

import { motion } from "framer-motion"
import Link from "next/link"

export default function DocsPage() {
  return (
    <main className="min-h-screen bg-[#0b0f14] text-white">
      <div className="mx-auto w-[min(1100px,92vw)] pt-28">
        <motion.h1
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-3xl md:text-4xl font-semibold text-white"
        >
          Docs
        </motion.h1>
        <p className="mt-3 text-white/80">API and CLI docs coming soon.</p>
        <Link href="/" className="text-[#22d3ee] hover:text-[#22d3ee]/80 mt-6 inline-block">
          ← Back home
        </Link>
      </div>
    </main>
  )
}
