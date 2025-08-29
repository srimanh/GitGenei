"use client"

import { FeatureCard } from "@/components/feature-card"

const features = [
  {
    title: "Auto Branching & Commits",
    desc: "AI analyzes your repo, organizes by function/folder, creates branches, and writes clean commit messages.",
  },
  {
    title: "Zero-Config Deploy",
    desc: "Click once to deploy your stack—no CI, no YAML. Works with common frameworks.",
  },
  {
    title: "Conflict Wrangler",
    desc: "We resolve merge conflicts safely with review hints you can accept or override.",
  },
  {
    title: "Audit Trail",
    desc: "Every change is tracked and explainable with diffs and AI rationale.",
  },
  {
    title: "Massive Zip Uploads",
    desc: "Ship any size project. We handle chunking, integrity, and resumable uploads.",
  },
  {
    title: "Team Ready",
    desc: "Invite collaborators, share branches, and keep velocity without chaos.",
  },
]

export function Features() {
  return (
    <section className="mx-auto w-[min(1100px,92vw)] mt-24">
      <h2 className="text-center text-3xl md:text-4xl font-semibold text-white">What makes it feel magic</h2>
      <p className="text-center text-white/80 mt-2">Animated cards. Hover to feel it.</p>
      <div className="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
        {features.map((f, i) => (
          <FeatureCard key={f.title} title={f.title} desc={f.desc} delay={i * 0.05} />
        ))}
      </div>
    </section>
  )
}
