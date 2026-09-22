import { Outlet } from 'react-router-dom'
import { useState } from 'react'
import Sidebar from '../components/Sidebar'

export default function AppShell() {
  const [theme, setTheme] = useState<'dark' | 'light'>('dark')

  const toggleTheme = () => {
    const next = theme === 'dark' ? 'light' : 'dark'
    setTheme(next)
    if (next === 'light') document.documentElement.setAttribute('data-theme', 'light')
    else document.documentElement.removeAttribute('data-theme')
  }

  return (
    <div className="flex h-screen w-screen overflow-hidden bg-paper text-ink">
      <Sidebar />
      <div className="flex-1 min-w-0 flex flex-col">
        <header className="flex items-center justify-between border-b border-border px-6 py-3.5 shrink-0">
          <div className="font-mono text-[10.5px] text-charcoal tracking-wide">
            JOB INTELLIGENCE WORKSPACE
          </div>
          <button
            onClick={toggleTheme}
            className="w-8 h-8 rounded-full border border-border-strong flex items-center justify-center text-[13px] hover:border-accent transition-colors"
            aria-label="Toggle theme"
          >
            {theme === 'dark' ? '◑' : '◐'}
          </button>
        </header>
        <main className="flex-1 min-h-0 overflow-hidden flex">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
