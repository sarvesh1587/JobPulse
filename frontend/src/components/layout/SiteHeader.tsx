import { useEffect, useState } from 'react'
import { NavLink, useLocation, useNavigate } from 'react-router-dom'
import { LogOut, Menu, X } from 'lucide-react'
import { Logo } from './Logo'
import { ThemeToggle } from './ThemeToggle'
import { useAuth } from '../../auth/AuthProvider'
import { ButtonLink } from '../ui/Button'
import { useToast } from '../ui/Toast'
import { initials } from '../../lib/format'
import type { Role } from '../../lib/types'

interface NavItem { to: string; label: string }

const PUBLIC_NAV: NavItem[] = [
  { to: '/jobs', label: 'Jobs' },
  { to: '/companies', label: 'Companies' },
]

const ROLE_NAV: Record<Role, NavItem[]> = {
  CANDIDATE: [
    { to: '/dashboard', label: 'Dashboard' },
    { to: '/jobs', label: 'Jobs' },
    { to: '/applications', label: 'Applications' },
    { to: '/saved', label: 'Saved' },
    { to: '/skill-gap', label: 'Skill gap' },
  ],
  EMPLOYER: [
    { to: '/employer', label: 'Workspace' },
    { to: '/employer/jobs/new', label: 'Post a job' },
    { to: '/jobs', label: 'Browse jobs' },
  ],
  ADMIN: [
    { to: '/admin', label: 'Admin' },
    { to: '/jobs', label: 'Jobs' },
    { to: '/companies', label: 'Companies' },
  ],
}

export function SiteHeader() {
  const { user, logout } = useAuth()
  const [menuOpen, setMenuOpen] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()
  const toast = useToast()

  useEffect(() => { setMenuOpen(false) }, [location.pathname])

  const items = user ? ROLE_NAV[user.role] : PUBLIC_NAV

  const signOut = async () => {
    // Leave the protected page first, so its route guard doesn't redirect to /login.
    navigate('/')
    try {
      await logout()
    } catch {
      toast.error("Couldn't sign out. Check your connection and try again.")
    }
  }

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
      isActive ? 'text-ink bg-raised' : 'text-muted hover:text-ink'
    }`

  return (
    <header className="sticky top-0 z-40 border-b border-border bg-paper/85 backdrop-blur-md">
      <div className="mx-auto flex h-14 max-w-page items-center gap-6 px-4 sm:px-6">
        <Logo />
        <nav aria-label="Main" className="hidden flex-1 items-center gap-1 md:flex">
          {items.map((item) => (
            <NavLink key={item.to} to={item.to} end={item.to === '/employer'} className={linkClass}>{item.label}</NavLink>
          ))}
        </nav>

        <div className="ml-auto flex items-center gap-2">
          <ThemeToggle />
          {user ? (
            <div className="hidden items-center gap-2 md:flex">
              {user.role === 'CANDIDATE' && (
                <NavLink to="/profile" className="flex items-center gap-2 rounded-lg py-1 pl-1 pr-2.5 text-sm font-medium hover:bg-raised" title="Your profile">
                  <span className="flex h-7 w-7 items-center justify-center rounded-full bg-accent/15 font-mono text-[11px] font-semibold text-accent-text">
                    {initials(user.fullName)}
                  </span>
                  Profile
                </NavLink>
              )}
              <button onClick={signOut} className="flex h-9 items-center gap-1.5 rounded-lg px-2.5 text-sm text-muted hover:bg-raised hover:text-ink">
                <LogOut size={15} /> Sign out
              </button>
            </div>
          ) : (
            <div className="hidden items-center gap-2 md:flex">
              <ButtonLink to="/login" variant="ghost" size="sm">Sign in</ButtonLink>
              <ButtonLink to="/register" size="sm">Create account</ButtonLink>
            </div>
          )}
          <button
            className="flex h-9 w-9 items-center justify-center rounded-lg hover:bg-raised md:hidden"
            onClick={() => setMenuOpen((o) => !o)}
            aria-expanded={menuOpen}
            aria-controls="mobile-nav"
            aria-label={menuOpen ? 'Close menu' : 'Open menu'}
          >
            {menuOpen ? <X size={18} /> : <Menu size={18} />}
          </button>
        </div>
      </div>

      {menuOpen && (
        <nav id="mobile-nav" aria-label="Mobile" className="rise border-t border-border px-4 pb-4 pt-2 md:hidden">
          {items.map((item) => (
            <NavLink key={item.to} to={item.to} end={item.to === '/employer'}
              className={({ isActive }) => `block rounded-md px-3 py-2.5 text-[15px] font-medium ${isActive ? 'bg-raised text-ink' : 'text-muted'}`}>
              {item.label}
            </NavLink>
          ))}
          <div className="mt-2 border-t border-border pt-3">
            {user ? (
              <>
                {user.role === 'CANDIDATE' && <NavLink to="/profile" className="block rounded-md px-3 py-2.5 text-[15px] font-medium text-muted">Profile</NavLink>}
                <button onClick={signOut} className="w-full rounded-md px-3 py-2.5 text-left text-[15px] font-medium text-muted">Sign out</button>
              </>
            ) : (
              <div className="flex gap-2">
                <ButtonLink to="/login" variant="secondary" className="flex-1">Sign in</ButtonLink>
                <ButtonLink to="/register" className="flex-1">Create account</ButtonLink>
              </div>
            )}
          </div>
        </nav>
      )}
    </header>
  )
}
