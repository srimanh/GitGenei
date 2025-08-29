"use client"

import type React from "react"

import { motion } from "framer-motion"
import { cn } from "@/lib/utils"

type FeatureCardProps = {
  title: string
  desc: string
  icon?: React.ReactNode
  delay?: number
  className?: string
}

export function FeatureCard({ title, desc, icon, delay = 0, className }: FeatureCardProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 24 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, amount: 0.3 }}
      transition={{ duration: 0.7, delay }}
      whileHover={{ y: -6, scale: 1.02 }}
      className={cn(
        "group rounded-xl border border-white/10 bg-white/5 backdrop-blur-md p-5",
        "shadow-[0_0_40px_-15px_rgba(96,165,250,0.35)]",
        "hover:shadow-[0_0_50px_-10px_rgba(244,114,182,0.35)] transition-all",
        className,
      )}
    >
      <div className="flex items-start gap-3">
        <div className="h-9 w-9 rounded-lg bg-gradient-to-br from-[#22d3ee] to-[#60a5fa] ring-1 ring-[#22d3ee]/40" />
        <div>
          <h3 className="text-white font-semibold">{title}</h3>
          <p className="text-sm text-white/80 mt-1">{desc}</p>
        </div>
      </div>
    </motion.div>
  )
}
