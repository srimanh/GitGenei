"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "@/components/auth-provider"
import { Button } from "@/components/ui/button"

export default function DashboardPage() {
  const { user, signout } = useAuth()
  const router = useRouter()

  useEffect(() => {
    if (!user) router.replace("/signin")
  }, [user, router])

  if (!user) return null

  return (
    <main className="min-h-screen bg-[#0b0f14] text-white">
      <div className="mx-auto w-[min(1100px,92vw)] pt-28">
        <div className="flex items-center justify-between">
          <h1 className="text-3xl font-semibold text-white">Welcome, {user.email}</h1>
          <Button onClick={() => signout()} className="bg-white/5 hover:bg-white/10 border border-white/10">
            Sign out
          </Button>
        </div>

        <div className="mt-6 grid sm:grid-cols-2 gap-5">
          <div className="rounded-xl border border-white/10 bg-white/5 p-6">
            <h2 className="text-white font-semibold">Your Projects</h2>
            <p className="text-sm mt-2 text-white/80">Uploads and deploys will appear here.</p>
          </div>
          <div className="rounded-xl border border-white/10 bg-white/5 p-6">
            <h2 className="text-white font-semibold">Quick Actions</h2>
            <ul className="text-sm mt-2 list-disc pl-5 text-white/80">
              <li>Upload zip</li>
              <li>Connect GitHub</li>
              <li>One-click deploy</li>
            </ul>
          </div>
        </div>
      </div>
    </main>
  )
}
