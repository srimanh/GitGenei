"use client"

import Image from "next/image"
import { motion } from "framer-motion"
import { useParallax } from "@/hooks/use-parallax"
import { cn } from "@/lib/utils"
import { useEffect, useRef, useState } from "react"

type Props = {
  title?: string
  code?: string
  imageSrc?: string
  className?: string
  delay?: number
  typewriter?: boolean
}

export function FloatingCodeWindow({
  title = "main.ts",
  code = "",
  imageSrc,
  className,
  delay = 0,
  typewriter = false,
}: Props) {
  const ref = useParallax(10)
  const [chars, setChars] = useState(typewriter ? 0 : code.length)
  const codeRef = useRef(code)

  useEffect(() => {
    codeRef.current = code
    if (!typewriter) {
      setChars(code.length)
      return
    }
    setChars(0)
    let i = 0
    const id = setInterval(() => {
      i += Math.max(1, Math.floor(codeRef.current.length / 120)) // speed adapts to length
      setChars(Math.min(i, codeRef.current.length))
      if (i >= codeRef.current.length) clearInterval(id)
    }, 20)
    return () => clearInterval(id)
  }, [code, typewriter])

  return (
    <motion.div
      ref={ref as any}
      initial={{ opacity: 0, y: 40, scale: 0.98 }}
      whileInView={{ opacity: 1, y: 0, scale: 1 }}
      viewport={{ once: true, amount: 0.3 }}
      transition={{ duration: 0.8, delay }}
      className={cn(
        "rounded-xl border border-white/10 bg-white/5 backdrop-blur-lg shadow-[0_0_40px_-10px_rgba(34,211,238,0.35)]",
        "relative overflow-hidden",
        className,
      )}
      style={{ willChange: "transform" }}
    >
      {/* title bar */}
      <div className="flex items-center justify-between px-3 py-2 border-b border-white/10 bg-white/5">
        <div className="flex items-center gap-1.5">
          <span className="h-2.5 w-2.5 rounded-full bg-[#f472b6]/90"></span>
          <span className="h-2.5 w-2.5 rounded-full bg-[#22d3ee]/90"></span>
          <span className="h-2.5 w-2.5 rounded-full bg-[#60a5fa]/90"></span>
        </div>
        <span className="text-[11px] text-white/80">{title}</span>
        <div className="w-6" />
      </div>

      <div className="relative">
        {imageSrc ? (
          <Image
            src={imageSrc || "/placeholder.svg"}
            alt="Reference window screenshot"
            width={1200}
            height={800}
            className="w-full h-auto object-cover"
            priority={false}
          />
        ) : (
          <pre className="p-4 text-[12px] leading-6 text-white/90">
            <code>{code.slice(0, chars)}</code>
            {typewriter && chars < code.length ? <span className="opacity-80">▋</span> : null}
          </pre>
        )}
      </div>

      {/* cyan/blue glow */}
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_20%_10%,rgba(34,211,238,0.18),transparent_45%),radial-gradient(circle_at_80%_90%,rgba(96,165,250,0.18),transparent_35%)]" />
    </motion.div>
  )
}
