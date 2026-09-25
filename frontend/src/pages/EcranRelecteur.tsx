import { useCallback, useEffect, useState } from 'react'

import { ApiError } from '@/api/client'
import { getRelecturesEnAttente, rendreRelecture } from '@/api/endpoints'
import type { Relecture } from '@/api/types'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import type { VueEcran } from '../App'

interface EcranRelecteurProps {
  etudiantId: number
  onChangerEcran: (ecran: VueEcran) => void
}

/** MODULE 6 — EF5 : liste des relectures assignées et formulaire de rendu. */
export function EcranRelecteur({ etudiantId, onChangerEcran }: EcranRelecteurProps) {
  const [relectures, setRelectures] = useState<Relecture[] | null>(null)
  const [note, setNote] = useState('')
  const [commentaire, setCommentaire] = useState('')
  const [coursDenvoi, setCoursDenvoi] = useState<number | null>(null)
  const [message, setMessage] = useState<{ ok: boolean; code?: string; texte: string } | null>(null)

  const charger = useCallback(() => {
    getRelecturesEnAttente(etudiantId)
      .then(setRelectures)
      .catch(() => setRelectures([]))
  }, [etudiantId])

  useEffect(() => {
    charger()
  }, [charger])

  const rendre = async (relectureId: number) => {
    const noteNumerique = Number(note)
    if (note === '' || commentaire.trim() === '' || Number.isNaN(noteNumerique)) return
    if (!Number.isInteger(noteNumerique) || noteNumerique < 0 || noteNumerique > 20) {
      setMessage({ ok: false, code: 'NOTE_INVALIDE', texte: 'La note doit être un entier entre 0 et 20.' })
      return
    }
    setCoursDenvoi(relectureId)
    setMessage(null)
    try {
      // relecteurId permet au backend de vérifier RG3 sans authentification
      await rendreRelecture(relectureId, {
        note: noteNumerique,
        commentaire: commentaire.trim(),
        relecteurId: etudiantId,
      })
      setMessage({ ok: true, texte: 'Relecture rendue ✅ Merci !' })
      setNote('')
      setCommentaire('')
      charger()
    } catch (error) {
      if (error instanceof ApiError) {
        setMessage({ ok: false, code: error.code, texte: error.message })
      } else {
        setMessage({ ok: false, code: 'INATTENDU', texte: String(error) })
      }
    } finally {
      setCoursDenvoi(null)
    }
  }

  return (
    <Card className="w-full max-w-md">
      <CardHeader>
        <CardTitle>Mes relectures</CardTitle>
        <CardDescription>
          Les exercices à relire apparaissent ici. Note entière de 0 à 20 (RG6).
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {relectures === null && (
          <p aria-live="polite" className="text-sm text-muted-foreground">Chargement…</p>
        )}

        {relectures !== null && relectures.length === 0 && (
          <p aria-live="polite" className="text-sm text-muted-foreground">
            Aucune relecture à faire pour le moment.
          </p>
        )}

        {relectures?.map((relecture) => (
          <form
            key={relecture.id}
            onSubmit={(e) => {
              e.preventDefault()
              rendre(relecture.id)
            }}
            className="rounded-lg border p-3 flex flex-col gap-3"
          >
            <p className="text-sm">
              Exercice #{relecture.exerciceId} —{' '}
              <a
                href={`#/exercice-${relecture.exerciceId}`}
                className="underline text-muted-foreground"
              >
                voir le dépôt
              </a>
            </p>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor={`note-${relecture.id}`}>Note (0–20)</Label>
              <Input
                id={`note-${relecture.id}`}
                type="number"
                min={0}
                max={20}
                step={1}
                value={note}
                onChange={(e) => setNote(e.target.value)}
                placeholder="15"
                required
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor={`commentaire-${relecture.id}`}>Commentaire</Label>
              <textarea
                id={`commentaire-${relecture.id}`}
                value={commentaire}
                onChange={(e) => setCommentaire(e.target.value)}
                placeholder="Bon travail, attention aux cas limites."
                rows={3}
                className="w-full rounded-lg border border-input bg-transparent px-2.5 py-1.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
                required
              />
            </div>
            <Button type="submit" disabled={coursDenvoi === relecture.id}>
              {coursDenvoi === relecture.id ? 'Envoi…' : 'Rendre la relecture'}
            </Button>
          </form>
        ))}

        {message && !message.ok && (
          <div role="alert" aria-live="assertive">
            <p className="text-sm text-red-700 dark:text-red-400">
              ❌ {message.code ? `[${message.code}] ` : ''}
              {message.texte}
            </p>
          </div>
        )}
        {message?.ok && (
          <p aria-live="polite" className="text-sm font-medium text-green-700 dark:text-green-400">
            {message.texte}
          </p>
        )}

        <Button variant="outline" onClick={() => onChangerEcran('selection')}>
          Changer d'identité
        </Button>
      </CardContent>
    </Card>
  )
}
