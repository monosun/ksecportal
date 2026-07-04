/**
 * Password strength rules:
 *   - Minimum 8 characters
 *   - Must include uppercase letters (A-Z)
 *   - Must include lowercase letters (a-z)
 *   - Must include numbers (0-9)
 *   - Must include special characters (!@#$ etc.)
 */

const RULES = [
  { test: (p) => p.length >= 8,            message: '8자 이상이어야 합니다.' },
  { test: (p) => /[A-Z]/.test(p),          message: '대문자(A-Z)를 포함해야 합니다.' },
  { test: (p) => /[a-z]/.test(p),          message: '소문자(a-z)를 포함해야 합니다.' },
  { test: (p) => /[0-9]/.test(p),          message: '숫자(0-9)를 포함해야 합니다.' },
  { test: (p) => /[^A-Za-z0-9]/.test(p),   message: '특수문자(!@#$ 등)를 포함해야 합니다.' },
]

/**
 * Returns the first failing rule message, or null if the password is valid.
 */
export function validatePassword(password) {
  for (const rule of RULES) {
    if (!rule.test(password)) return rule.message
  }
  return null
}

export const PASSWORD_HINT = '8자 이상, 대·소문자·숫자·특수문자를 각각 포함해야 합니다.'
