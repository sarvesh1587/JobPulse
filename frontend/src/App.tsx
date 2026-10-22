import { lazy, Suspense, useEffect } from 'react'
import { BrowserRouter, Outlet, Route, Routes, useLocation } from 'react-router-dom'
import { SiteHeader } from './components/layout/SiteHeader'
import { SiteFooter } from './components/layout/SiteFooter'
import { RequireRole } from './auth/RequireRole'
import { Skeleton } from './components/ui/Feedback'
import LandingPage from './pages/LandingPage'
import JobsPage from './pages/JobsPage'
import JobDetailPage from './pages/JobDetailPage'
import { LoginPage, RegisterPage } from './pages/AuthPages'
import NotFoundPage from './pages/NotFoundPage'

// Signed-in areas are split into their own chunks, so the public pages load
// without code that most visitors never run.
const DashboardPage = lazy(() => import('./pages/candidate/DashboardPage'))
const SavedPage = lazy(() => import('./pages/candidate/SavedPage'))
const ApplicationsPage = lazy(() => import('./pages/candidate/ApplicationsPage'))
const ApplicationDetailPage = lazy(() => import('./pages/candidate/ApplicationDetailPage'))
const ProfilePage = lazy(() => import('./pages/candidate/ProfilePage'))
const SkillGapPage = lazy(() => import('./pages/candidate/SkillGapPage'))
const CompaniesPage = lazy(() => import('./pages/CompaniesPage').then((m) => ({ default: m.CompaniesPage })))
const CompanyDetailPage = lazy(() => import('./pages/CompaniesPage').then((m) => ({ default: m.CompanyDetailPage })))
const EmployerHomePage = lazy(() => import('./pages/employer/EmployerHomePage'))
const JobFormPage = lazy(() => import('./pages/employer/JobFormPage'))
const ApplicantsPage = lazy(() => import('./pages/employer/ApplicantsPage'))
const EmployerApplicationPage = lazy(() => import('./pages/employer/EmployerApplicationPage'))
const CompanyProfilePage = lazy(() => import('./pages/employer/CompanyProfilePage'))
const AdminPage = lazy(() => import('./pages/admin/AdminPage'))

function ScrollToTop() {
  const { pathname } = useLocation()
  useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])
  return null
}

function Layout() {
  return (
    <div className="flex min-h-screen flex-col">
      <a href="#main" className="sr-only z-50 rounded-md bg-accent px-3 py-2 text-accent-ink focus:not-sr-only focus:fixed focus:left-3 focus:top-3">
        Skip to content
      </a>
      <SiteHeader />
      <main id="main" className="flex-1">
        <Suspense fallback={<div className="mx-auto max-w-page space-y-4 px-4 py-10 sm:px-6"><Skeleton className="h-8 w-60" /><Skeleton className="h-48 w-full" /></div>}>
          <Outlet />
        </Suspense>
      </main>
      <SiteFooter />
    </div>
  )
}

const candidate = (page: JSX.Element) => <RequireRole role="CANDIDATE">{page}</RequireRole>
const employer = (page: JSX.Element) => <RequireRole role="EMPLOYER">{page}</RequireRole>

export default function App() {
  return (
    <BrowserRouter>
      <ScrollToTop />
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<LandingPage />} />
          <Route path="jobs" element={<JobsPage />} />
          <Route path="jobs/:id" element={<JobDetailPage />} />
          <Route path="companies" element={<CompaniesPage />} />
          <Route path="companies/:id" element={<CompanyDetailPage />} />
          <Route path="login" element={<LoginPage />} />
          <Route path="register" element={<RegisterPage />} />

          <Route path="dashboard" element={candidate(<DashboardPage />)} />
          <Route path="saved" element={candidate(<SavedPage />)} />
          <Route path="applications" element={candidate(<ApplicationsPage />)} />
          <Route path="applications/:id" element={candidate(<ApplicationDetailPage />)} />
          <Route path="profile" element={candidate(<ProfilePage />)} />
          <Route path="skill-gap" element={candidate(<SkillGapPage />)} />

          <Route path="employer" element={employer(<EmployerHomePage />)} />
          <Route path="employer/company" element={employer(<CompanyProfilePage />)} />
          <Route path="employer/jobs/new" element={employer(<JobFormPage />)} />
          <Route path="employer/jobs/:id" element={employer(<ApplicantsPage />)} />
          <Route path="employer/jobs/:id/edit" element={employer(<JobFormPage />)} />
          <Route path="employer/applications/:id" element={employer(<EmployerApplicationPage />)} />

          <Route path="admin" element={<RequireRole role="ADMIN"><AdminPage /></RequireRole>} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
