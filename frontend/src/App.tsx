import { BrowserRouter, Routes, Route } from 'react-router-dom'
import AppShell from './layouts/AppShell'
import Overview from './pages/Overview'
import Discover from './pages/Discover'
import Saved from './pages/Saved'
import Applications from './pages/Applications'
import SkillGap from './pages/SkillGap'
import Companies from './pages/Companies'
import Insights from './pages/Insights'
import Profile from './pages/Profile'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<AppShell />}>
          <Route path="/" element={<Overview />} />
          <Route path="/discover" element={<Discover />} />
          <Route path="/saved" element={<Saved />} />
          <Route path="/applications" element={<Applications />} />
          <Route path="/skill-gap" element={<SkillGap />} />
          <Route path="/companies" element={<Companies />} />
          <Route path="/insights" element={<Insights />} />
          <Route path="/profile" element={<Profile />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
