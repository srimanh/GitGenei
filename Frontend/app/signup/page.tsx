"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { useAuth } from "@/components/auth-provider"
import Link from "next/link"
import { Github } from "lucide-react"

export default function SignUpPage() {
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const { signup } = useAuth()
  const router = useRouter()

  const handleGithubSignup = async () => {
    // For demo purposes, simulate GitHub OAuth
    console.log("GitHub signup initiated")
    await signup("github@example.com", "github-oauth")
    router.push("/dashboard")
  }

  return (
    <main className="min-h-screen bg-[#0b0f14] text-white grid place-items-center px-4">
      <div className="w-full max-w-sm rounded-2xl border border-white/10 bg-white/5 backdrop-blur p-6">
        <h1 className="text-2xl font-semibold text-white">Create account</h1>
        <p className="text-sm text-white/70 mt-1">Get started with GitGenei today.</p>

        {/* GitHub Sign Up Button */}
        <Button
          onClick={handleGithubSignup}
          className="w-full mt-4 bg-[#24292e] hover:bg-[#24292e]/90 text-white border border-white/10 flex items-center gap-2"
        >
          <Github className="w-4 h-4" />
          Continue with GitHub
        </Button>

        <div className="relative my-4">
          <div className="absolute inset-0 flex items-center">
            <span className="w-full border-t border-white/10" />
          </div>
          <div className="relative flex justify-center text-xs uppercase">
            <span className="bg-[#0b0f14] px-2 text-white/60">Or continue with email</span>
          </div>
        </div>

        <form
          className="space-y-3"
          onSubmit={async (e) => {
            e.preventDefault()
            await signup(email, password)
            router.push("/dashboard")
          }}
        >
          <input
            className="w-full rounded-lg bg-white/5 border border-white/10 px-3 py-2 outline-none text-white placeholder:text-white/60 focus:border-[#22d3ee]/50 transition-colors"
            placeholder="Email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <input
            className="w-full rounded-lg bg-white/5 border border-white/10 px-3 py-2 outline-none text-white placeholder:text-white/60 focus:border-[#22d3ee]/50 transition-colors"
            placeholder="Password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <Button className="w-full bg-gradient-to-r from-[#22d3ee] to-[#60a5fa] text-black font-semibold hover:from-[#22d3ee]/90 hover:to-[#60a5fa]/90 transition-all">
            Create account
          </Button>
        </form>
        <div className="text-xs mt-4 text-center">
          Already have an account?{" "}
          <Link className="text-[#22d3ee] hover:text-[#22d3ee]/80 transition-colors" href="/signin">
            Sign in
          </Link>
        </div>
      </div>
    </main>
  )
}
