export type Job = {
  id: string
  title: string
  company: string
  location: string
  workMode: 'Remote' | 'Hybrid' | 'On-site'
  skills: string[]
  match: number
  skillFit: number
  postedAgo: string
  source: 'Greenhouse' | 'Lever' | 'Ashby'
  officialSource: boolean
  applyability: 'HIGH' | 'MEDIUM' | 'LOW'
  missingSkills: string[]
}

// Demo data — for prototype/layout purposes only, not live listings.
export const mockJobs: Job[] = [
  {
    id: 'jp-001',
    title: 'Java Backend Intern',
    company: 'Acme Technologies',
    location: 'Pune',
    workMode: 'Hybrid',
    skills: ['Java', 'Spring Boot', 'MySQL', 'REST'],
    match: 92,
    skillFit: 96,
    postedAgo: '3h ago',
    source: 'Greenhouse',
    officialSource: true,
    applyability: 'HIGH',
    missingSkills: ['Redis', 'Kafka'],
  },
  {
    id: 'jp-002',
    title: 'Backend Engineering Intern',
    company: 'Northlane Systems',
    location: 'Pune',
    workMode: 'On-site',
    skills: ['Java', 'PostgreSQL', 'Docker'],
    match: 79,
    skillFit: 84,
    postedAgo: '6h ago',
    source: 'Lever',
    officialSource: true,
    applyability: 'HIGH',
    missingSkills: ['Docker'],
  },
  {
    id: 'jp-003',
    title: 'Software Engineer I — Java',
    company: 'Forge Labs',
    location: 'Pune',
    workMode: 'Remote',
    skills: ['Java', 'Spring', 'Kafka', 'AWS'],
    match: 71,
    skillFit: 68,
    postedAgo: '1d ago',
    source: 'Ashby',
    officialSource: true,
    applyability: 'MEDIUM',
    missingSkills: ['Kafka', 'AWS'],
  },
  {
    id: 'jp-004',
    title: 'Full Stack Developer Intern',
    company: 'Meridian Labs',
    location: 'Bangalore',
    workMode: 'Remote',
    skills: ['React', 'Node.js', 'MongoDB'],
    match: 58,
    skillFit: 52,
    postedAgo: '2d ago',
    source: 'Greenhouse',
    officialSource: true,
    applyability: 'LOW',
    missingSkills: ['Node.js', 'MongoDB', 'GraphQL'],
  },
  {
    id: 'jp-005',
    title: 'Java Developer — Graduate Program',
    company: 'Ridgeline Softworks',
    location: 'Pune',
    workMode: 'Hybrid',
    skills: ['Java', 'Spring Boot', 'MySQL', 'JUnit'],
    match: 88,
    skillFit: 90,
    postedAgo: '5h ago',
    source: 'Ashby',
    officialSource: true,
    applyability: 'HIGH',
    missingSkills: ['JUnit'],
  },
]
