"use client"

import { useEffect, useState, useCallback } from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "@/components/auth-provider"
import { Button } from "@/components/ui/button"
import { motion, AnimatePresence } from "framer-motion"
import { ProjectUploader } from "@/components/upload/project-uploader"
import {
  Upload,
  Github,
  Zap,
  Star,
  GitBranch,
  Clock,
  Rocket,
  Sparkles,
  Activity,
  TrendingUp,
  Code,
  Database,
  Globe,
  Bell,
  Search,
  Filter,
  Download,
  Eye,
  Heart,
  Coffee,
  FolderOpen,
  Calendar,
  CheckCircle,
  XCircle,
  AlertCircle,
  ExternalLink,
  Plus
} from "lucide-react"

interface Repository {
  id: number
  name: string
  full_name: string
  description: string
  html_url: string
  updated_at: string
  language: string
  stargazers_count: number
  forks_count: number
  private: boolean
}

interface DeploymentHistory {
  id: string
  project: string
  status: 'success' | 'pending' | 'failed'
  timestamp: string
  duration: string
}

interface Stats {
  totalProjects: number
  totalDeployments: number
  successRate: number
  activeRepos: number
}

export default function DashboardPage() {
  const { user, signout, loading } = useAuth()
  const router = useRouter()
  const [repositories, setRepositories] = useState<Repository[]>([])
  const [loadingRepos, setLoadingRepos] = useState(false)
  const [isDragging, setIsDragging] = useState(false)
  const [searchTerm, setSearchTerm] = useState("")
  const [selectedFilter, setSelectedFilter] = useState("all")
  const [showConfetti, setShowConfetti] = useState(false)
  const [mounted, setMounted] = useState(false)

  // Fix hydration error by ensuring client-side rendering
  useEffect(() => {
    setMounted(true)
  }, [])
  
  // Mock data for demo
  const [deploymentHistory] = useState<DeploymentHistory[]>([
    { id: '1', project: 'AI-Chat-Bot', status: 'success', timestamp: '2 hours ago', duration: '45s' },
    { id: '2', project: 'React-Dashboard', status: 'pending', timestamp: '5 hours ago', duration: '...' },
    { id: '3', project: 'Node-API', status: 'success', timestamp: '1 day ago', duration: '32s' },
    { id: '4', project: 'Vue-Portfolio', status: 'failed', timestamp: '2 days ago', duration: '1m 12s' },
  ])

  const [stats, setStats] = useState<Stats>({
    totalProjects: 0,
    totalDeployments: 0,
    successRate: 94.2,
    activeRepos: 0
  })
  const [totalCommits, setTotalCommits] = useState(0)

  useEffect(() => {
    if (!loading && !user) {
      router.replace("/signin")
    }
  }, [user, loading, router])

  useEffect(() => {
    if (user && user.githubUsername) {
      fetchRepositories()
    }
  }, [user])

  // Update stats when repositories or commits change
  useEffect(() => {
    if (repositories.length > 0) {
      setStats(prev => ({
        ...prev,
        totalProjects: repositories.length,
        totalDeployments: totalCommits, // Use commit count as deployments
        activeRepos: repositories.filter(repo => !repo.private).length
      }))
    }
  }, [repositories, totalCommits])

  const fetchRepositories = async () => {
    setLoadingRepos(true)
    try {
      const response = await fetch("http://localhost:8080/api/auth/github/repos", {
        credentials: "include",
      })
      if (response.ok) {
        const repos = await response.json()
        setRepositories(Array.isArray(repos) ? repos : [])
        // Fetch commit data for each repository
        fetchCommitData(repos)
      }
    } catch (error) {
      console.error("Failed to fetch repositories:", error)
    } finally {
      setLoadingRepos(false)
    }
  }

  const fetchCommitData = async (repos: Repository[]) => {
    try {
      let totalCommitCount = 0

      // Fetch commits for each repository (limit to avoid API rate limits)
      const commitPromises = repos.slice(0, 10).map(async (repo) => {
        try {
          const response = await fetch(
            `https://api.github.com/repos/${repo.full_name}/commits?per_page=100`,
            {
              headers: {
                'Accept': 'application/vnd.github.v3+json',
              }
            }
          )
          if (response.ok) {
            const commits = await response.json()
            return commits.length
          }
          return 0
        } catch (error) {
          console.error(`Failed to fetch commits for ${repo.name}:`, error)
          return 0
        }
      })

      const commitCounts = await Promise.all(commitPromises)
      totalCommitCount = commitCounts.reduce((sum, count) => sum + count, 0)

      setTotalCommits(totalCommitCount)
    } catch (error) {
      console.error("Failed to fetch commit data:", error)
    }
  }

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(true)
  }, [])

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(false)
  }, [])

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(false)
    setShowConfetti(true)
    setTimeout(() => setShowConfetti(false), 3000)
    // Handle file upload logic here
  }, [])

  const filteredRepos = repositories.filter(repo => {
    const matchesSearch = repo.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         repo.description?.toLowerCase().includes(searchTerm.toLowerCase())
    const matchesFilter = selectedFilter === "all" || 
                         (selectedFilter === "private" && repo.private) ||
                         (selectedFilter === "public" && !repo.private) ||
                         (selectedFilter === "starred" && repo.stargazers_count > 0)
    return matchesSearch && matchesFilter
  })

  // Fix hydration error by ensuring client-side rendering
  if (!mounted || loading) {
    return (
      <main className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 text-white flex items-center justify-center">
        <motion.div
          className="flex flex-col items-center gap-4"
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.5 }}
        >
          <div className="relative">
            <div className="w-16 h-16 border-4 border-cyan-500/30 border-t-cyan-400 rounded-full animate-spin"></div>
            <Sparkles className="absolute inset-0 m-auto w-6 h-6 text-cyan-400 animate-pulse" />
          </div>
          <p className="text-white/60 animate-pulse">Initializing your workspace...</p>
        </motion.div>
      </main>
    )
  }

  if (!user) return null

  const displayName = user.name || user.githubUsername || user.email
  const isGitHubUser = user.githubUsername && user.githubUsername.length > 0

  return (
    <div className="min-h-screen bg-[#0b0f14] text-white relative overflow-hidden">
      {/* Animated Background Elements */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-20 left-10 w-72 h-72 bg-cyan-500/10 rounded-full blur-3xl animate-pulse"></div>
        <div className="absolute bottom-20 right-10 w-96 h-96 bg-purple-500/10 rounded-full blur-3xl animate-pulse"></div>
        <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-gradient-to-r from-cyan-500/5 to-purple-500/5 rounded-full blur-3xl"></div>
      </div>

      {/* Glassy Animated Navbar */}
      <motion.nav 
        className="fixed top-0 left-0 right-0 z-50 backdrop-blur-xl bg-white/5 border-b border-white/10"
        initial={{ y: -100, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ duration: 0.8, ease: "easeOut" }}
      >
        <div className="max-w-7xl mx-auto px-6 py-4">
          <div className="flex items-center justify-between">
            <motion.div 
              className="flex items-center gap-3"
              whileHover={{ scale: 1.05 }}
              transition={{ type: "spring", stiffness: 400, damping: 10 }}
            >
              <div className="relative">
                <Zap className="w-8 h-8 text-cyan-400" />
                <motion.div
                  className="absolute inset-0 bg-cyan-400/20 rounded-full blur-lg"
                  animate={{ scale: [1, 1.2, 1] }}
                  transition={{ duration: 2, repeat: Infinity }}
                />
              </div>
              <span className="text-xl font-bold bg-gradient-to-r from-cyan-400 to-purple-400 bg-clip-text text-transparent">
                GitGenei
              </span>
            </motion.div>

            <div className="flex items-center gap-4">
              <motion.button
                className="relative p-2 rounded-full bg-white/5 hover:bg-white/10 transition-colors"
                whileHover={{ scale: 1.1 }}
                whileTap={{ scale: 0.95 }}
              >
                <Bell className="w-5 h-5 text-white/70" />
                <motion.div
                  className="absolute -top-1 -right-1 w-3 h-3 bg-red-500 rounded-full"
                  animate={{ scale: [1, 1.2, 1] }}
                  transition={{ duration: 1, repeat: Infinity }}
                />
              </motion.button>

              <motion.button
                onClick={() => signout()}
                className="px-4 py-2 rounded-full bg-gradient-to-r from-red-500/20 to-pink-500/20 hover:from-red-500/30 hover:to-pink-500/30 border border-red-500/30 text-white/90 transition-all duration-300"
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
              >
                Sign out
              </motion.button>
            </div>
          </div>
        </div>
      </motion.nav>

      {/* Main Content */}
      <main className="relative z-10 pt-24 pb-20">
        <div className="max-w-7xl mx-auto px-6">

          {/* Hero Greeting Section - Matching the image style */}
          <motion.section
            className="mb-8"
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.2 }}
          >
            <div className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-purple-600/20 via-purple-500/10 to-blue-600/20 backdrop-blur-xl border border-purple-500/30 p-6">
              <div className="absolute inset-0 bg-gradient-to-r from-purple-500/10 via-transparent to-blue-500/10"></div>

              <div className="relative flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <motion.div
                    className="relative"
                    whileHover={{ scale: 1.05 }}
                    transition={{ type: "spring", stiffness: 400, damping: 10 }}
                  >
                    {user.avatarUrl ? (
                      <div className="relative">
                        <img
                          src={user.avatarUrl}
                          alt="Avatar"
                          className="w-16 h-16 rounded-full border-2 border-green-400/50"
                        />
                        <div className="absolute -bottom-1 -right-1 w-4 h-4 bg-green-500 rounded-full border-2 border-white/20 flex items-center justify-center">
                          <div className="w-1.5 h-1.5 bg-white rounded-full"></div>
                        </div>
                      </div>
                    ) : (
                      <div className="w-16 h-16 rounded-full bg-gradient-to-br from-cyan-400 to-purple-500 flex items-center justify-center text-xl font-bold">
                        {displayName.charAt(0).toUpperCase()}
                      </div>
                    )}
                  </motion.div>

                  <div>
                    <motion.h1
                      className="text-2xl font-bold text-white mb-1"
                      initial={{ opacity: 0, x: -20 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ duration: 0.8, delay: 0.4 }}
                    >
                      Welcome back, {displayName}! ✨
                    </motion.h1>
                    {isGitHubUser && (
                      <motion.div
                        className="flex items-center gap-2 text-white/70 text-sm"
                        initial={{ opacity: 0, x: -20 }}
                        animate={{ opacity: 1, x: 0 }}
                        transition={{ duration: 0.8, delay: 0.6 }}
                      >
                        <Github className="w-4 h-4" />
                        <span>@{user.githubUsername}</span>
                        <span className="text-green-400">• Connected</span>
                      </motion.div>
                    )}
                    <motion.p
                      className="text-white/60 text-sm mt-1"
                      initial={{ opacity: 0, x: -20 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ duration: 0.8, delay: 0.8 }}
                    >
                      Ready to deploy something amazing today?
                    </motion.p>
                  </div>
                </div>

                {/* Live Stats Badges - Matching the image */}
                <motion.div
                  className="flex gap-6"
                  initial={{ opacity: 0, scale: 0.8 }}
                  animate={{ opacity: 1, scale: 1 }}
                  transition={{ duration: 0.8, delay: 1 }}
                >
                  <div className="text-center">
                    <div className="text-xl font-bold text-cyan-400">{stats.totalProjects}</div>
                    <div className="text-xs text-white/60">Projects</div>
                  </div>
                  <div className="text-center">
                    <div className="text-xl font-bold text-purple-400">{stats.totalDeployments}</div>
                    <div className="text-xs text-white/60">Deployments</div>
                  </div>
                  <div className="text-center">
                    <div className="text-xl font-bold text-green-400">{stats.successRate}%</div>
                    <div className="text-xs text-white/60">Success Rate</div>
                  </div>
                </motion.div>
              </div>
            </div>
          </motion.section>

          {/* Main Dashboard Grid */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">

            {/* Upload Section */}
            <motion.div
              className="lg:col-span-2"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6, delay: 0.3 }}
            >
              <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-6">
                <div className="mb-6">
                  <h2 className="text-xl font-semibold text-white mb-2 flex items-center gap-2">
                    <Upload className="w-5 h-5 text-cyan-400" />
                    Upload Your Whole Project
                  </h2>
                  <p className="text-white/70 text-sm">
                    Drop your entire project (up to 5 GB) and let AI analyze, organize, and push to GitHub automatically
                  </p>
                </div>

                <ProjectUploader
                  onUploadComplete={(result) => {
                    console.log('Upload completed:', result)
                    setShowConfetti(true)
                    setTimeout(() => setShowConfetti(false), 3000)
                  }}
                  onAnalysisComplete={(analysis) => {
                    console.log('Analysis completed:', analysis)
                    // Handle analysis results - could show results modal or navigate to results page
                  }}
                />
              </div>
            </motion.div>

            {/* Quick Stats */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6, delay: 0.4 }}
            >
              <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-6">
                <h2 className="text-xl font-semibold text-white mb-4 flex items-center gap-2">
                  <Activity className="w-5 h-5 text-purple-400" />
                  Quick Stats
                </h2>

                <div className="space-y-4">
                  <div className="flex items-center justify-between p-3 bg-white/5 rounded-lg">
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 bg-cyan-400/20 rounded-lg flex items-center justify-center">
                        <Rocket className="w-4 h-4 text-cyan-400" />
                      </div>
                      <span className="text-white/80">Active Projects</span>
                    </div>
                    <span className="text-cyan-400 font-semibold">{stats.activeRepos}</span>
                  </div>

                  <div className="flex items-center justify-between p-3 bg-white/5 rounded-lg">
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 bg-green-400/20 rounded-lg flex items-center justify-center">
                        <CheckCircle className="w-4 h-4 text-green-400" />
                      </div>
                      <span className="text-white/80">Successful</span>
                    </div>
                    <span className="text-green-400 font-semibold">{Math.round(stats.totalDeployments * stats.successRate / 100)}</span>
                  </div>

                  <div className="flex items-center justify-between p-3 bg-white/5 rounded-lg">
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 bg-red-400/20 rounded-lg flex items-center justify-center">
                        <XCircle className="w-4 h-4 text-red-400" />
                      </div>
                      <span className="text-white/80">Failed</span>
                    </div>
                    <span className="text-red-400 font-semibold">{stats.totalDeployments - Math.round(stats.totalDeployments * stats.successRate / 100)}</span>
                  </div>
                </div>
              </div>
            </motion.div>
          </div>

          {/* Repositories Section */}
          <motion.section
            className="mb-8"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.5 }}
          >
            <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-xl font-semibold text-white flex items-center gap-2">
                  <Github className="w-5 h-5 text-cyan-400" />
                  Your Repositories
                </h2>

                {/* Search Bar */}
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-white/40" />
                  <input
                    type="text"
                    placeholder="Search repositories..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="pl-10 pr-4 py-2 bg-white/10 border border-white/20 rounded-lg text-white placeholder-white/40 focus:outline-none focus:border-cyan-400/50 transition-colors w-64"
                  />
                </div>
              </div>

              {/* Filter Buttons */}
              <div className="flex gap-2 mb-4">
                {["all", "public", "private", "starred"].map((filter) => (
                  <button
                    key={filter}
                    onClick={() => setSelectedFilter(filter)}
                    className={`px-3 py-1 rounded-lg text-sm transition-colors ${
                      selectedFilter === filter
                        ? "bg-cyan-400/20 text-cyan-400 border border-cyan-400/30"
                        : "bg-white/5 text-white/70 hover:bg-white/10"
                    }`}
                  >
                    {filter.charAt(0).toUpperCase() + filter.slice(1)}
                  </button>
                ))}
              </div>

              {/* Repository Grid */}
              <div className="grid grid-cols-3 gap-4 max-h-[600px] overflow-y-auto pr-2">
                {loadingRepos ? (
                  [...Array(9)].map((_, i) => (
                    <div key={i} className="bg-white/5 rounded-lg p-4 animate-pulse">
                      <div className="h-4 bg-white/10 rounded mb-2"></div>
                      <div className="h-3 bg-white/10 rounded mb-3 w-3/4"></div>
                      <div className="flex gap-2">
                        <div className="h-2 bg-white/10 rounded w-12"></div>
                        <div className="h-2 bg-white/10 rounded w-8"></div>
                      </div>
                    </div>
                  ))
                ) : filteredRepos.length > 0 ? (
                  filteredRepos.map((repo) => (
                    <motion.div
                      key={repo.id}
                      className="bg-white/5 hover:bg-white/10 border border-white/10 rounded-lg p-3 transition-all duration-300 group cursor-pointer"
                      whileHover={{ scale: 1.02 }}
                      transition={{ type: "spring", stiffness: 300, damping: 20 }}
                    >
                      <div className="flex items-start justify-between mb-2">
                        <div className="flex items-center gap-2 min-w-0 flex-1">
                          <FolderOpen className="w-3 h-3 text-cyan-400 flex-shrink-0" />
                          <h3 className="font-semibold text-white group-hover:text-cyan-400 transition-colors text-sm truncate">
                            {repo.name}
                          </h3>
                          {repo.private && (
                            <span className="px-1 py-0.5 bg-yellow-500/20 text-yellow-400 text-xs rounded flex-shrink-0">
                              Private
                            </span>
                          )}
                        </div>
                      </div>

                      <p className="text-white/60 text-xs mb-3 line-clamp-2 h-8 overflow-hidden">
                        {repo.description || "No description available"}
                      </p>

                      <div className="space-y-2">
                        <div className="flex items-center justify-between text-xs text-white/50">
                          <div className="flex items-center gap-2">
                            {repo.language && (
                              <span className="flex items-center gap-1">
                                <div className="w-2 h-2 bg-cyan-400 rounded-full"></div>
                                <span className="truncate max-w-[50px]">{repo.language}</span>
                              </span>
                            )}
                            <span className="flex items-center gap-1">
                              <Star className="w-3 h-3" />
                              {repo.stargazers_count}
                            </span>
                          </div>
                        </div>
                        <div className="text-xs text-white/50 mb-3">
                          Updated {new Date(repo.updated_at).toLocaleDateString()}
                        </div>

                        {/* Deploy Button */}
                        <motion.button
                          className="w-full bg-gradient-to-r from-purple-500 to-cyan-500 hover:from-purple-600 hover:to-cyan-600 text-white text-xs font-medium py-2 px-3 rounded-lg transition-all duration-300 flex items-center justify-center gap-2 group"
                          whileHover={{ scale: 1.02 }}
                          whileTap={{ scale: 0.98 }}
                          onClick={(e) => {
                            e.stopPropagation()
                            // TODO: Implement deploy functionality
                            console.log(`Deploy ${repo.name}`)
                          }}
                        >
                          <Rocket className="w-3 h-3 group-hover:animate-pulse" />
                          Deploy
                        </motion.button>
                      </div>
                    </motion.div>
                  ))
                ) : (
                  <div className="col-span-full text-center py-12">
                    <Github className="w-12 h-12 text-white/30 mx-auto mb-4" />
                    <p className="text-white/60">No repositories found</p>
                  </div>
                )}
              </div>
            </div>
          </motion.section>

          {/* Recent Deployments */}
          <motion.section
            className="mb-8"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.6 }}
          >
            <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-6">
              <h2 className="text-xl font-semibold text-white mb-6 flex items-center gap-2">
                <Clock className="w-5 h-5 text-purple-400" />
                Recent Deployments
              </h2>

              <div className="space-y-3">
                {deploymentHistory.map((deployment) => (
                  <motion.div
                    key={deployment.id}
                    className="flex items-center justify-between p-4 bg-white/5 hover:bg-white/10 rounded-lg transition-all duration-300 group"
                    whileHover={{ scale: 1.01 }}
                    transition={{ type: "spring", stiffness: 300, damping: 20 }}
                  >
                    <div className="flex items-center gap-4">
                      <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${
                        deployment.status === 'success'
                          ? 'bg-green-400/20 text-green-400'
                          : deployment.status === 'pending'
                          ? 'bg-yellow-400/20 text-yellow-400'
                          : 'bg-red-400/20 text-red-400'
                      }`}>
                        {deployment.status === 'success' && <CheckCircle className="w-5 h-5" />}
                        {deployment.status === 'pending' && <Clock className="w-5 h-5" />}
                        {deployment.status === 'failed' && <XCircle className="w-5 h-5" />}
                      </div>

                      <div>
                        <h3 className="font-semibold text-white group-hover:text-cyan-400 transition-colors">
                          {deployment.project}
                        </h3>
                        <div className="flex items-center gap-3 text-sm text-white/60">
                          <span>{deployment.timestamp}</span>
                          <span>•</span>
                          <span>Duration: {deployment.duration}</span>
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-2">
                      <span className={`px-3 py-1 rounded-full text-xs font-medium ${
                        deployment.status === 'success'
                          ? 'bg-green-400/20 text-green-400'
                          : deployment.status === 'pending'
                          ? 'bg-yellow-400/20 text-yellow-400'
                          : 'bg-red-400/20 text-red-400'
                      }`}>
                        {deployment.status.charAt(0).toUpperCase() + deployment.status.slice(1)}
                      </span>

                      <button className="p-2 rounded-lg bg-white/5 hover:bg-white/10 transition-colors opacity-0 group-hover:opacity-100">
                        <ExternalLink className="w-4 h-4 text-white/70" />
                      </button>
                    </div>
                  </motion.div>
                ))}
              </div>

              {/* View All Button */}
              <div className="mt-6 text-center">
                <Button
                  variant="outline"
                  className="border-white/20 text-white/70 hover:bg-white/10 hover:text-white"
                >
                  View All Deployments
                  <ExternalLink className="w-4 h-4 ml-2" />
                </Button>
              </div>
            </div>
          </motion.section>
        </div>
      </main>
    </div>
  )
}
