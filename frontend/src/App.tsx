import { useEffect, useState } from 'react'

import { ApiError } from '@/api/client'
import { getPromotions } from '@/api/endpoints'
import type { Promotion } from '@/api/types'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

type ApiState =
  | { kind: 'loading' }
  | { kind: 'ok'; promotions: Promotion[] }
  | { kind: 'error'; code: string; message: string }

const APP_TITLE = 'KFOKAM48 — Présences et relectures'

/** Coquille : aucun écran métier, juste la preuve que l'app parle au backend. */
function App() {
  const [state, setState] = useState<ApiState>({ kind: 'loading' })

  useEffect(() => {
    let cancelled = false
    getPromotions()
      .then((promotions) => {
        if (!cancelled) setState({ kind: 'ok', promotions })
      })
      .catch((error: unknown) => {
        if (cancelled) return
        if (error instanceof ApiError) {
          setState({ kind: 'error', code: error.code, message: error.message })
        } else {
          setState({ kind: 'error', code: 'INATTENDU', message: String(error) })
        }
      })
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <main className="mx-auto flex min-h-screen max-w-2xl flex-col items-center justify-center gap-6 p-6">
      <h1 className="text-2xl font-semibold">{APP_TITLE}</h1>

      <Card className="w-full">
        <CardHeader>
          <CardTitle>Connexion au backend</CardTitle>
          <CardDescription>
            Coquille du frontend : la couche API est en place, les écrans arrivent.
          </CardDescription>
        </CardHeader>
        <CardContent>
          {state.kind === 'loading' && <p aria-live="polite">Interrogation de l'API…</p>}

          {state.kind === 'ok' && (
            <div aria-live="polite">
              <p className="text-green-700 dark:text-green-400">
                ✅ API joignable — {state.promotions.length} promotion(s) reçue(s).
              </p>
              {state.promotions.length > 0 && (
                <ul className="mt-2 list-disc pl-5 text-sm">
                  {state.promotions.map((p) => (
                    <li key={p.id}>
                      {p.nom} <span className="text-muted-foreground">(id {p.id})</span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          )}

          {state.kind === 'error' && (
            <div aria-live="assertive" role="alert">
              <p className="text-red-700 dark:text-red-400">
                ❌ Erreur API [{state.code}] : {state.message}
              </p>
              <p className="mt-2 text-sm text-muted-foreground">
                Vérifie que le backend tourne : <code>docker compose up -d</code> dans{' '}
                <code>backend/</code>, puis l'API répond sur{' '}
                <code>http://localhost:8080</code>.
              </p>
              <Button className="mt-3" onClick={() => location.reload()}>
                Réessayer
              </Button>
            </div>
          )}
        </CardContent>
      </Card>
    </main>
  )
}

export default App
