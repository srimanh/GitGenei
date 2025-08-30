"use client"

import type React from "react"
import { createContext, useContext, useEffect, useMemo, useState } from "react"

type User = {
  id: number
  email: string
  name: string
  githubUsername: string
  avatarUrl: string
}

type AuthContextType = {
  user: User | null
  loading: boolean
  signin: (email: string, password: string) => Promise<void>
  signup: (email: string, password: string) => Promise<void>
  signout: () => void
  checkAuth: () => Promise<void>
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  const checkAuth = async () => {
    try {
      const response = await fetch("http://localhost:8080/api/auth/user", {
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
        },
      })

      if (response.ok) {
        const data = await response.json()
        if (data.authenticated && data.user) {
          setUser(data.user)
        } else {
          setUser(null)
        }
      } else {
        setUser(null)
      }
    } catch (error) {
      console.error("Auth check failed:", error)
      setUser(null)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    checkAuth()
  }, [])

  const value = useMemo<AuthContextType>(
    () => ({
      user,
      loading,
      signin: async (email, password) => {
        // For demo purposes - regular email/password signin
        const next = {
          id: 1,
          email,
          name: email.split('@')[0],
          githubUsername: '',
          avatarUrl: ''
        }
        setUser(next)
      },
      signup: async (email, password) => {
        // For demo purposes - regular email/password signup
        const next = {
          id: 1,
          email,
          name: email.split('@')[0],
          githubUsername: '',
          avatarUrl: ''
        }
        setUser(next)
      },
      signout: async () => {
        try {
          await fetch("http://localhost:8080/logout", {
            method: "POST",
            credentials: "include",
          })
        } catch (error) {
          console.error("Logout failed:", error)
        }
        setUser(null)
      },
      checkAuth,
    }),
    [user, loading],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error("useAuth must be used within AuthProvider")
  return ctx
}
