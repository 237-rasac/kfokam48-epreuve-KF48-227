import { useCallback, useEffect, useState } from 'react'

import { ApiError } from '@/api/client'
import { getRelecturesEnAttente, getRelecturesRendues, rendreRelecture } from '@/api/endpoints'
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

/** MODULE 6+7 — rendre une relecture (EF5) et la modifier avant clôture (EF9/RG7). */
export function EcranRelecteur({ etudiantId, onChangerEcran }: EcranRelecteurProps) {
  const [enAttente, setEnAttente] = useState<Relecture[] | null>(null)
  const [rendues, setRendues] = useState<Relecture[] | null>(null)
  const [note, setNote] = useState('')
  const [commentaire, setCommentaire] = useState('')
  const [enCours, setEnCours] = useState<number | null>(null)
  const [editionId, setEditionId] = useState<number | null>(null)
  const [message, setMessage] = useState<{ ok: boolean; code?: string; texte: string } | null>(null)

  const charger = useCallback(() => {
    getRelecturesEnAttente(etudiantId).then(setEnAttente).catch(() => setEnAttente([]))
    getRelecturesRendues(etudiantId).then(setRendues).catch(() => setRendues([]))
  }, [etudiantId])

  useEffect(() => {
    charger()
  }, [charger])

  const soumettre = async (relectureId: number) => {
    const noteNumerique = Number(note)
    if (note === '' || commentaire.trim() === '' || Number.isNaN(noteNumerique)) return
    if (!Number.isInteger(noteNumerique) || noteNumerique < 0 || noteNumerique > 20) {
      setMessage({ ok: false, code: 'NOTE_INVALIDE', texte: 'La note doit être un entier entre 0 et 20.' })
      return
    }
    setEnCours(relectureId)
    setMessage(null)
    try {
      // relecteurId permet au backend de vérifier RG3 sans authentification
      await rendreRelecture(relectureId, {
        note: noteNumerique,
        commentaire: commentaire.trim(),
        relecteurId: etudiantId,
      })
      setMessage(
        editionId === null
          ? { ok: true, texte: 'Relecture rendue ✅ Merci !' }
          : { ok: true, texte: 'Relecture modifiée ✅ L\'ancienne valeur est conservée dans l\'historique.' },
      )
      setNote('')
      setCommentaire('')
      setEditionId(null)
      charger()
    } catch (error) {
      if (error instanceof ApiError) {
        setMessage({ ok: false, code: error.code, texte: error.message })
      } else {
        setMessage({ ok: false, code: 'INATTENDU', texte: String(error) })
      }
    } finally {
      setEnCours(null)
    }
  }

  const demarrerEdition = (relecture: Relecture) => {
    setEditionId(relecture.id)
    setNote(relecture.note === null ? '' : String(relecture.note))
    setCommentaire(relecture.commentaire ?? '')
    setMessage(null)
  }

  const annulerEdition = () => {
    setEditionId(null)
    setNote('')
    setCommentaire('')
    setMessage(null)
  }

  return (
    <Card className="w-full max-w-md">
      <CardHeader>
        <CardTitle>Mes relectures</CardTitle>
        <CardDescription>
          Note entière de 0 à 20 (RG6). Tu peux modifier ta note tant que la session n'est pas
          clôturée (EF9) — l'ancienne valeur reste dans l'historique.
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {enAttente === null && rendues === null && (
          <p aria-live="polite" className="text-sm text-muted-foreground">Chargement…</p>
        )}

        {enAttente !== null && enAttente.length === 0 && editionId === null && (
          <p className="text-sm text-muted-foreground">Aucune relecture à faire pour le moment.</p>
        )}

        {enAttente?.map((relecture) => (
          <form
            key={relecture.id}
            onSubmit={(e) => {
              e.preventDefault()
              soumettre(relecture.id)
            }}
            className="rounded-lg border p-3 flex flex-col gap-3"
          >
            <p className="text-sm font-medium">Exercice #{relecture.exerciceId}</p>
            <ChampsNoteCommentaire
              idSuffixe={String(relecture.id)}
              note={note}
              setNote={setNote}
              commentaire={commentaire}
              setCommentaire={setCommentaire}
            />
            <Button type="submit" disabled={enCours === relecture.id}>
              {enCours === relecture.id ? 'Envoi…' : 'Rendre la relecture'}
            </Button>
          </form>
        ))}

        {/* MODULE 7 — EF9 : relectures rendues, modifiables avant clôture */}
        {rendues !== null && rendues.length > 0 && (
          <div className="flex flex-col gap-3">
            <p className="text-sm font-semibold">Déjà rendues</p>
            {rendues.map((relecture) => (
              <div key={relecture.id} className="rounded-lg border p-3 flex flex-col gap-2">
                <p className="text-sm">
                  Exercice #{relecture.exerciceId} — note <strong>{relecture.note}/20</strong>
                </p>
                {relecture.commentaire && (
                  <p className="text-sm text-muted-foreground">« {relecture.commentaire} »</p>
                )}
                {editionId === relecture.id ? (
                  <form
                    onSubmit={(e) => {
                      e.preventDefault()
                      soumettre(relecture.id)
                    }}
                    className="flex flex-col gap-3"
                  >
                    <ChampsNoteCommentaire
                      idSuffixe={`modif-${relecture.id}`}
                      note={note}
                      setNote={setNote}
                      commentaire={commentaire}
                      setCommentaire={setCommentaire}
                    />
                    <div className="flex gap-2">
                      <Button type="submit" disabled={enCours === relecture.id}>
                        {enCours === relecture.id ? 'Envoi…' : 'Enregistrer'}
                      </Button>
                      <Button type="button" variant="outline" onClick={annulerEdition}>
                        Annuler
                      </Button>
                    </div>
                  </form>
                ) : (
                  <Button variant="outline" onClick={() => demarrerEdition(relecture)}>
                    Modifier
                  </Button>
                )}
              </div>
            ))}
          </div>
        )}

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

function ChampsNoteCommentaire({
  idSuffixe,
  note,
  setNote,
  commentaire,
  setCommentaire,
}: {
  idSuffixe: string
  note: string
  setNote: (v: string) => void
  commentaire: string
  setCommentaire: (v: string) => void
}) {
  return (
    <>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor={`note-${idSuffixe}`}>Note (0–20)</Label>
        <Input
          id={`note-${idSuffixe}`}
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
        <Label htmlFor={`commentaire-${idSuffixe}`}>Commentaire</Label>
        <textarea
          id={`commentaire-${idSuffixe}`}
          value={commentaire}
          onChange={(e) => setCommentaire(e.target.value)}
          placeholder="Bon travail, attention aux cas limites."
          rows={3}
          className="w-full rounded-lg border border-input bg-transparent px-2.5 py-1.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
          required
        />
      </div>
    </>
  )
}
