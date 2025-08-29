"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { useEffect, useState } from "react"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import Image from "next/image"

const links = [
  { href: "/product", label: "Product" },
  { href: "/pricing", label: "Pricing" },
  { href: "/docs", label: "Docs" },
  { href: "/blog", label: "Blog" },
]

export function Navbar() {
  const pathname = usePathname()
  const [scrolled, setScrolled] = useState(false)

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8)
    onScroll()
    window.addEventListener("scroll", onScroll)
    return () => window.removeEventListener("scroll", onScroll)
  }, [])

  return (
    <header
      className={cn(
        "fixed top-3 left-1/2 z-50 -translate-x-1/2 w-[min(1100px,92vw)] rounded-2xl border transition-all backdrop-blur-xl",
        scrolled
          ? "bg-white/7 border-white/10 shadow-[0_0_30px_-10px_rgba(34,211,238,0.35)]"
          : "bg-white/5 border-white/10",
      )}
      role="banner"
    >
      <nav className="flex items-center justify-between px-4 py-2">
        <Link href="/" className="flex items-center gap-2">
          <Image
            src="/gitgenei-logo.svg"
            alt="GitGenei Logo"
            width={24}
            height={24}
            className="shadow-[0_0_20px_2px_rgba(34,211,238,0.35)]"
          />
          <span className="text-sm font-medium text-white/90">GitGenei</span>
        </Link>

        <ul className="hidden md:flex items-center gap-2">
          {links.map((l) => (
            <li key={l.href}>
              <Link
                href={l.href}
                className={cn(
                  "text-sm text-white/75 hover:text-white transition-colors px-3 py-2 rounded-lg",
                  pathname === l.href && "text-white bg-white/5",
                )}
              >
                {l.label}
              </Link>
            </li>
          ))}
        </ul>

        <div className="flex items-center gap-2">
          <Link href="/signin">
            <Button variant="ghost" className="text-white/80 hover:text-white">
              Sign in
            </Button>
          </Link>
          <Link href="/signup">
            <Button
              className="bg-gradient-to-r from-[#22d3ee] to-[#60a5fa] text-black font-semibold hover:from-[#22d3ee] hover:to-[#60a5fa]/90"
              aria-label="Get started for free"
            >
              Get Started
            </Button>
          </Link>
        </div>
      </nav>
    </header>
  )
}
