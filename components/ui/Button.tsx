import { ButtonHTMLAttributes, forwardRef } from 'react'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'cyan' | 'pink-ghost' | 'dim-ghost' | 'outline'
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className = '', variant = 'cyan', children, disabled, ...props }, ref) => {
    const base =
      'inline-flex items-center justify-center gap-2 font-orbitron text-xs tracking-widest uppercase transition-colors duration-200 disabled:opacity-40 disabled:cursor-not-allowed'

    const variants: Record<string, string> = {
      cyan: 'btn-cyan',
      'pink-ghost': 'btn-pink-ghost',
      'dim-ghost': 'btn-dim-ghost',
      outline: 'btn-outline',
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
