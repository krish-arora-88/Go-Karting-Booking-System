import { ButtonHTMLAttributes, forwardRef } from 'react'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'cyan' | 'pink-ghost' | 'dim-ghost' | 'outline'
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className = '', variant = 'cyan', children, disabled, ...props }, ref) => {
    const base =
      'inline-flex items-center justify-center gap-2 font-orbitron text-xs tracking-widest uppercase transition-all duration-200 disabled:opacity-40 disabled:cursor-not-allowed'

    const variants: Record<string, string> = {
      cyan:
        'bg-[var(--cyan)] text-[var(--bg)] px-6 py-2.5 hover:shadow-[0_0_16px_var(--cyan)] hover:-translate-y-px',
      'pink-ghost':
        'border border-[var(--pink)] text-[var(--pink)] px-6 py-2.5 hover:bg-[var(--pink)] hover:text-[var(--bg)] hover:shadow-[0_0_12px_var(--pink)]',
      'dim-ghost':
        'border border-[var(--dim)] text-[var(--dim)] px-4 py-2 hover:border-[var(--white)] hover:text-[var(--white)]',
      outline:
        'border border-[var(--cyan)] text-[var(--cyan)] px-6 py-2.5 hover:bg-[var(--cyan)] hover:text-[var(--bg)] hover:shadow-[0_0_12px_var(--cyan)]',
    }

    return (
      <button
        ref={ref}
        disabled={disabled}
        className={`${base} ${variants[variant] ?? variants.cyan} ${className}`}
        {...props}
      >
        {children}
      </button>
    )
  }
)

Button.displayName = 'Button'
