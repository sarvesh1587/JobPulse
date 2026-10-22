import { Link } from 'react-router-dom'

/** The bar-chart "pulse" mark from the original JobPulse identity. */
export function LogoMark({ size = 22 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" aria-hidden>
      <rect width="32" height="32" rx="8" className="fill-ink" />
      <rect x="7" y="17" width="3.5" height="8" rx="1.5" className="fill-accent" />
      <rect x="12.5" y="7" width="3.5" height="18" rx="1.5" className="fill-accent" />
      <rect x="18" y="12" width="3.5" height="13" rx="1.5" className="fill-accent" />
      <rect x="23.5" y="9" width="3.5" height="16" rx="1.5" className="fill-accent" />
    </svg>
  )
}

export function Logo() {
  return (
    <Link to="/" className="flex items-center gap-2 rounded-md" aria-label="JobPulse home">
      <LogoMark />
      <span className="text-[15px] font-extrabold tracking-tight">JobPulse</span>
    </Link>
  )
}
