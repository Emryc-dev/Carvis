import React, { useEffect, useState } from 'react'
import { createRoot } from 'react-dom/client'
import { createClient } from '@supabase/supabase-js'
import './style.css'

const supabase = createClient(import.meta.env.VITE_SUPABASE_URL, import.meta.env.VITE_SUPABASE_ANON_KEY)
const api = import.meta.env.VITE_API_URL

type Scan = { id: string; status: string; confidence: number | null; created_at: string; identification?: { brand: string; model: string; uncertainties: string[] } }

function App() {
  const [email, setEmail] = useState('operator@example.com')
  const [scans, setScans] = useState<Scan[]>([])
  const [error, setError] = useState('')

  async function load() {
    const { data } = await supabase.auth.getSession()
    if (!data.session) return
    const response = await fetch(`${api}/api/v1/scans?limit=50`, { headers: { Authorization: `Bearer ${data.session.access_token}` } })
    if (!response.ok) { setError('Unable to load scans. This console currently shows only the signed-in user until server-side admin roles are enabled.'); return }
    setScans((await response.json()).items)
  }
  useEffect(() => { load() }, [])

  async function signIn(event: React.FormEvent) {
    event.preventDefault()
    const { error } = await supabase.auth.signInWithOAuth({ provider: 'google', options: { redirectTo: window.location.origin } })
    if (error) setError(error.message)
  }

  return <main>
    <header><div><small>CARVISION // OPERATIONS</small><h1>Recognition audit</h1></div><button onClick={load}>Refresh</button></header>
    <section className="notice">Safe initial console: authenticated personal scan audit. Global KPIs and review mutations remain disabled until a Supabase app-metadata admin role and backend authorization policy are configured.</section>
    <form onSubmit={signIn}><input type="email" placeholder="operator@example.com" value={email} onChange={e => setEmail(e.target.value)} required/><button>Send magic link</button></form>
    {error && <p className="status">{error}</p>}
    <section className="grid">{scans.map(scan => <article key={scan.id}><small>{scan.status} · {new Date(scan.created_at).toLocaleString()}</small><h2>{scan.identification ? `${scan.identification.brand} ${scan.identification.model}` : 'No identification'}</h2><p>Confidence: {scan.confidence == null ? '—' : `${Math.round(scan.confidence * 100)}%`}</p><p>{scan.identification?.uncertainties?.join(' · ') || 'No uncertainty notes'}</p></article>)}</section>
  </main>
}
createRoot(document.getElementById('root')!).render(<React.StrictMode><App/></React.StrictMode>)
