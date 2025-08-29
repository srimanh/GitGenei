"use client"

import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { useAuth } from "@/components/auth-provider"
import Link from "next/link"
import { Github, ArrowLeft } from "lucide-react"
import Image from "next/image"

export default function SignInPage() {
  const { loginWithGitHub } = useAuth()
  const router = useRouter()

  const handleGithubSignin = () => {
    loginWithGitHub()
  }

  return (
    <main className="min-h-screen bg-[#0b0f14] text-white relative overflow-hidden">
      {/* Animated background */}
      <div className="absolute inset-0 bg-gradient-to-br from-[#0b0f14] via-[#0f1419] to-[#0b0f14]">
        <div className="absolute top-20 right-20 w-72 h-72 bg-[#22d3ee]/10 rounded-full blur-3xl animate-pulse"></div>
        <div className="absolute bottom-20 left-20 w-96 h-96 bg-[#60a5fa]/10 rounded-full blur-3xl animate-pulse delay-1000"></div>
      </div>

      <div className="relative z-10 grid place-items-center min-h-screen px-4">
        <div className="w-full max-w-md">
          {/* Logo and Header */}
          <div className="text-center mb-8">
            <div className="flex justify-center mb-4">
              <Image 
                src="/gitgenei-logo.svg" 
                alt="GitGenei Logo" 
                width={56} 
                height={56} 
                className="shadow-[0_0_25px_3px_rgba(34,211,238,0.4)]" 
              />
            </div>
            <h1 className="text-3xl font-bold text-white mb-2">
              Welcome back to <span className="bg-gradient-to-r from-[#22d3ee] to-[#60a5fa] bg-clip-text text-transparent">GitGenei</span>
            </h1>
            <p className="text-white/70">Continue your AI-powered development journey</p>
          </div>

          {/* Signin Card */}
          <div className="rounded-2xl border border-white/10 bg-white/5 backdrop-blur-xl p-8 shadow-2xl">
            <div className="text-center mb-6">
              <h2 className="text-xl font-semibold text-white mb-2">Sign In</h2>
              <p className="text-white/70">Access your GitGenei dashboard</p>
            </div>
            
            {/* GitHub Sign In Button */}
            <Button 
              onClick={handleGithubSignin}
              className="w-full h-12 bg-gradient-to-r from-[#24292e] to-[#1a1e23] hover:from-[#2c3237] hover:to-[#24292e] text-white border border-white/20 flex items-center justify-center gap-3 text-lg font-semibold transition-all duration-300 shadow-lg hover:shadow-xl hover:scale-[1.02]"
            >
              <Github className="w-5 h-5" />
              Continue with GitHub
            </Button>

            <div className="mt-6 text-center">
              <p className="text-xs text-white/50 mb-4">
                Secure authentication powered by GitHub OAuth
              </p>
              
              <div className="text-xs">
                Don't have an account?{" "}
                <Link className="text-[#22d3ee] hover:text-[#22d3ee]/80 transition-colors font-medium" href="/signup">
                  Sign up here
                </Link>
              </div>
            </div>
          </div>

          {/* Back to home */}
          <div className="text-center mt-6">
            <Link 
              href="/" 
              className="text-white/60 hover:text-white/80 transition-colors text-sm flex items-center justify-center gap-2"
            >
              <ArrowLeft className="w-4 h-4" />
              Back to home
            </Link>
          </div>
        </div>
      </div>
    </main>
  )
}
