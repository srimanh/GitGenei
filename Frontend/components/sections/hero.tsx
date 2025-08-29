"use client"

import { motion } from "framer-motion"
import Link from "next/link"
import { Button } from "@/components/ui/button"
import { FloatingCodeWindow } from "@/components/floating-code"

export function Hero() {
  return (
    <section className="relative pt-36 md:pt-40">
      {/* glow blobs */}
      <div
        aria-hidden
        className="pointer-events-none absolute -top-48 -left-32 h-[60vmax] w-[60vmax] rounded-full bg-[radial-gradient(circle,rgba(34,211,238,0.14)_0%,transparent_60%)] blur-3xl"
      />
      <div
        aria-hidden
        className="pointer-events-none absolute top-10 right-0 h-[50vmax] w-[50vmax] rounded-full bg-[radial-gradient(circle,rgba(96,165,250,0.12)_0%,transparent_60%)] blur-3xl"
      />

      <div className="mx-auto w-[min(1100px,92vw)] text-center">
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8 }}
          className="inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/5 px-4 py-2 text-xs text-white/80"
        >
          Better Context. Better AI. Better Code
        </motion.div>

        <motion.h1
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.9, delay: 0.1 }}
          className="mt-6 text-pretty text-4xl md:text-6xl font-semibold tracking-tight text-white"
        >
          <span className="inline-block shimmer-float bg-gradient-to-r from-white via-white to-white/70 bg-clip-text text-transparent">
            Perfect Git.
          </span>{" "}
          <span className="inline-block shimmer-float-delay bg-gradient-to-r from-[#22d3ee] via-white to-[#60a5fa] bg-clip-text text-transparent">
            Instant Deploy.
          </span>{" "}
          <span className="inline-block shimmer-float-2 bg-gradient-to-r from-white via-[#f472b6] to-white/70 bg-clip-text text-transparent">
            Zero Panic.
          </span>
        </motion.h1>

        <motion.p
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.9, delay: 0.2 }}
          className="mx-auto mt-4 max-w-2xl text-white/80 text-balance"
        >
          Upload your entire project and GitGenei auto‑organizes branches, commits, merges, and pushes to GitHub. One
          click deploys to the cloud. Built for hackathons—useful for every team.
        </motion.p>

        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.9, delay: 0.3 }}
          className="mt-7 flex items-center justify-center gap-3"
        >
          <Link href="/signup">
            <Button className="px-6 py-6 text-base font-semibold bg-gradient-to-r from-[#22d3ee] to-[#60a5fa] text-black hover:from-[#22d3ee] hover:to-[#60a5fa]/90">
              Start free — Deploy now
            </Button>
          </Link>
          <Link href="#problem" className="text-white/80 hover:text-white text-sm">
            See how it works →
          </Link>
        </motion.div>

        {/* Floating code windows */}
        <div className="relative mt-12 grid grid-cols-1 md:grid-cols-2 gap-5">
          <FloatingCodeWindow
            title="organize.ts"
            delay={0.1}
            typewriter
            code={`const upload = await api.upload.zip(file)
const plan = await ai.analyze(upload).splitBy('function','folder')
await git.init()
for (const branch of plan.branches) {
  await git.checkout(branch.name)
  await git.commit(branch.changes, branch.message)
}
await git.merge.plan(plan)
await git.push('origin')
await cloud.deploy({ project: upload.name })`}
          />
          <FloatingCodeWindow
            title="deploy.ts"
            delay={0.2}
            code={`import { deploy } from 'gitgenei'
const deployment = await deploy.toCloud()
console.log('✅ Deployed to cloud!')`}
          />
        </div>
      </div>
    </section>
  )
}
