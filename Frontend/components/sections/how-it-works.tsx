"use client"

import { motion } from "framer-motion"
import { useState } from "react"
import { FloatingCodeWindow } from "@/components/floating-code"

const CODES = {
  upload: `const file = await pick.zip()\nconst upload = await api.upload.zip(file)\nconsole.log('📦 Uploaded:', upload.name)`,
  analyze: `const plan = await ai.plan(repo)\nplan.summary()\n// → branches: api, ui, infra, scripts, docs, tests, assets`,
  push: `await git.remote.add('origin', repoUrl)\nawait git.push('origin', { all: true })\nconsole.log('🚀 All branches pushed!')`,
  deploy: `import { deploy } from 'cloud'\nawait deploy({ provider: 'vercel', project: repo.name })\nconsole.log('✅ Deployed!')`,
} as const

export function HowItWorks() {
  const [active, setActive] = useState<keyof typeof CODES>("upload")

  return (
    <section className="mx-auto w-[min(1100px,92vw)] mt-28">
      <h2 className="text-center text-3xl md:text-4xl font-semibold text-white">How it works</h2>
      <p className="text-center text-white/80 mt-2">Upload → Organize → Push → Deploy</p>

      <div className="mt-8 grid md:grid-cols-12 gap-6">
        <div className="md:col-span-6">
          <div className="md:sticky md:top-28">
            <FloatingCodeWindow title="demo.ts" code={CODES[active]} delay={0.05} />
          </div>
        </div>

        <div className="md:col-span-6 space-y-6">
          {[
            { key: "upload", title: "Upload", text: "Drop a zip or paste a repo URL. We chunk and verify integrity." },
            { key: "analyze", title: "Analyze", text: "AI proposes branches, commit messages, and a safe merge plan." },
            { key: "push", title: "Push", text: "Connect GitHub and push all branches with a pristine history." },
            { key: "deploy", title: "Deploy", text: "Click once to deploy to the cloud. Roll back safely anytime." },
          ].map((s, i) => (
            <motion.div
              key={s.key}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: false, amount: 0.5 }}
              onViewportEnter={() => setActive(s.key as keyof typeof CODES)}
              transition={{ duration: 0.6, delay: i * 0.05 }}
              className="rounded-2xl border border-white/10 bg-white/5 p-6 backdrop-blur"
            >
              <h3 className="text-xl font-semibold text-white">
                {i + 1}. {s.title}
              </h3>
              <p className="mt-2 text-white/80">{s.text}</p>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  )
}
