"use client"

import { motion } from "framer-motion"
import Link from "next/link"
import { Button } from "@/components/ui/button"

export function CTA() {
  return (
    <section className="mx-auto w-[min(1100px,92vw)] my-24">
      <motion.div
        initial={{ opacity: 0, scale: 0.98 }}
        whileInView={{ opacity: 1, scale: 1 }}
        viewport={{ once: true, amount: 0.4 }}
        transition={{ duration: 0.7 }}
        className="relative overflow-hidden rounded-2xl border border-white/10 p-8 md:p-10"
        style={{ background: "linear-gradient(135deg, rgba(34,211,238,0.15), rgba(96,165,250,0.15))" }}
      >
        <div aria-hidden className="absolute -right-24 -top-24 h-72 w-72 rounded-full bg-[#f472b6]/30 blur-[80px]" />
        <h3 className="text-2xl md:text-3xl font-semibold text-white">Ready to never fear the final push again?</h3>
        <p className="mt-2 text-white/80 max-w-2xl">Join thousands of devs shipping clean repos and instant deploys.</p>
        <div className="mt-6 flex flex-wrap items-center gap-3">
          <Link href="/signup">
            <Button className="px-6 py-6 text-base font-semibold bg-gradient-to-r from-[#22d3ee] to-[#60a5fa] text-black hover:from-[#22d3ee] hover:to-[#60a5fa]/90">
              Create free account
            </Button>
          </Link>
          <Link href="/product" className="text-white/80 hover:text-white text-sm">
            Explore features →
          </Link>
        </div>
      </motion.div>
    </section>
  )
}
