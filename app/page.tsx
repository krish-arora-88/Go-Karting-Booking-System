'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { Button } from '@/components/ui/Button'
import { authService } from '@/services/authService'
import toast from 'react-hot-toast'

type AuthMode = 'login' | 'signup'

export default function HomePage() {
  const router = useRouter()
  const [mode, setMode] = useState<AuthMode>('login')
  const [isLoading, setIsLoading] = useState(false)
  const [form, setForm] = useState({ username: '', password: '', confirm: '' })
  const [errors, setErrors] = useState<Record<string, string>>({})

  const update = (field: string, value: string) => {
    setForm(prev => ({ ...prev, [field]: value }))
    setErrors(prev => ({ ...prev, [field]: '' }))
  }

  const validate = (): boolean => {
    const e: Record<string, string> = {}
    if (!form.username.trim()) e.username = 'required'
    if (!form.password) e.password = 'required'
    else if (form.password.length < 5) e.password = 'min 5 chars'
    if (mode === 'signup' && form.password !== form.confirm) e.confirm = 'passwords differ'
    setErrors(e)
    return Object.keys(e).length === 0
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!validate()) return
    setIsLoading(true)
    try {
      if (mode === 'login') {
        await authService.login(form.username, form.password)
        toast.success('ACCESS GRANTED')
      } else {
        await authService.register(form.username, `${form.username}@example.com`, form.password)
        toast.success('PLAYER REGISTERED')
      }
      router.push('/dashboard')
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'AUTH FAILED'
      toast.error(msg === 'Failed to fetch' ? 'Cannot reach server — backend offline?' : msg)
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="relative min-h-screen overflow-hidden flex flex-col" style={{ background: 'var(--bg)' }}>

      {/* Hero content */}
      <div className="relative z-10 flex flex-col items-center justify-center flex-1 px-4">

        {/* Logo */}
        <div className="mb-8 text-center">
          <div
            className="font-orbitron font-black text-4xl sm:text-5xl text-neon-cyan"
            style={{ color: 'var(--cyan)' }}
          >
            ◈ APEX RACING
          </div>
          <div className="font-mono text-xs tracking-[0.3em] mt-2" style={{ color: 'var(--dim)' }}>
            GO-KART BOOKING SYSTEM
          </div>
        </div>

        {/* Terminal card */}
        <div
          className="relative w-full max-w-sm p-6"
          style={{ background: 'var(--surface)', border: '1px solid var(--border)' }}
        >
          <span className="bracket-corner tl" />
          <span className="bracket-corner tr" />
          <span className="bracket-corner bl" />
          <span className="bracket-corner br" />

          {/* Mode tabs */}
          <div className="flex mb-6 border-b" style={{ borderColor: 'var(--border)' }}>
            {(['login', 'signup'] as AuthMode[]).map(m => (
              <button
                key={m}
                onClick={() => { setMode(m); setErrors({}) }}
                className="flex-1 pb-2 font-orbitron text-[10px] tracking-widest uppercase transition-colors"
                style={{
                  color: mode === m ? 'var(--cyan)' : 'var(--dim)',
                  borderBottom: mode === m ? '1px solid var(--cyan)' : '1px solid transparent',
                  marginBottom: '-1px',
                }}
              >
                {m === 'login' ? 'SIGN IN' : 'NEW PLAYER'}
              </button>
            ))}
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
            {/* Username */}
            <div>
              <label className="font-mono text-[10px] tracking-widest uppercase" style={{ color: 'var(--dim)' }}>
                USERNAME
              </label>
              <input
                className="tron-input mt-1"
                value={form.username}
                onChange={e => update('username', e.target.value)}
                placeholder="enter handle"
                autoComplete="username"
              />
              {errors.username && (
                <p className="font-mono text-[10px] mt-1" style={{ color: 'var(--pink)' }}>✕ {errors.username}</p>
              )}
            </div>

            {/* Password */}
            <div>
              <label className="font-mono text-[10px] tracking-widest uppercase" style={{ color: 'var(--dim)' }}>
                PASSWORD
              </label>
              <input
                className="tron-input mt-1"
                type="password"
                value={form.password}
                onChange={e => update('password', e.target.value)}
                placeholder="••••••••"
                autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
              />
              {errors.password && (
                <p className="font-mono text-[10px] mt-1" style={{ color: 'var(--pink)' }}>✕ {errors.password}</p>
              )}
            </div>

            {/* Confirm (signup only) */}
            {mode === 'signup' && (
              <div>
                <label className="font-mono text-[10px] tracking-widest uppercase" style={{ color: 'var(--dim)' }}>
                  CONFIRM
                </label>
                <input
                  className="tron-input mt-1"
                  type="password"
                  value={form.confirm}
                  onChange={e => update('confirm', e.target.value)}
                  placeholder="••••••••"
                  autoComplete="new-password"
                />
                {errors.confirm && (
                  <p className="font-mono text-[10px] mt-1" style={{ color: 'var(--pink)' }}>✕ {errors.confirm}</p>
                )}
              </div>
            )}

            <Button
              type="submit"
              variant="cyan"
              disabled={isLoading}
              className="w-full mt-2 text-[10px]"
            >
              {isLoading ? '...' : mode === 'login' ? 'ENTER RACE ▶' : 'REGISTER PLAYER +'}
            </Button>
          </form>
        </div>
      </div>

      {/* Perspective grid floor */}
      <div className="grid-floor-container">
        <div className="grid-floor-inner" />
      </div>
    </div>
  )
}
