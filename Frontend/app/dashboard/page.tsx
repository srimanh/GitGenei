"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "@/components/auth-provider"
import { Button } from "@/components/ui/button"
import Image from "next/image"

interface Repository {
  id: number
  name: string
  full_name: string
  description: string
  html_url: string
  language: string
  stargazers_count: number
  updated_at: string
  private: boolean
}

export default function DashboardPage() {
  const { user, signout, loading } = useAuth()
  const router = useRouter()
  const [repositories, setRepositories] = useState<Repository[]>([])
  const [loadingRepos, setLoadingRepos] = useState(false)

  useEffect(() => {
    if (!loading && !user) {
      router.replace("/signin")
    }
  }, [user, loading, router])

  useEffect(() => {
    if (user) {
      fetchRepositories()
    }
  }, [user])

  const fetchRepositories = async () => {
    setLoadingRepos(true)
    try {
      const response = await fetch("http://localhost:8080/api/dashboard/repositories", {
        credentials: 'include',
      })
      if (response.ok) {
        const data = await response.json()
        setRepositories(data.repositories || [])
      }
    } catch (error) {
      console.error('Failed to fetch repositories:', error)
    } finally {
      setLoadingRepos(false)
    }
  }

  if (loading) {
    return (
      <main className="min-h-screen bg-[#0b0f14] text-white grid place-items-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-[#22d3ee] mx-auto"></div>
          <p className="mt-2 text-white/70">Loading...</p>
        </div>
      </main>
    )
  }

  if (!user) return null

  return (
    <main className="min-h-screen bg-[#0b0f14] text-white">
      <div className="mx-auto w-[min(1100px,92vw)] pt-28">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            {user.avatarUrl && (
              <Image
                src={user.avatarUrl}
                alt={user.name}
                width={48}
                height={48}
                className="rounded-full"
              />
            )}
            <div>
              <h1 className="text-3xl font-semibold text-white">Welcome, {user.name}</h1>
              <p className="text-white/70">@{user.githubUsername}</p>
            </div>
          </div>
          <Button onClick={() => signout()} className="bg-white/5 hover:bg-white/10 border border-white/10">
            Sign out
          </Button>
        </div>

        <div className="mt-8 grid gap-6">
          <div className="rounded-xl border border-white/10 bg-white/5 p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-semibold text-white">Your GitHub Repositories</h2>
              <Button
                onClick={fetchRepositories}
                disabled={loadingRepos}
                className="bg-[#22d3ee] hover:bg-[#22d3ee]/90 text-black text-sm"
              >
                {loadingRepos ? "Loading..." : "Refresh"}
              </Button>
            </div>

            {loadingRepos ? (
              <div className="text-center py-8">
                <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-[#22d3ee] mx-auto"></div>
                <p className="mt-2 text-white/70">Loading repositories...</p>
              </div>
            ) : repositories.length > 0 ? (
              <div className="grid gap-4 max-h-96 overflow-y-auto">
                {repositories.slice(0, 10).map((repo) => (
                  <div key={repo.id} className="border border-white/10 rounded-lg p-4 bg-white/5">
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <h3 className="font-semibold text-white">{repo.name}</h3>
                        <p className="text-sm text-white/70 mt-1">{repo.description || "No description"}</p>
                        <div className="flex items-center gap-4 mt-2 text-xs text-white/60">
                          {repo.language && <span>• {repo.language}</span>}
                          <span>⭐ {repo.stargazers_count}</span>
                          <span>{repo.private ? "🔒 Private" : "🌐 Public"}</span>
                        </div>
                      </div>
                      <a
                        href={repo.html_url}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-[#22d3ee] hover:text-[#22d3ee]/80 text-sm"
                      >
                        View →
                      </a>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-white/70 text-center py-8">No repositories found</p>
            )}
          </div>
        </div>
      </div>
    </main>
  )
}
