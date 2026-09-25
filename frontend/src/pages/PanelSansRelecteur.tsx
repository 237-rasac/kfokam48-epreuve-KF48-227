import { useCallback, useEffect, useState } from 'react'

import { ApiError } from '@/api/client'
import { assignerRelecteur, getExercicesSansRelecteur } from '@/api/endpoints'
import type { Etudiant, ExerciceSansRelecteur } from '@/api/types'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

interface PanelSansRelecteurProps {
  sessionId: number
  etudiants: Etudiant[]
}

/**
 * MODULE 10 (issue #22) — le formateur débloque en un clic les exercices restés
 * sans relecteur (RG8) : sélection d'un étudiant puis Assigner.
 */
export function PanelSansRelecteur({ sessionId, etudiants }: PanelSansRelecteurProps) {
  const [exercices, setExercices] = useState<ExerciceSansRelecteur[] | null>(null)
  const [choix, setChoix] = useState<Record<number, number | null>>({})
  const [enCours, setEnCours] = useState<number | null>(null)
  const [message, setMessage] = useState<{ ok: boolean; texte: string } | null>(null)

  const charger = useCallback(() => {
    getExercicesSansRelecteur(sessionId)
      .then(setExercices)
      .catch(() => setExercices([]))
  }, [sessionId])

  useEffect(() => {
    charger()
  }, [charger])

  const assigner = async (exerciceId: number) => {
    const relecteurId = choix[exerciceId]
    if (relecteurId === null || relecteurId === undefined) return
    setEnCours(exerciceId)
    setMessage(null)
    try {
      await assignerRelecteur(exerciceId, relecteurId)
      setMessage({ ok: true, texte: 'Relecteur assigné ✅' })
      setChoix((ancien) => ({ ...ancien, [exerciceId]: null }))
      charger()
    } catch (error) {
      const code = error instanceof ApiError ? error.code : 'INATTENDU'
      const detail = error instanceof ApiError ? error.message : String(error)
      setMessage({ ok: false, texte: `[${code}] ${detail}` })
    } finally {
      setEnCours(null)
    }
  }

  const nomsParId = new Map(etudiants.map((e) => [e.id, e.nom]))

  if (exercices !== null && exercices.length === 0) {
    return null // Rien à débloquer : le panel disparaît
  }

  return (
    <Card className="w-full">
      <CardHeader>
        <CardTitle>Exercices sans relecteur</CardTitle>
        <CardDescription>
          Aucun relecteur n'était disponible au dépôt (RG8) — assigne-en un manuellement.
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        {exercices === null && (
          <p aria-live="polite" className="text-sm text-muted-foreground">Chargement…</p>
        )}
        {exercices?.map((exercice) => (
          <div key={exercice.id} className="rounded-lg border p-3 flex flex-col gap-2">
            <p className="text-sm">
              Exercice #{exercice.id} de{' '}
              <strong>{nomsParId.get(exercice.etudiantId) ?? `étudiant ${exercice.etudiantId}`}</strong>
            </p>
            <p className="text-xs break-all text-muted-foreground">{exercice.lien}</p>
            <div className="flex gap-2">
              <select
                aria-label={`Relecteur pour l'exercice ${exercice.id}`}
                value={choix[exercice.id] ?? ''}
                onChange={(e) =>
                  setChoix((ancien) => ({
                    ...ancien,
                    [exercice.id]: e.target.value === '' ? null : Number(e.target.value),
                  }))
                }
                className="h-8 flex-1 rounded-lg border border-input bg-transparent px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
              >
                <option value="">— Choisir un relecteur —</option>
                {etudiants
                  .filter((e) => e.id !== exercice.etudiantId)
                  .map((e) => (
                    <option key={e.id} value={e.id}>
                      {e.nom}
                    </option>
                  ))}
              </select>
              <Button
                onClick={() => assigner(exercice.id)}
                disabled={choix[exercice.id] === null || choix[exercice.id] === undefined || enCours === exercice.id}
              >
                {enCours === exercice.id ? 'Assignation…' : 'Assigner'}
              </Button>
            </div>
          </div>
        ))}
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
      </CardContent>
    </Card>
  )
}
