export type AppStatus =
  | 'Saved'
  | 'Planning'
  | 'Applied'
  | 'Assessment'
  | 'Interview'
  | 'Offer'
  | 'Rejected'

export const STAGES: AppStatus[] = [
  'Saved',
  'Planning',
  'Applied',
  'Assessment',
  'Interview',
  'Offer',
]

export type Application = {
  id: string
  role: string
  company: string
  match: number
  status: AppStatus
  appliedDate: string | null
  nextAction: string
}

// Demo data — for prototype/layout purposes only.
export const mockApplications: Application[] = [
  {
    id: 'app-001',
    role: 'Java Backend Intern',
    company: 'Acme Technologies',
    match: 92,
    status: 'Interview',
    appliedDate: '3 Sep 2026',
    nextAction: 'Technical round — 18 Sep',
  },
  {
    id: 'app-002',
    role: 'Java Developer — Graduate Program',
    company: 'Ridgeline Softworks',
    match: 88,
    status: 'Assessment',
    appliedDate: '8 Sep 2026',
    nextAction: 'Online assessment due 20 Sep',
  },
  {
    id: 'app-003',
    role: 'Backend Engineering Intern',
    company: 'Northlane Systems',
    match: 79,
    status: 'Applied',
    appliedDate: '11 Sep 2026',
    nextAction: 'Awaiting response',
  },
  {
    id: 'app-004',
    role: 'Software Engineer I — Java',
    company: 'Forge Labs',
    match: 71,
    status: 'Planning',
    appliedDate: null,
    nextAction: 'Tailor resume before applying',
  },
  {
    id: 'app-005',
    role: 'Full Stack Developer Intern',
    company: 'Meridian Labs',
    match: 58,
    status: 'Rejected',
    appliedDate: '29 Aug 2026',
    nextAction: 'Closed — role filled',
  },
  {
    id: 'app-006',
    role: 'Cloud Support Intern',
    company: 'Vantage Systems',
    match: 64,
    status: 'Saved',
    appliedDate: null,
    nextAction: 'Review before applying',
  },
]
