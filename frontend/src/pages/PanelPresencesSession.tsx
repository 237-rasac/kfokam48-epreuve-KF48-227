import { useCallback, useEffect, useState } from 'react'

import { ApiError } from '@/api/client'
import { getPresencesSession, marquerPresence } from '@/api/endpoints'
import type { Etudiant, Presence, Session } from '@/api/types'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Label } from '@/components/ui/label'

interface PanelPresencesSessionProps {
  session: Session
  etudiants: Etudiant[]
}

/**
 * MODULE 4 + 9 — EF7/RG11 : ajout manuel tant que la session est ouverte ;
 * une fois clôturée (EF8), l'ajout est désactivé et un badge « gelée » s'affiche.
 */
export function PanelPresencesSession({ session, etudiants }: PanelPresencesSessionProps) {
  const [presences, setPresences] = useState<Presence[] | null>(null)
  const [etudiantChoisi, setEtudiantChoisi] = useState<number | null>(null)
  const [envoi, setEnvoi] = useState(false)
  const [message, setMessage] = useState<{ ok: boolean; texte: string } | null>(null)

  const cloturee = session.clotureAt !== null

  const charger = useCallback(() => {
    getPresencesSession(session.id)
      .then(setPresences)
      .catch(() => setPresences([]))
    }, [session.id])

  useEffect(() => {
    charger()
  }, [charger])

  const ajouterManuellement = async () => {
    if (etudiantChoisi === null) return
    setEnvoi(true)
    setMessage(null)
    try {
      await marquerPresence({
        code: session.code,
        etudiantId: etudiantChoisi,
        source: 'FORMATEUR',
      })
      setMessage({ ok: true, texte: 'Présence ajoutée.' })
      setEtudiantChoisi(null)
      charger()
    } catch (error) {
      const code = error instanceof ApiError ? error.code : 'INATTENDU'
      const detail = error instanceof ApiError ? error.message : String(error)
      setMessage({ ok: false, texte: `[${code}] ${detail}` })
    } finally {
      setEnvoi(false)
    }
  }

  const nomsParId = new Map(etudiants.map((e) => [e.id, e.nom]))

  return (
    <Card className="w-full">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          Présences — {session.titre}
          {cloturee && (
            <span className="rounded-full bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground">
              ❄ session gelée
            </span>
          )}
        </CardTitle>
        <CardDescription>
          {cloturee
            ? 'Session clôturée : plus aucune présence ne peut être ajoutée (EF8).'
            : 'Ajout manuel possible tant que la session n\'est pas clôturée (EF7).'}
        </CardDescription>
      </CardHeader>
      <CardContent>
        {!cloturee && (
          <div className="flex flex-col gap-2">
            <Label htmlFor="etudiant-ajout">Ajouter un étudiant présent</Label>
            <div className="flex gap-2">
              <select
                id="etudiant-ajout"
                value={etudiantChoisi ?? ''}
                onChange={(e) => setEtudiantChoisi(e.target.value === '' ? null : Number(e.target.value))}
                className="h-8 flex-1 rounded-lg border border-input bg-transparent px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
              >
                <option value="">— Choisir un étudiant —</option>
                {etudiants.map((e) => (
                  <option key={e.id} value={e.id}>
                    {e.nom}
                    {presences?.some((p) => p.etudiantId === e.id) ? ' (déjà présent)' : ''}
                  </option>
                ))}
              </select>
              <Button onClick={ajouterManuellement} disabled={etudiantChoisi === null || envoi}>
                {envoi ? 'Ajout…' : 'Ajouter'}
              </Button>
            </div>
            {message && (
              <p
                role={message.ok ? 'status' : 'alert'}
                aria-live="polite"
                className={message.ok ? 'text-sm text-green-700 dark:text-green-400' : 'text-sm text-red-700 dark:text-red-400'}
              >
                {message.ok ? '✅ ' : '❌ '}
                {message.texte}
              </p>
            )}
          </div>
        )}

        <ul className={cloturee ? 'divide-y' : 'mt-4 divide-y'} aria-label="Liste des présences de la session">
          {presences?.map((p) => (
            <li key={p.id} className="flex items-center justify-between py-2 text-sm">
              <span>{nomsParId.get(p.etudiantId) ?? `Étudiant ${p.etudiantId}`}</span>
              <span
                className={
                  p.source === 'FORMATEUR'
                    ? 'rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800 dark:bg-amber-900/40 dark:text-amber-300'
                    : 'rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground'
                }
              >
                {p.source === 'FORMATEUR' ? 'ajouté par le formateur' : 'étudiant'}
              </span>
            </li>
          ))}
          {presences !== null && presences.length === 0 && (
            <li className="py-2 text-sm text-muted-foreground">Aucune présence pour l'instant.</li>
          )}
        </ul>
      </CardContent>
    </Card>
  )
}
