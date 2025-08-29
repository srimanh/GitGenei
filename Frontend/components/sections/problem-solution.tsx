"use client"

import { motion } from "framer-motion"

export function ProblemSolution() {
  return (
    <section id="problem" className="relative mx-auto w-[min(1100px,92vw)] mt-28">
      <div className="grid md:grid-cols-12 gap-8">
        <div className="md:col-span-5">
          <div className="md:sticky md:top-28 md:h-[calc(100vh-7rem)] flex items-start md:items-center">
            <div className="w-full rounded-2xl border border-white/10 bg-white/5 p-6 backdrop-blur">
              <h2 className="text-3xl font-semibold text-white">Why GitGenei</h2>
              <p className="mt-2 text-white/75">Scroll the story: chaos → clarity → click deploy.</p>
            </div>
          </div>
        </div>

        <div className="md:col-span-7 space-y-6">
          <motion.div
            initial={{ opacity: 0, y: 24 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, amount: 0.4 }}
            transition={{ duration: 0.7 }}
            className="rounded-2xl border border-white/10 bg-white/5 p-6 backdrop-blur"
          >
            <span className="text-xs text-white/60">Slide 1</span>
            <h3 className="text-2xl font-semibold text-white mt-1">Problem</h3>
            <p className="mt-3 text-white/80">
              Final hours mean crammed code, dirty repos, conflicts, lost files, and failed uploads—teams lose points
              for git issues, not skill.
            </p>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 24 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, amount: 0.4 }}
            transition={{ duration: 0.7, delay: 0.05 }}
            className="rounded-2xl border border-white/10 bg-white/5 p-6 backdrop-blur"
            style={{ background: "linear-gradient(135deg, rgba(34,211,238,0.10), rgba(96,165,250,0.10))" }}
          >
            <span className="text-xs text-white/60">Slide 2</span>
            <h3 className="text-2xl font-semibold text-white mt-1">Solution</h3>
            <p className="mt-3 text-white/80">
              Upload once. We auto‑analyze, split by function/folder, create branches, commit, merge, push—and then one
              click deploys. No DevOps.
            </p>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 24 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, amount: 0.4 }}
            transition={{ duration: 0.7, delay: 0.1 }}
            className="rounded-2xl border border-white/10 p-6"
            style={{ background: "linear-gradient(135deg, rgba(34,211,238,0.15), rgba(96,165,250,0.15))" }}
          >
            <span className="text-xs text-white/60">Slide 3</span>
            <h3 className="text-2xl font-semibold text-white mt-1">Unique Value</h3>
            <p className="mt-3 text-white/80">
              Save hours, eliminate git errors, and guarantee a polished submission—built for hackathons, useful for any
              team.
            </p>
          </motion.div>
        </div>
      </div>
    </section>
  )
}
