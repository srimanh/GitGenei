"use client"

import { useEffect, useRef } from "react"

// Matrix-style letter rain with soft cyan/blue/pink glow orbs
export function AnimatedBackground() {
  const canvasRef = useRef<HTMLCanvasElement | null>(null)
  const rafRef = useRef<number | null>(null)

  useEffect(() => {
    const canvas = canvasRef.current!
    const ctx = canvas.getContext("2d")!
    let width = (canvas.width = window.innerWidth)
    let height = (canvas.height = window.innerHeight)
    const letters = "01ABCDEFGHJKLMNOPQRSTUVWXYZ{}[]()<>=+-*/$".split("")
    const fontSize = 14
    let columns = Math.floor(width / fontSize)
    let drops = Array.from({ length: columns }, () => Math.random() * height)

    const resize = () => {
      width = canvas.width = window.innerWidth
      height = canvas.height = window.innerHeight
      columns = Math.floor(width / fontSize)
      drops = Array.from({ length: columns }, () => Math.random() * height)
    }
    window.addEventListener("resize", resize)

    const draw = () => {
      // translucent clear for trails
      ctx.fillStyle = "rgba(11,15,20,0.22)" // near-black bg with slight alpha
      ctx.fillRect(0, 0, width, height)

      // soft glow orbs
      for (let i = 0; i < 3; i++) {
        const x = (Math.sin(Date.now() / (3000 + i * 700)) * 0.5 + 0.5) * width
        const y = (Math.cos(Date.now() / (3800 + i * 500)) * 0.5 + 0.5) * height
        const grad = ctx.createRadialGradient(x, y, 0, x, y, 260 + i * 80)
        grad.addColorStop(0, "rgba(34,211,238,0.18)") // cyan
        grad.addColorStop(0.7, "rgba(96,165,250,0.10)") // blue
        grad.addColorStop(1, "rgba(244,114,182,0.06)") // pink
        ctx.fillStyle = grad
        ctx.beginPath()
        ctx.arc(x, y, 280 + i * 62, 0, Math.PI * 2)
        ctx.fill()
      }

      ctx.font = `${fontSize}px ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace`
      ctx.fillStyle = "rgba(255,255,255,0.85)"
      for (let i = 0; i < drops.length; i++) {
        const text = letters[Math.floor(Math.random() * letters.length)]
        const x = i * fontSize
        const y = drops[i] * fontSize
        ctx.fillText(text, x, y)

        // cyan highlight on some columns
        if (i % 22 === 0) {
          ctx.fillStyle = "rgba(34,211,238,0.9)"
          ctx.fillText(text, x, y)
          ctx.fillStyle = "rgba(255,255,255,0.85)"
        }

        if (y > height && Math.random() > 0.975) drops[i] = 0
        drops[i]++
      }

      rafRef.current = requestAnimationFrame(draw)
    }

    draw()
    return () => {
      if (rafRef.current) cancelAnimationFrame(rafRef.current)
      window.removeEventListener("resize", resize)
    }
  }, [])

  return (
    <div aria-hidden className="absolute inset-0 -z-10 overflow-hidden">
      <canvas ref={canvasRef} className="h-full w-full" />
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(transparent_40%,rgba(0,0,0,0.55)_100%)]" />
    </div>
  )
}
