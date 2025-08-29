"use client"

import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { useAuth } from "@/components/auth-provider"
import Link from "next/link"
import { Github, Sparkles, Zap, GitBranch } from "lucide-react"
import Image from "next/image"

export default function SignUpPage() {
  const { loginWithGitHub } = useAuth()
  const router = useRouter()

  const handleGithubSignup = () => {
    loginWithGitHub()
  }

  return (
    <main className="min-h-screen bg-[#0b0f14] text-white relative overflow-hidden">
      {/* Animated background */}
      <div className="absolute inset-0 bg-gradient-to-br from-[#0b0f14] via-[#0f1419] to-[#0b0f14]">
        <div className="absolute top-20 left-20 w-72 h-72 bg-[#22d3ee]/10 rounded-full blur-3xl animate-pulse"></div>
        <div className="absolute bottom-20 right-20 w-96 h-96 bg-[#60a5fa]/10 rounded-full blur-3xl animate-pulse delay-1000"></div>
        <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-64 h-64 bg-[#22d3ee]/5 rounded-full blur-2xl animate-pulse delay-500"></div>
      </div>

      <div className="relative z-10 grid place-items-center min-h-screen px-4">
        <div className="w-full max-w-md">
          {/* Logo and Header */}
          <div className="text-center mb-8">
            <div className="flex justify-center mb-4">
              <Image
                src="/gitgenei-logo.svg"
                alt="GitGenei Logo"
                width={64}
                height={64}
                className="shadow-[0_0_30px_5px_rgba(34,211,238,0.4)]"
              />
            </div>
            <h1 className="text-4xl font-bold text-white mb-2">
              Welcome to <span className="bg-gradient-to-r from-[#22d3ee] to-[#60a5fa] bg-clip-text text-transparent">GitGenei</span>
            </h1>
            <p className="text-lg text-white/70 mb-6">AI-powered Git automation at your fingertips</p>

            {/* Feature highlights */}
            <div className="grid grid-cols-3 gap-4 mb-8">
              <div className="text-center">
                <div className="w-12 h-12 bg-gradient-to-br from-[#22d3ee]/20 to-[#60a5fa]/20 rounded-xl flex items-center justify-center mx-auto mb-2">
                  <Zap className="w-6 h-6 text-[#22d3ee]" />
                </div>
                <p className="text-xs text-white/60">Instant Deploy</p>
              </div>
              <div className="text-center">
                <div className="w-12 h-12 bg-gradient-to-br from-[#22d3ee]/20 to-[#60a5fa]/20 rounded-xl flex items-center justify-center mx-auto mb-2">
                  <GitBranch className="w-6 h-6 text-[#60a5fa]" />
                </div>
                <p className="text-xs text-white/60">Auto Branches</p>
              </div>
              <div className="text-center">
                <div className="w-12 h-12 bg-gradient-to-br from-[#22d3ee]/20 to-[#60a5fa]/20 rounded-xl flex items-center justify-center mx-auto mb-2">
                  <Sparkles className="w-6 h-6 text-[#22d3ee]" />
                </div>
                <p className="text-xs text-white/60">AI Commits</p>
              </div>
            </div>
          </div>

          {/* Signup Card */}
          <div className="rounded-2xl border border-white/10 bg-white/5 backdrop-blur-xl p-8 shadow-2xl">
            <div className="text-center mb-6">
              <h2 className="text-2xl font-semibold text-white mb-2">Get Started</h2>
              <p className="text-white/70">Connect your GitHub account to begin</p>
            </div>

            {/* GitHub Sign Up Button */}
            <Button
              onClick={handleGithubSignup}
              className="w-full h-12 bg-gradient-to-r from-[#24292e] to-[#1a1e23] hover:from-[#2c3237] hover:to-[#24292e] text-white border border-white/20 flex items-center justify-center gap-3 text-lg font-semibold transition-all duration-300 shadow-lg hover:shadow-xl hover:scale-[1.02]"
            >
              <Github className="w-5 h-5" />
              Continue with GitHub
            </Button>

            <div className="mt-6 text-center">
              <p className="text-xs text-white/50 mb-4">
                By continuing, you agree to our Terms of Service and Privacy Policy
              </p>

              <div className="text-xs">
                Already have an account?{" "}
                <Link className="text-[#22d3ee] hover:text-[#22d3ee]/80 transition-colors font-medium" href="/signin">
                  Sign in here
                </Link>
              </div>
            </div>
          </div>

          {/* Back to home */}
          <div className="text-center mt-6">
            <Link
              href="/"
              className="text-white/60 hover:text-white/80 transition-colors text-sm"
            >
              ← Back to home
            </Link>
          </div>
        </div>
      </div>
    </main>
  )
}
