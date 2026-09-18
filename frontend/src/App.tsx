import { FormEvent, useEffect, useMemo, useState } from 'react'
import {
  Award,
  BookOpenCheck,
  Cloud,
  Gauge,
  History,
  LogOut,
  NotebookPen,
  PlayCircle,
  ShieldCheck,
  UserCircle
} from 'lucide-react'
import {
  AuthUser,
  Dashboard,
  ErrorNotebookEntry,
  Question,
  SimulationResult,
  clearSession,
  finishSimulation,
  getDashboard,
  getErrorNotebook,
  getMe,
  getQuestions,
  getStoredToken,
  login,
  register
} from './api'

type Page = 'dashboard' | 'simulation' | 'errors'
type Theme = 'light' | 'moderate' | 'dark'

const THEME_KEY = 'sisaws-theme'

function initialTheme(): Theme {
  if (typeof window === 'undefined') return 'moderate'
  const saved = window.localStorage.getItem(THEME_KEY)
  return saved === 'light' || saved === 'moderate' || saved === 'dark' ? saved : 'moderate'
}

export default function App() {
  const [page, setPage] = useState<Page>('dashboard')
  const [theme, setTheme] = useState<Theme>(initialTheme)
  const [user, setUser] = useState<AuthUser | null>(null)
  const [checkingSession, setCheckingSession] = useState(true)
  const [dashboard, setDashboard] = useState<Dashboard | null>(null)
  const [questions, setQuestions] = useState<Question[]>([])
  const [errorEntries, setErrorEntries] = useState<ErrorNotebookEntry[]>([])
  const [index, setIndex] = useState(0)
  const [answers, setAnswers] = useState<Record<number, number>>({})
  const [result, setResult] = useState<SimulationResult | null>(null)
  const [error, setError] = useState('')

  const current = questions[index]
  const answered = Object.keys(answers).length
  const progress = questions.length ? Math.round((answered / questions.length) * 100) : 0

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    document.documentElement.style.colorScheme = theme === 'dark' ? 'dark' : 'light'
    window.localStorage.setItem(THEME_KEY, theme)
  }, [theme])

  useEffect(() => {
    if (!getStoredToken()) {
      setCheckingSession(false)
      return
    }

    getMe()
      .then(setUser)
      .catch(() => clearSession())
      .finally(() => setCheckingSession(false))
  }, [])

  useEffect(() => {
    if (user) refreshDashboard()
  }, [user])

  async function refreshDashboard() {
    try {
      setDashboard(await getDashboard())
      setError('')
    } catch {
      setError('Não foi possível carregar seu progresso. Verifique o backend.')
    }
  }

  async function startSimulation() {
    try {
      setQuestions(await getQuestions(10))
      setAnswers({})
      setIndex(0)
      setResult(null)
      setPage('simulation')
      setError('')
    } catch {
      setError('Não foi possível iniciar o simulado.')
    }
  }

  async function openErrorNotebook() {
    try {
      setErrorEntries(await getErrorNotebook())
      setPage('errors')
      setError('')
    } catch {
      setError('Não foi possível carregar o Caderno de Erros.')
    }
  }

  async function finish() {
    if (answered !== questions.length) return

    try {
      const payload = questions.map(q => ({
        questionId: q.id,
        selectedOptionIds: [answers[q.id]]
      }))
      const response = await finishSimulation(payload)
      setResult(response)
      await refreshDashboard()
    } catch {
      setError('Não foi possível corrigir o simulado.')
    }
  }

  function logout() {
    clearSession()
    setUser(null)
    setDashboard(null)
    setQuestions([])
    setErrorEntries([])
    setResult(null)
    setPage('dashboard')
    setError('')
  }

  const resultMap = useMemo(
    () => new Map(result?.results.map(r => [r.questionId, r]) ?? []),
    [result]
  )

  if (checkingSession) {
    return <div className="session-splash"><Cloud size={42}/><strong>SisAWS</strong><span>Carregando ambiente de estudos...</span></div>
  }

  if (!user) {
    return <AuthScreen theme={theme} setTheme={setTheme} onAuthenticated={setUser} />
  }

  const pageTitle =
    page === 'dashboard'
      ? `Olá, ${user.name.split(' ')[0]}`
      : page === 'simulation'
        ? 'Simulado SAA-C03'
        : 'Caderno de Erros'

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand"><Cloud size={28}/><span>SisAWS</span></div>

        <nav>
          <button className={page === 'dashboard' ? 'active' : ''} onClick={() => setPage('dashboard')}>
            <Gauge/> Dashboard
          </button>
          <button className={page === 'simulation' ? 'active' : ''} onClick={startSimulation}>
            <BookOpenCheck/> Simulados
          </button>
          <button className={page === 'errors' ? 'active' : ''} onClick={openErrorNotebook}>
            <NotebookPen/> Caderno de Erros
          </button>
          <button disabled><Award/> Certificações</button>
          <button disabled><History/> Histórico</button>
        </nav>

        <div className="sidebar-user">
          <UserCircle size={20}/>
          <div>
            <strong>{user.name}</strong>
            <span>{user.email}</span>
          </div>
        </div>
        <div className="version">v1.2 • AWS Learning Lab</div>
      </aside>

      <main>
        <header>
          <div className="header-title">
            <p className="eyebrow">AWS LEARNING PLATFORM</p>
            <h1>{pageTitle}</h1>
          </div>

          <div className="header-actions">
            <ThemeSwitcher theme={theme} setTheme={setTheme}/>
            <button className="logout-button" onClick={logout} title="Sair">
              <LogOut size={17}/> Sair
            </button>
          </div>
        </header>

        {error && <div className="alert">{error}</div>}

        {page === 'dashboard' && (
          <div className="page-content dashboard-content">
            <section className="hero">
              <div>
                <span className="pill">AWS Certified Solutions Architect</span>
                <h2>Prepare-se construindo, respondendo e revisando.</h2>
                <p>Seu progresso agora é individual. Cada simulado alimenta suas métricas e cada erro entra automaticamente no seu caderno de revisão.</p>
                <div className="hero-buttons">
                  <button className="primary" onClick={startSimulation}><PlayCircle/> Iniciar simulado</button>
                  <button className="secondary" onClick={openErrorNotebook}><NotebookPen/> Revisar erros</button>
                </div>
              </div>
              <div className="hero-mark">AWS</div>
            </section>

            <section className="cards five-cards">
              <Metric label="Banco de questões" value={dashboard?.questionBank ?? 0} />
              <Metric label="Simulados" value={dashboard?.attempts ?? 0} />
              <Metric label="Média" value={dashboard?.averageScore ?? 0} suffix="%" />
              <Metric label="Melhor nota" value={dashboard?.bestScore ?? 0} suffix="%" />
              <Metric label="Para revisar" value={dashboard?.errorNotebookCount ?? 0} />
            </section>

            <section className="panel">
              <div>
                <p className="eyebrow">ESTUDO ORIENTADO</p>
                <h3>Domine arquitetura por cenários</h3>
                <p>IAM → VPC → EC2 → S3 → RDS → Serverless → Mensageria → Containers.</p>
              </div>
              <div className="domain-list">
                <span>Segurança</span><span>Resiliência</span><span>Performance</span><span>Custos</span>
              </div>
            </section>
          </div>
        )}

        {page === 'simulation' && current && !result && (
          <div className="page-content simulation-content">
            <section className="quiz-card">
              <div className="quiz-top">
                <div><span className="pill">{current.awsService}</span><span className="difficulty">{current.difficulty}</span></div>
                <strong>{index + 1} / {questions.length}</strong>
              </div>
              <div className="progress"><span style={{width: `${progress}%`}} /></div>
              <p className="domain">{current.domain}</p>
              <h2>{current.prompt}</h2>
              <div className="options">
                {current.options.map((option, i) => (
                  <button
                    key={option.id}
                    className={answers[current.id] === option.id ? 'selected' : ''}
                    onClick={() => setAnswers({...answers, [current.id]: option.id})}
                  >
                    <b>{String.fromCharCode(65 + i)}</b>{option.text}
                  </button>
                ))}
              </div>
              <div className="quiz-actions">
                <button disabled={index === 0} onClick={() => setIndex(index - 1)}>Anterior</button>
                {index < questions.length - 1
                  ? <button className="primary" onClick={() => setIndex(index + 1)}>Próxima</button>
                  : <button className="primary" disabled={answered !== questions.length} onClick={finish}>Finalizar simulado</button>}
              </div>
            </section>
          </div>
        )}

        {page === 'simulation' && result && (
          <div className="page-content simulation-content">
            <section className="result-card">
              <p className="eyebrow">RESULTADO</p>
              <div className="score">{result.scorePercent}%</div>
              <h2>{result.correctAnswers} de {result.totalQuestions} questões corretas</h2>
              <p>As questões incorretas já foram registradas no seu Caderno de Erros.</p>
              <div className="review-list">
                {questions.map((q, idx) => {
                  const r = resultMap.get(q.id)
                  return <div className={r?.correct ? 'review ok' : 'review fail'} key={q.id}>
                    <strong>Questão {idx + 1} • {q.awsService}</strong>
                    <span>{r?.correct ? 'Correta' : 'Adicionada ao caderno'}</span>
                    <p>{r?.explanation}</p>
                  </div>
                })}
              </div>
              <div className="quiz-actions">
                <button onClick={openErrorNotebook}>Caderno de Erros</button>
                <button className="primary" onClick={startSimulation}>Novo simulado</button>
              </div>
            </section>
          </div>
        )}

        {page === 'errors' && (
          <div className="page-content notebook-content">
            <section className="notebook-card">
              <div className="notebook-heading">
                <div>
                  <p className="eyebrow">REVISÃO INTELIGENTE</p>
                  <h2>Questões que merecem sua atenção</h2>
                </div>
                <span className="notebook-count">{errorEntries.length} para revisar</span>
              </div>

              {errorEntries.length === 0 ? (
                <div className="empty-state">
                  <ShieldCheck size={46}/>
                  <h3>Seu caderno está vazio</h3>
                  <p>Quando você errar uma questão em um simulado, ela aparecerá aqui automaticamente.</p>
                  <button className="primary" onClick={startSimulation}>Fazer um simulado</button>
                </div>
              ) : (
                <div className="error-list">
                  {errorEntries.map(entry => (
                    <article className="error-entry" key={entry.id}>
                      <div className="error-entry-top">
                        <div>
                          <span className="pill">{entry.awsService}</span>
                          <span className="domain-inline">{entry.domain}</span>
                        </div>
                        <strong>{entry.wrongCount}x erro{entry.wrongCount > 1 ? 's' : ''}</strong>
                      </div>
                      <h3>{entry.prompt}</h3>
                      <div className="explanation-box">
                        <b>Revisão:</b> {entry.explanation}
                      </div>
                      <small>Último erro: {new Date(entry.lastWrongAt).toLocaleString('pt-BR')}</small>
                    </article>
                  ))}
                </div>
              )}
            </section>
          </div>
        )}
      </main>
    </div>
  )
}

function ThemeSwitcher({theme, setTheme}: {theme: Theme; setTheme: (theme: Theme) => void}) {
  return (
    <div className="theme-switcher" role="group" aria-label="Tema da interface">
      <button className={theme === 'light' ? 'active' : ''} onClick={() => setTheme('light')}>☀ <span>Light</span></button>
      <button className={theme === 'moderate' ? 'active' : ''} onClick={() => setTheme('moderate')}>◐ <span>Moderado</span></button>
      <button className={theme === 'dark' ? 'active' : ''} onClick={() => setTheme('dark')}>☾ <span>Dark</span></button>
    </div>
  )
}

function AuthScreen({
  theme,
  setTheme,
  onAuthenticated
}: {
  theme: Theme
  setTheme: (theme: Theme) => void
  onAuthenticated: (user: AuthUser) => void
}) {
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function submit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError('')

    try {
      const response = mode === 'login'
        ? await login(email, password)
        : await register(name, email, password)

      onAuthenticated(response.user)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Não foi possível autenticar.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth-shell">
      <div className="auth-topbar">
        <div className="brand auth-brand"><Cloud size={28}/><span>SisAWS</span></div>
        <ThemeSwitcher theme={theme} setTheme={setTheme}/>
      </div>

      <section className="auth-card">
        <div className="auth-intro">
          <span className="pill">AWS Learning Platform</span>
          <h1>Estude. Pratique. Evolua.</h1>
          <p>Simulados, métricas pessoais e revisão orientada para sua preparação em certificações AWS.</p>
        </div>

        <form onSubmit={submit}>
          <div className="auth-tabs">
            <button type="button" className={mode === 'login' ? 'active' : ''} onClick={() => {setMode('login'); setError('')}}>
              Entrar
            </button>
            <button type="button" className={mode === 'register' ? 'active' : ''} onClick={() => {setMode('register'); setError('')}}>
              Criar conta
            </button>
          </div>

          {mode === 'register' && (
            <label>
              Nome
              <input value={name} onChange={e => setName(e.target.value)} minLength={2} required placeholder="Seu nome"/>
            </label>
          )}

          <label>
            E-mail
            <input value={email} onChange={e => setEmail(e.target.value)} type="email" required placeholder="voce@email.com"/>
          </label>

          <label>
            Senha
            <input value={password} onChange={e => setPassword(e.target.value)} type="password" minLength={8} required placeholder="Mínimo de 8 caracteres"/>
          </label>

          {error && <div className="auth-error">{error}</div>}

          <button className="primary auth-submit" disabled={busy}>
            {busy ? 'Processando...' : mode === 'login' ? 'Entrar no SisAWS' : 'Criar minha conta'}
          </button>

          <p className="auth-note">Projeto educacional independente. Não afiliado ou endossado pela Amazon Web Services.</p>
        </form>
      </section>
    </div>
  )
}

function Metric({label, value, suffix = ''}: {label: string; value: number; suffix?: string}) {
  return <div className="metric"><span>{label}</span><strong>{value}{suffix}</strong></div>
}
