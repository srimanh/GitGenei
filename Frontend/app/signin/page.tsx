"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { useAuth } from "@/components/auth-provider"
import Link from "next/link"
import { Github } from "lucide-react"

export default function SignInPage() {
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const { signin } = useAuth()
  const router = useRouter()

  const handleGithubSignin = async () => {
    // For demo purposes, simulate GitHub OAuth
    console.log("GitHub signin initiated")
    await signin("github@example.com", "github-oauth")
    router.push("/dashboard")
  }

  return (
    <main className="min-h-screen bg-[#0b0f14] text-white grid place-items-center px-4">
      <div className="w-full max-w-sm rounded-2xl border border-white/10 bg-white/5 backdrop-blur p-6">
        <h1 className="text-2xl font-semibold text-white">Welcome back</h1>
        <p className="text-sm text-white/70 mt-1">Sign in to your GitGenei account.</p>

        {/* GitHub button */}
        <Button
          onClick={handleGithubSignin}
          className="w-full mt-4 bg-[#24292e] hover:bg-[#24292e]/90 text-white border border-white/10 flex items-center gap-2"
        >
          <span className="mr-2"></span> Continue with GitHub
        </Button>

        <div className="relative my-4">
          <div className="h-px bg-white/10" />
          <span className="absolute inset-0 -top-3 mx-auto w-max px-2 text-[11px] text-white/50 bg-[#0b0f14]">or</span>
        </div>

        <form
          className="mt-2 space-y-3"
          onSubmit={async (e) => {
            e.preventDefault()
            await signin(email, password)
            router.push("/dashboard")
          }}
        >
          <input
            className="w-full rounded-lg bg-white/5 border border-white/10 px-3 py-2 outline-none text-white placeholder:text-white/60"
            placeholder="Email"
            type="email"
            onChange={(e) => setEmail(e.target.value)}
          />
          <input
            className="w-full rounded-lg bg-white/5 border border-white/10 px-3 py-2 outline-none text-white placeholder:text-white/60"
            placeholder="Password"
            type="password"
            onChange={(e) => setPassword(e.target.value)}
          />
          <Button className="w-full bg-gradient-to-r from-[#22d3ee] to-[#60a5fa] text-black">Sign in</Button>
        </form>
        <div className="text-xs mt-3">
          No account?{" "}
          <Link className="text-[#22d3ee] hover:text-[#22d3ee]/80" href="/signup">
            Sign up
          </Link>
        </div>
      </div>
    </main>
  )
}
