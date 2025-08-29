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
  loginWithGitHub: () => void
}

const AuthContext = createContext<AuthContextType | null>(null)
const API_BASE_URL = "http://localhost:8080"

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    // Only run on client side to avoid hydration issues
    if (typeof window !== 'undefined') {
      checkAuthStatus()
    } else {
      setLoading(false)
    }
  }, [])

  const checkAuthStatus = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/user`, {
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
      })

      if (response.ok) {
        const data = await response.json()
        if (data.authenticated && data.user) {
          setUser(data.user)
        }
      }
    } catch (error) {
      console.error('Auth check failed:', error)
    } finally {
      setLoading(false)
    }
  }

  const loginWithGitHub = () => {
    window.location.href = `${API_BASE_URL}/oauth2/authorization/github`
  }

  const value = useMemo<AuthContextType>(
    () => ({
      user,
      loading,
      signin: async () => {
        loginWithGitHub()
      },
      signup: async () => {
        loginWithGitHub()
      },
      signout: async () => {
        try {
          await fetch(`${API_BASE_URL}/api/auth/logout`, {
            method: 'POST',
            credentials: 'include',
          })
        } catch (error) {
          console.error('Logout failed:', error)
        } finally {
          setUser(null)
          window.location.href = '/'
        }
      },
      loginWithGitHub,
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
