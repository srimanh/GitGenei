"use client"

import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { useAuth } from "@/components/auth-provider"
import Link from "next/link"
import { Github, Sparkles, Zap, Code, Rocket } from "lucide-react"
import { motion, AnimatePresence } from "framer-motion"

export default function SignInPage() {
  const { signin } = useAuth()
  const router = useRouter()
  const [isLoading, setIsLoading] = useState(false)
  const [particles, setParticles] = useState<Array<{id: number, x: number, y: number, delay: number}>>([])

  useEffect(() => {
    // Generate floating particles for background animation
    const newParticles = Array.from({ length: 20 }, (_, i) => ({
      id: i,
      x: Math.random() * 100,
      y: Math.random() * 100,
      delay: Math.random() * 2
    }))
    setParticles(newParticles)
  }, [])

  const handleGithubSignin = async () => {
    setIsLoading(true)
    // Add a small delay for better UX
    setTimeout(() => {
      window.location.href = "http://localhost:8080/oauth2/authorization/github"
    }, 500)
  }

  return (
    <main className="min-h-screen bg-gradient-to-br from-[#0b0f14] via-[#1a1f2e] to-[#0b0f14] text-white relative overflow-hidden">
      {/* Animated Background Particles */}
      <div className="absolute inset-0 overflow-hidden">
        {particles.map((particle) => (
          <motion.div
            key={particle.id}
            className="absolute w-1 h-1 bg-cyan-400/30 rounded-full"
            style={{
              left: `${particle.x}%`,
              top: `${particle.y}%`,
            }}
            animate={{
              y: [0, -20, 0],
              opacity: [0.3, 0.8, 0.3],
              scale: [1, 1.5, 1],
            }}
            transition={{
              duration: 3 + Math.random() * 2,
              repeat: Infinity,
              delay: particle.delay,
            }}
          />
        ))}
      </div>

      {/* Gradient Orbs */}
      <div className="absolute top-1/4 left-1/4 w-64 h-64 bg-purple-500/10 rounded-full blur-3xl animate-pulse" />
      <div className="absolute bottom-1/4 right-1/4 w-64 h-64 bg-cyan-500/10 rounded-full blur-3xl animate-pulse" style={{ animationDelay: '1s' }} />

      <div className="relative z-10 min-h-screen flex items-center justify-center px-4">
        <motion.div
          initial={{ opacity: 0, y: 50, scale: 0.9 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          transition={{ duration: 0.8, ease: "easeOut" }}
          className="w-full max-w-md"
        >
          {/* Logo and Title Section */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.2 }}
            className="text-center mb-8"
          >
            <motion.div
              className="inline-flex items-center gap-2 mb-4"
              whileHover={{ scale: 1.05 }}
              transition={{ type: "spring", stiffness: 400, damping: 10 }}
            >
              <div className="relative">
                <Zap className="w-8 h-8 text-cyan-400" />
                <motion.div
                  className="absolute inset-0"
                  animate={{ rotate: 360 }}
                  transition={{ duration: 8, repeat: Infinity, ease: "linear" }}
                >
                  <Sparkles className="w-8 h-8 text-purple-400" />
                </motion.div>
              </div>
              <span className="text-2xl font-bold bg-gradient-to-r from-cyan-400 to-purple-400 bg-clip-text text-transparent">
                GitGenei
              </span>
            </motion.div>
            
            <motion.h1
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ duration: 0.6, delay: 0.4 }}
              className="text-3xl font-bold text-white mb-2"
            >
              Welcome Back! ✨
            </motion.h1>
            
            <motion.p
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ duration: 0.6, delay: 0.6 }}
              className="text-white/70 text-lg"
            >
              Continue your coding journey with AI-powered Git magic
            </motion.p>
          </motion.div>

          {/* Main Sign-in Card */}
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.3 }}
            className="relative rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl p-8 shadow-2xl"
          >
            <div className="relative z-10">
              {/* Feature highlights */}
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ duration: 0.6, delay: 0.8 }}
                className="grid grid-cols-3 gap-4 mb-6"
              >
                {[
                  { icon: Code, text: "Smart Commits", color: "text-cyan-400" },
                  { icon: Github, text: "Auto Deploy", color: "text-purple-400" },
                  { icon: Rocket, text: "AI Powered", color: "text-green-400" }
                ].map((feature, index) => (
                  <motion.div
                    key={feature.text}
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.5, delay: 0.9 + index * 0.1 }}
                    className="text-center"
                  >
                    <motion.div
                      className={`w-10 h-10 mx-auto mb-2 rounded-lg bg-white/5 flex items-center justify-center ${feature.color}`}
                      whileHover={{ scale: 1.1, rotate: 5 }}
                      transition={{ type: "spring", stiffness: 400, damping: 10 }}
                    >
                      <feature.icon className="w-5 h-5" />
                    </motion.div>
                    <p className="text-xs text-white/60">{feature.text}</p>
                  </motion.div>
                ))}
              </motion.div>

              {/* GitHub Sign-in Button */}
              <motion.div
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ duration: 0.6, delay: 1.2 }}
              >
                <motion.button
                  onClick={handleGithubSignin}
                  disabled={isLoading}
                  className="w-full relative group"
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                  transition={{ type: "spring", stiffness: 400, damping: 10 }}
                >
                  <div className="absolute inset-0 bg-gradient-to-r from-[#24292e] to-[#1a1e22] rounded-xl blur-sm group-hover:blur-md transition-all duration-300" />
                  <div className="relative bg-gradient-to-r from-[#24292e] to-[#1a1e22] hover:from-[#2d333a] hover:to-[#24292e] text-white border border-white/20 rounded-xl px-6 py-4 flex items-center justify-center gap-3 transition-all duration-300">
                    <AnimatePresence mode="wait">
                      {isLoading ? (
                        <motion.div
                          key="loading"
                          initial={{ opacity: 0, scale: 0.8 }}
                          animate={{ opacity: 1, scale: 1 }}
                          exit={{ opacity: 0, scale: 0.8 }}
                          className="flex items-center gap-2"
                        >
                          <motion.div
                            className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full"
                            animate={{ rotate: 360 }}
                            transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
                          />
                          <span className="font-semibold">Connecting...</span>
                        </motion.div>
                      ) : (
                        <motion.div
                          key="github"
                          initial={{ opacity: 0, scale: 0.8 }}
                          animate={{ opacity: 1, scale: 1 }}
                          exit={{ opacity: 0, scale: 0.8 }}
                          className="flex items-center gap-3"
                        >
                          <motion.div
                            whileHover={{ rotate: 360 }}
                            transition={{ duration: 0.5 }}
                          >
                            <Github className="w-5 h-5" />
                          </motion.div>
                          <span className="font-semibold text-lg">Continue with GitHub</span>
                          <motion.div
                            animate={{ x: [0, 5, 0] }}
                            transition={{ duration: 1.5, repeat: Infinity }}
                          >
                            →
                          </motion.div>
                        </motion.div>
                      )}
                    </AnimatePresence>
                  </div>
                </motion.button>
              </motion.div>

              {/* Benefits text */}
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ duration: 0.6, delay: 1.4 }}
                className="mt-6 text-center"
              >
                <p className="text-sm text-white/60">
                  🔒 Secure • 🚀 Instant Setup • ⚡ No Configuration Required
                </p>
              </motion.div>
            </div>
          </motion.div>

          {/* Footer */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.6, delay: 1.6 }}
            className="text-center mt-8"
          >
            <p className="text-sm text-white/50">
              New to GitGenei?{" "}
              <Link 
                className="text-cyan-400 hover:text-cyan-300 transition-colors font-medium" 
                href="/signup"
              >
                Create your account →
              </Link>
            </p>
          </motion.div>
        </motion.div>
      </div>
    </main>
  )
}
