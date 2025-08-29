"use client"

import type React from "react"
import { createContext, useContext, useEffect, useMemo, useState } from "react"

type User = { email: string }
type AuthContextType = {
  user: User | null
  signin: (email: string, password: string) => Promise<void>
  signup: (email: string, password: string) => Promise<void>
  signout: () => void
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null)

  useEffect(() => {
    const raw = localStorage.getItem("demo-user")
    if (raw) setUser(JSON.parse(raw))
  }, [])

  const value = useMemo<AuthContextType>(
    () => ({
      user,
      signin: async (email) => {
        const next = { email }
        localStorage.setItem("demo-user", JSON.stringify(next))
        setUser(next)
      },
      signup: async (email) => {
        const next = { email }
        localStorage.setItem("demo-user", JSON.stringify(next))
        setUser(next)
      },
      signout: () => {
        localStorage.removeItem("demo-user")
        setUser(null)
      },
    }),
    [user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error("useAuth must be used within AuthProvider")
  return ctx
}
