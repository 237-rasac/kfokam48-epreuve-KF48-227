// Couche API dédiée : tout appel HTTP du frontend passe par ici.
// Points d'extension : base URL configurable, timeout, gestion d'erreur unifiée (ENF4).

const BASE_URL: string =
  (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '/api'

/** Erreur normalisée, conforme au contrat d'erreur { code, message } (ENF4). */
export class ApiError extends Error {
  readonly status: number
  readonly code: string

  constructor(status: number, code: string, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

export interface ApiOptions {
  method?: 'GET' | 'POST' | 'PATCH'
  body?: unknown
  signal?: AbortSignal
}

const DEFAULT_TIMEOUT_MS = 10_000

export async function apiFetch<T>(path: string, options: ApiOptions = {}): Promise<T> {
  const { method = 'GET', body, signal } = options

  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), DEFAULT_TIMEOUT_MS)
  // Si l'appelant fournit son propre signal, on l'enchaîne au timeout
  const onCallerAbort = () => controller.abort()
  signal?.addEventListener('abort', onCallerAbort)

  try {
    const response = await fetch(`${BASE_URL}${path}`, {
      method,
      headers: {
        Accept: 'application/json',
        ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal: controller.signal,
    })

    if (!response.ok) {
      // Le contrat impose { code, message } pour toute erreur (ENF4)
      let code = 'ERREUR_INTERNE'
      let message = `Erreur ${response.status}`
      try {
        const payload = (await response.json()) as { code?: string; message?: string }
        if (payload?.code) code = payload.code
        if (payload?.message) message = payload.message
      } catch {
        // réponse non JSON (ex. 500 HTML) : on garde les valeurs par défaut
      }
      throw new ApiError(response.status, code, message)
    }

    if (response.status === 204) {
      return undefined as T
    }
    return (await response.json()) as T
  } catch (error) {
    if (error instanceof ApiError) throw error
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw new ApiError(0, 'TIMEOUT', 'Le serveur ne répond pas (délai dépassé).')
    }
    // Réseau coupé, CORS, serveur éteint…
    throw new ApiError(0, 'RESEAU_INJOIGNABLE', 'Impossible de joindre le serveur.')
  } finally {
    clearTimeout(timeout)
    signal?.removeEventListener('abort', onCallerAbort)
  }
}
