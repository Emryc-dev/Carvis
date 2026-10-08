import React, { useCallback, useEffect, useMemo, useState } from "react"
import { createRoot } from "react-dom/client"
import { createClient, type Session } from "@supabase/supabase-js"
import {
  Activity, Car, CheckCircle2, CircleAlert, Database, FileSearch, Gauge,
  Eye, EyeOff, HelpCircle, LayoutDashboard, LogOut, Menu, Moon, RefreshCw, Search,
  Server, Sun, Users,
} from "lucide-react"
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip as ChartTooltip, XAxis, YAxis } from "recharts"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip"
import {
  Sidebar, SidebarContent, SidebarFooter, SidebarGroup, SidebarGroupLabel,
  SidebarHeader, SidebarInset, SidebarMenu, SidebarMenuButton, SidebarMenuItem,
  SidebarProvider, SidebarTrigger, useSidebar,
} from "@/components/ui/sidebar"
import "./style.css"

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL
const supabaseKey = import.meta.env.VITE_SUPABASE_ANON_KEY
const api = String(import.meta.env.VITE_API_URL || "http://127.0.0.1:8000").replace(/\/$/, "")
const supabase = createClient(supabaseUrl, supabaseKey)

type Page = "overview" | "scans" | "users" | "vehicles" | "system"
type Metrics = { users: number; vehicles: number; scans: number; scans_last_30_days: number; completed_scans: number; failed_scans: number; ai_requests: number; favorites: number; average_confidence: number | null }
type Scan = { id: string; status: string; confidence: number | null; created_at: string; completed_at?: string | null; error_code?: string | null; user_id: string; user_name?: string | null; vehicle?: { id?: string | null; brand?: string | null; model?: string | null; year?: number | null } }
type User = { id: string; auth_user_id: string; name?: string | null; avatar_url?: string | null; created_at: string; scan_count: number }
type Vehicle = { id: string; brand: string; model: string; generation?: string | null; year?: number | null; vehicle_type?: string | null; image_url?: string | null; created_at: string }
type Overview = { metrics: Metrics; scan_trend: { day: string; count: number }[]; recent_scans: Scan[] }
type ListResponse<T> = { items: T[]; total: number; limit: number; offset: number }
type SystemState = { health?: unknown; ready?: unknown }

type ApiError = Error & { status?: number }

async function apiRequest<T>(path: string, session: Session): Promise<T> {
  const response = await fetch(`${api}${path}`, { headers: { Authorization: `Bearer ${session.access_token}` } })
  if (!response.ok) {
    const body = await response.json().catch(() => null)
    const error = new Error(body?.error?.message || `Request failed (${response.status})`) as ApiError
    error.status = response.status
    throw error
  }
  return response.json() as Promise<T>
}

const navigation: { page: Page; label: string; icon: React.ElementType }[] = [
  { page: "overview", label: "Overview", icon: LayoutDashboard },
  { page: "scans", label: "Scans", icon: FileSearch },
  { page: "users", label: "Users", icon: Users },
  { page: "vehicles", label: "Vehicles", icon: Car },
  { page: "system", label: "System", icon: Server },
]

function AuthScreen() {
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [showPassword, setShowPassword] = useState(false)
  const [message, setMessage] = useState("")
  const [isError, setIsError] = useState(false)
  const [busy, setBusy] = useState(false)

  async function googleSignIn() {
    setBusy(true); setMessage(""); setIsError(false)
    try {
      const { error } = await supabase.auth.signInWithOAuth({ provider: "google", options: { redirectTo: window.location.origin } })
      if (error) throw error
    } catch (caught) {
      setIsError(true)
      setMessage(caught instanceof Error ? caught.message : "Unable to start Google sign-in.")
      setBusy(false)
    }
  }

  async function passwordSignIn(event: React.FormEvent) {
    event.preventDefault(); setBusy(true); setMessage(""); setIsError(false)
    try {
      const { error } = await supabase.auth.signInWithPassword({ email: email.trim().toLowerCase(), password })
      if (error) throw error
    } catch (caught) {
      setIsError(true)
      setMessage(caught instanceof Error ? caught.message : "Unable to sign in.")
    } finally {
      setBusy(false)
    }
  }

  return <div className="flex min-h-svh items-center justify-center bg-muted/40 p-6">
    <Card className="w-full max-w-md">
      <CardHeader><div className="mb-3 flex size-11 items-center justify-center rounded-xl bg-primary text-primary-foreground"><Gauge /></div><CardTitle className="text-2xl">CarVision operations</CardTitle><CardDescription>Sign in with an account that has the admin role.</CardDescription></CardHeader>
      <CardContent className="space-y-4">
        <Button className="w-full" onClick={googleSignIn} disabled={busy}>Continue with Google</Button>
        <div className="flex items-center gap-3 text-xs text-muted-foreground"><div className="h-px flex-1 bg-border"/>or use email<div className="h-px flex-1 bg-border"/></div>
        <form className="space-y-3" onSubmit={passwordSignIn}>
          <Input aria-label="Administrator email" autoComplete="email" type="email" placeholder="admin@carvision.com" value={email} onChange={event => setEmail(event.target.value)} required/>
          <div className="relative">
            <Input className="pr-11" aria-label="Password" autoComplete="current-password" type={showPassword ? "text" : "password"} placeholder="Password" value={password} onChange={event => setPassword(event.target.value)} required/>
            <Button className="absolute right-1 top-1/2 -translate-y-1/2" type="button" size="icon" variant="ghost" aria-label={showPassword ? "Hide password" : "Show password"} onClick={() => setShowPassword(value => !value)}>{showPassword ? <EyeOff/> : <Eye/>}</Button>
          </div>
          <Button className="w-full" type="submit" disabled={busy}>{busy ? "Signing in..." : "Sign in"}</Button>
        </form>
        {message && <p role={isError ? "alert" : "status"} aria-live="polite" className={isError ? "text-sm text-destructive" : "text-sm text-muted-foreground"}>{message}</p>}
      </CardContent>
    </Card>
  </div>
}

function SidebarCloseOnMobile({ children, onClick }: { children: React.ReactNode; onClick: () => void }) {
  const { isMobile, setOpenMobile } = useSidebar()
  return <span className="contents" onClick={() => { onClick(); if (isMobile) setOpenMobile(false) }}>{children}</span>
}

function AdminApp({ session }: { session: Session }) {
  const [page, setPage] = useState<Page>("overview")
  const [query, setQuery] = useState("")
  const [debouncedQuery, setDebouncedQuery] = useState("")
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<ApiError | null>(null)
  const [overview, setOverview] = useState<Overview | null>(null)
  const [rows, setRows] = useState<Array<Scan | User | Vehicle>>([])
  const [total, setTotal] = useState(0)
  const [offset, setOffset] = useState(0)
  const [selected, setSelected] = useState<Scan | User | Vehicle | null>(null)
  const [system, setSystem] = useState<SystemState>({})
  const [dark, setDark] = useState(() => localStorage.getItem("carvision-theme") !== "light")
  const limit = 20

  useEffect(() => { document.documentElement.classList.toggle("dark", dark); localStorage.setItem("carvision-theme", dark ? "dark" : "light") }, [dark])
  useEffect(() => { const timer = window.setTimeout(() => { setDebouncedQuery(query.trim()); setOffset(0) }, 300); return () => window.clearTimeout(timer) }, [query])

  const load = useCallback(async () => {
    setLoading(true); setError(null); setSelected(null)
    try {
      if (page === "overview") {
        setOverview(await apiRequest<Overview>("/api/v1/admin/overview", session))
      } else if (page === "system") {
        const [health, ready] = await Promise.all([fetch(`${api}/health`).then(r => r.json()), fetch(`${api}/ready`).then(r => r.json())])
        setSystem({ health, ready })
      } else {
        const params = new URLSearchParams({ limit: String(limit), offset: String(offset) })
        if (debouncedQuery) params.set("q", debouncedQuery)
        const data = await apiRequest<ListResponse<Scan | User | Vehicle>>(`/api/v1/admin/${page}?${params}`, session)
        setRows(data.items); setTotal(data.total)
      }
    } catch (caught) { setError(caught as ApiError) } finally { setLoading(false) }
  }, [page, offset, debouncedQuery, session])

  useEffect(() => { load() }, [load])
  const title = navigation.find(item => item.page === page)?.label || "Overview"
  const filteredRecent = useMemo(() => {
    const records = overview?.recent_scans || []
    if (!debouncedQuery) return records
    const needle = debouncedQuery.toLowerCase()
    return records.filter(scan => `${scan.user_name || ""} ${scan.vehicle?.brand || ""} ${scan.vehicle?.model || ""} ${scan.status}`.toLowerCase().includes(needle))
  }, [overview, debouncedQuery])

  function navigate(next: Page) { setPage(next); setOffset(0); setQuery(""); window.location.hash = next }

  return <TooltipProvider>
    <SidebarProvider>
      <Sidebar collapsible="icon">
        <SidebarHeader className="border-b"><div className="flex h-10 items-center gap-3 px-2"><div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-primary text-primary-foreground"><Gauge className="size-4"/></div><div className="min-w-0 group-data-[collapsible=icon]:hidden"><p className="truncate font-semibold">CarVision</p><p className="truncate text-xs text-muted-foreground">Operations</p></div></div></SidebarHeader>
        <SidebarContent><SidebarGroup><SidebarGroupLabel>Workspace</SidebarGroupLabel><SidebarMenu>{navigation.map(item => { const Icon = item.icon; return <SidebarMenuItem key={item.page}><SidebarCloseOnMobile onClick={() => navigate(item.page)}><SidebarMenuButton isActive={page === item.page} tooltip={item.label}><Icon/><span>{item.label}</span></SidebarMenuButton></SidebarCloseOnMobile></SidebarMenuItem> })}</SidebarMenu></SidebarGroup></SidebarContent>
        <SidebarFooter className="border-t"><SidebarMenu><SidebarMenuItem><SidebarMenuButton onClick={() => supabase.auth.signOut()} tooltip="Sign out"><LogOut/><span>Sign out</span></SidebarMenuButton></SidebarMenuItem></SidebarMenu></SidebarFooter>
      </Sidebar>
      <SidebarInset>
        <header className="sticky top-0 z-20 flex h-16 items-center gap-3 border-b bg-background/90 px-4 backdrop-blur md:px-6">
          <SidebarTrigger><Menu/></SidebarTrigger>
          <div className="min-w-0 flex-1"><h1 className="truncate font-semibold">{title}</h1><p className="hidden text-xs text-muted-foreground sm:block">Live CarVision application data</p></div>
          <div className={page === "system" ? "hidden" : "relative hidden w-full max-w-sm md:block"}><Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"/><Input className="pl-9" aria-label="Search dashboard records" placeholder={`Search ${page === "overview" ? "recent scans" : title.toLowerCase()}...`} value={query} onChange={event => setQuery(event.target.value)}/></div>
          <Tooltip><TooltipTrigger render={<Button aria-label="Open API documentation" size="icon" variant="outline" onClick={() => window.open(`${api}/docs`, "_blank", "noopener,noreferrer")}/> }><HelpCircle/></TooltipTrigger><TooltipContent>API documentation</TooltipContent></Tooltip>
          <Tooltip><TooltipTrigger render={<Button aria-label="Refresh data" size="icon" variant="outline" onClick={load} disabled={loading}/>}>{<RefreshCw className={loading ? "animate-spin" : ""}/>}</TooltipTrigger><TooltipContent>Refresh data</TooltipContent></Tooltip>
          <Tooltip><TooltipTrigger render={<Button aria-label="Toggle theme" size="icon" variant="outline" onClick={() => setDark(value => !value)}/>}>{dark ? <Sun/> : <Moon/>}</TooltipTrigger><TooltipContent>Toggle theme</TooltipContent></Tooltip>
          <Button variant="ghost" className="hidden max-w-48 truncate sm:inline-flex" onClick={() => supabase.auth.signOut()}>{session.user.email || "Admin"}</Button>
        </header>
        <main className="mx-auto flex w-full max-w-7xl flex-1 flex-col gap-5 p-4 md:p-6">
          <div className={page === "system" ? "hidden" : "relative md:hidden"}><Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"/><Input className="pl-9" aria-label="Search dashboard records" placeholder="Search..." value={query} onChange={event => setQuery(event.target.value)}/></div>
          {error ? <ErrorState error={error} retry={load}/> : loading ? <LoadingState/> : page === "overview" ? <OverviewView data={overview} scans={filteredRecent} select={setSelected}/> : page === "system" ? <SystemView state={system}/> : <ListView page={page} rows={rows} total={total} offset={offset} limit={limit} select={setSelected} previous={() => setOffset(value => Math.max(0, value - limit))} next={() => setOffset(value => value + limit)}/>} 
          {selected && <SelectedRecord value={selected} close={() => setSelected(null)}/>} 
        </main>
      </SidebarInset>
    </SidebarProvider>
  </TooltipProvider>
}

function ErrorState({ error, retry }: { error: ApiError; retry: () => void }) {
  const forbidden = error.status === 403
  return <Card><CardHeader><CardTitle className="flex items-center gap-2"><CircleAlert className="text-destructive"/>{forbidden ? "Administrator access required" : "Unable to load dashboard"}</CardTitle><CardDescription>{forbidden ? "This account is authenticated but does not have app_metadata.role set to admin." : error.message}</CardDescription></CardHeader><CardContent className="flex gap-2"><Button onClick={retry}>Try again</Button>{forbidden && <Button variant="outline" onClick={() => supabase.auth.signOut()}>Use another account</Button>}</CardContent></Card>
}

function LoadingState() { return <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">{Array.from({ length: 8 }).map((_, index) => <Skeleton key={index} className="h-36 rounded-xl"/>)}</div> }

function MetricCard({ label, value, description }: { label: string; value: string | number; description: string }) { return <Card><CardHeader className="pb-2"><CardDescription>{label}</CardDescription><CardTitle className="font-mono text-3xl tabular-nums">{value}</CardTitle></CardHeader><CardContent className="text-xs text-muted-foreground">{description}</CardContent></Card> }

function OverviewView({ data, scans, select }: { data: Overview | null; scans: Scan[]; select: (scan: Scan | User | Vehicle) => void }) {
  if (!data) return <EmptyState text="No overview data is available."/>
  const m = data.metrics
  return <>
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4"><MetricCard label="Users" value={m.users} description="Application profiles"/><MetricCard label="Scans" value={m.scans} description={`${m.scans_last_30_days} in the last 30 days`}/><MetricCard label="Vehicles" value={m.vehicles} description={`${m.favorites} garage favorites`}/><MetricCard label="Average confidence" value={m.average_confidence == null ? "Not available" : `${Math.round(m.average_confidence * 100)}%`} description={`${m.completed_scans} completed, ${m.failed_scans} failed`}/></div>
    <Card><CardHeader><CardTitle>Scan activity</CardTitle><CardDescription>Real scans recorded during the last 30 days.</CardDescription></CardHeader><CardContent className="h-72">{data.scan_trend.length ? <ResponsiveContainer width="100%" height="100%"><AreaChart data={data.scan_trend}><defs><linearGradient id="scanFill" x1="0" y1="0" x2="0" y2="1"><stop offset="5%" stopColor="var(--color-primary)" stopOpacity={0.4}/><stop offset="95%" stopColor="var(--color-primary)" stopOpacity={0}/></linearGradient></defs><CartesianGrid vertical={false}/><XAxis dataKey="day" tickFormatter={value => String(value).slice(5)}/><YAxis allowDecimals={false}/><ChartTooltip/><Area type="monotone" dataKey="count" stroke="var(--color-primary)" fill="url(#scanFill)"/></AreaChart></ResponsiveContainer> : <EmptyState text="No scans recorded in the last 30 days."/>}</CardContent></Card>
    <RecordsTable page="scans" rows={scans} select={select}/>
  </>
}

function ListView({ page, rows, total, offset, limit, select, previous, next }: { page: Page; rows: Array<Scan | User | Vehicle>; total: number; offset: number; limit: number; select: (row: Scan | User | Vehicle) => void; previous: () => void; next: () => void }) {
  return <><div className="flex items-end justify-between"><div><h2 className="text-2xl font-semibold">{total.toLocaleString()} {page}</h2><p className="text-sm text-muted-foreground">Search and inspect records from the live database.</p></div></div>{rows.length ? <RecordsTable page={page} rows={rows} select={select}/> : <EmptyState text={`No ${page} match this search.`}/>}<div className="flex items-center justify-between"><span className="text-sm text-muted-foreground">{total ? `${offset + 1}-${Math.min(offset + limit, total)} of ${total}` : "0 records"}</span><div className="flex gap-2"><Button variant="outline" onClick={previous} disabled={offset === 0}>Previous</Button><Button variant="outline" onClick={next} disabled={offset + limit >= total}>Next</Button></div></div></>
}

function RecordsTable({ page, rows, select }: { page: Page; rows: Array<Scan | User | Vehicle>; select: (row: Scan | User | Vehicle) => void }) {
  return <Card><CardHeader><CardTitle>{page === "scans" ? "Recent scans" : page[0].toUpperCase() + page.slice(1)}</CardTitle><CardDescription>Select a row to inspect its stored fields.</CardDescription></CardHeader><CardContent className="overflow-x-auto p-0"><Table><TableHeader><TableRow>{page === "scans" ? <><TableHead>Vehicle</TableHead><TableHead>User</TableHead><TableHead>Status</TableHead><TableHead>Confidence</TableHead><TableHead>Date</TableHead></> : page === "users" ? <><TableHead>Name</TableHead><TableHead>Scans</TableHead><TableHead>Joined</TableHead></> : <><TableHead>Vehicle</TableHead><TableHead>Generation</TableHead><TableHead>Type</TableHead><TableHead>Added</TableHead></>}</TableRow></TableHeader><TableBody>{rows.map(row => {
    if ("status" in row) return <TableRow className="cursor-pointer" key={row.id} onClick={() => select(row)}><TableCell className="font-medium">{[row.vehicle?.brand, row.vehicle?.model].filter(Boolean).join(" ") || "Unidentified"}</TableCell><TableCell>{row.user_name || "Unnamed user"}</TableCell><TableCell><StatusBadge value={row.status}/></TableCell><TableCell>{row.confidence == null ? "Not available" : `${Math.round(row.confidence * 100)}%`}</TableCell><TableCell>{new Date(row.created_at).toLocaleString()}</TableCell></TableRow>
    if ("auth_user_id" in row) return <TableRow className="cursor-pointer" key={row.id} onClick={() => select(row)}><TableCell className="font-medium">{row.name || "Unnamed user"}</TableCell><TableCell>{row.scan_count}</TableCell><TableCell>{new Date(row.created_at).toLocaleDateString()}</TableCell></TableRow>
    return <TableRow className="cursor-pointer" key={row.id} onClick={() => select(row)}><TableCell className="font-medium">{row.brand} {row.model} {row.year || ""}</TableCell><TableCell>{row.generation || "Not available"}</TableCell><TableCell>{row.vehicle_type || "Not available"}</TableCell><TableCell>{new Date(row.created_at).toLocaleDateString()}</TableCell></TableRow>
  })}</TableBody></Table></CardContent></Card>
}

function StatusBadge({ value }: { value: string }) { const variant = value === "completed" ? "default" : value === "failed" ? "destructive" : "secondary"; return <Badge variant={variant}>{value}</Badge> }
function EmptyState({ text }: { text: string }) { return <div className="flex min-h-40 items-center justify-center rounded-xl border border-dashed p-8 text-center text-sm text-muted-foreground">{text}</div> }
function SelectedRecord({ value, close }: { value: Scan | User | Vehicle; close: () => void }) { return <Card className="border-primary/30"><CardHeader className="flex-row items-start justify-between"><div><CardTitle>Record details</CardTitle><CardDescription>Values returned by the admin API.</CardDescription></div><Button variant="ghost" onClick={close}>Close</Button></CardHeader><CardContent><pre className="max-h-80 overflow-auto rounded-lg bg-muted p-4 text-xs">{JSON.stringify(value, null, 2)}</pre></CardContent></Card> }
function SystemView({ state }: { state: SystemState }) { const ready = (state.ready as { status?: string })?.status === "ready"; return <div className="grid gap-4 md:grid-cols-2"><Card><CardHeader><CardTitle className="flex items-center gap-2">{state.health ? <CheckCircle2 className="text-emerald-500"/> : <CircleAlert/>}API health</CardTitle><CardDescription>Response from the public health endpoint.</CardDescription></CardHeader><CardContent><pre className="overflow-auto rounded-lg bg-muted p-4 text-xs">{JSON.stringify(state.health, null, 2)}</pre></CardContent></Card><Card><CardHeader><CardTitle className="flex items-center gap-2">{ready ? <CheckCircle2 className="text-emerald-500"/> : <CircleAlert className="text-amber-500"/>}Dependencies</CardTitle><CardDescription>Backend configuration readiness.</CardDescription></CardHeader><CardContent><pre className="overflow-auto rounded-lg bg-muted p-4 text-xs">{JSON.stringify(state.ready, null, 2)}</pre></CardContent></Card></div> }

function Root() {
  const [session, setSession] = useState<Session | null | undefined>(undefined)
  useEffect(() => {
    supabase.auth.getSession().then(({ data }) => setSession(data.session))
    const { data } = supabase.auth.onAuthStateChange((_event, next) => setSession(next))
    return () => data.subscription.unsubscribe()
  }, [])
  if (session === undefined) return <div className="flex min-h-svh items-center justify-center"><Activity className="animate-pulse"/><span className="ml-2">Loading session...</span></div>
  return session ? <AdminApp session={session}/> : <AuthScreen/>
}

createRoot(document.getElementById("root")!).render(<React.StrictMode><Root/></React.StrictMode>)
