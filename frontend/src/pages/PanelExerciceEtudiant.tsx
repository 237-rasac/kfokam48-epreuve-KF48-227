import { useCallback, useEffect, useState } from 'react'

import { ApiError } from '@/api/client'
import { deposerExercice, getExercice, remplacerLienExercice } from '@/api/endpoints'
import type { ExerciceDetail } from '@/api/types'
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

interface PanelExerciceEtudiantProps {
  sessionId: number
  etudiantId: number
}

type Etat =
  | { kind: 'vide' }
  | { kind: 'exercice'; exercice: ExerciceDetail; exerciceId: number }
  | { kind: 'erreur'; code: string; message: string }

const LIEN_EXEMPLE = 'https://github.com/etudiant/exercice'

/** MODULE 5 — écran étudiant : dépôt du lien, remplacement (EF10), consultation (EF11). */
export function PanelExerciceEtudiant({ sessionId, etudiantId }: PanelExerciceEtudiantProps) {
  const [lien, setLien] = useState('')
  const [etat, setEtat] = useState<Etat>({ kind: 'vide' })
  const [envoi, setEnvoi] = useState(false)
  // Dernière erreur d'API, affichée dans les deux branches du composant
  const [erreur, setErreur] = useState<{ code: string; message: string } | null>(null)

  const recharger = useCallback(() => {
    // On tente de retrouver l'exercice déjà déposé ; sinon on reste en mode dépôt.
    // Le contrat n'expose pas "exercice par session/étudiant" : le panneau garde
    // l'id en mémoire de session navigateur.
    const memorise = window.sessionStorage.getItem(`exercice-${sessionId}-${etudiantId}`)
    if (memorise) {
      getExercice(Number(memorise))
        .then((exercice) =>
          setEtat({ kind: 'exercice', exercice, exerciceId: Number(memorise) }),
        )
        .catch(() => window.sessionStorage.removeItem(`exercice-${sessionId}-${etudiantId}`))
    }
  }, [sessionId, etudiantId])

  useEffect(() => {
    recharger()
  }, [recharger])

  const deposer = async (event: React.FormEvent) => {
    event.preventDefault()
    if (lien.trim() === '') return
    setEnvoi(true)
    try {
      const exercice = await deposerExercice({ sessionId, etudiantId, lien: lien.trim() })
      window.sessionStorage.setItem(`exercice-${sessionId}-${etudiantId}`, String(exercice.id))
      const detail = await getExercice(exercice.id)
      setEtat({ kind: 'exercice', exercice: detail, exerciceId: exercice.id })
      setLien('')
    } catch (error) {
      const code = error instanceof ApiError ? error.code : 'INATTENDU'
      const detail = error instanceof ApiError ? error.message : String(error)
      setErreur({ code, message: detail })
    } finally {
      setEnvoi(false)
    }
  }

  const remplacerLien = async () => {
    if (etat.kind !== 'exercice' || lien.trim() === '') return
    setEnvoi(true)
    try {
      await remplacerLienExercice(etat.exerciceId, lien.trim())
      const detail = await getExercice(etat.exerciceId)
      setEtat({ kind: 'exercice', exercice: detail, exerciceId: etat.exerciceId })
      setLien('')
    } catch (error) {
      const code = error instanceof ApiError ? error.code : 'INATTENDU'
      const detail = error instanceof ApiError ? error.message : String(error)
      setErreur({ code, message: detail })
    } finally {
      setEnvoi(false)
    }
  }

  if (etat.kind === 'exercice') {
    const { exercice } = etat
    const enAttente = exercice.statut === 'EN_ATTENTE'
    const noteAffichee = exercice.note !== null
    return (
      <Card className="w-full">
        <CardHeader>
          <CardTitle>Mon exercice</CardTitle>
          <CardDescription>
            {noteAffichee
              ? 'Ta note est la moyenne de tes relectures — les commentaires sont ci-dessous.'
              : 'En attente de relecture.'}
          </CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-3">
          <p className="text-sm">
            Statut :{' '}
            <span
              className={
                enAttente
                  ? 'rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground'
                  : 'rounded-full bg-green-100 px-2 py-0.5 text-xs font-medium text-green-800 dark:bg-green-900/40 dark:text-green-300'
              }
            >
              {enAttente ? 'En attente de relecture' : 'Relu'}
            </span>{' '}
            {/* EF11 v2 (issue #42) : badge « note provisoire » tant qu'un seul
                relecteur a rendu — la moyenne peut encore bouger */}
            {exercice.provisoire && (
              <span
                data-testid="badge-provisoire"
                className="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800 dark:bg-amber-900/40 dark:text-amber-300"
              >
                Note provisoire ({exercice.relecturesRendues}/{exercice.relecturesAttendues} relectures rendues)
              </span>
            )}
          </p>
          <p className="text-sm break-all">
            Lien :{' '}
            <a href={exercice.lien} target="_blank" rel="noreferrer" className="underline">
              {exercice.lien}
            </a>
          </p>
          {noteAffichee && (
            <div className="rounded-lg border p-3">
              <p className="text-2xl font-bold">
                Note : {exercice.note}/20
              </p>
              {/* EF11 v2 : tous les commentaires des relectures rendues, sans
                  jamais l'identité des relecteurs (issue #42) */}
              {exercice.commentaires.length > 0 && (
                <ul className="mt-1 list-none space-y-1">
                  {exercice.commentaires.map((commentaire, index) => (
                    <li key={index} className="text-sm text-muted-foreground">
                      « {commentaire} »
                    </li>
                  ))}
                </ul>
              )}
            </div>
          )}

          {/* EF10 : remplacement du lien tant qu'aucune relecture n'a commencé */}
          {enAttente && (
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="lien-remplacement">Remplacer le lien (EF10)</Label>
              <div className="flex gap-2">
                <Input
                  id="lien-remplacement"
                  value={lien}
                  onChange={(e) => setLien(e.target.value)}
                  placeholder={LIEN_EXEMPLE}
                />
                <Button onClick={remplacerLien} disabled={lien.trim() === '' || envoi}>
                  {envoi ? 'Envoi…' : 'Remplacer'}
                </Button>
              </div>
            </div>
          )}
          {erreur && (
            <p role="alert" className="text-sm text-red-700 dark:text-red-400">
              ❌ [{erreur.code}] {erreur.message}
            </p>
          )}
        </CardContent>
      </Card>
    )
  }

  return (
    <Card className="w-full">
      <CardHeader>
        <CardTitle>Déposer mon exercice</CardTitle>
        <CardDescription>
          Colle le lien de ton dépôt (GitHub, GitLab…). Un relecteur sera tiré au sort parmi
          les présents (EF4).
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={deposer} className="flex flex-col gap-3">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="lien-exercice">Lien de l'exercice</Label>
            <Input
              id="lien-exercice"
              value={lien}
              onChange={(e) => setLien(e.target.value)}
              placeholder={LIEN_EXEMPLE}
              type="url"
              required
            />
          </div>
          <Button type="submit" disabled={lien.trim() === '' || envoi}>
            {envoi ? 'Dépôt…' : 'Déposer mon exercice'}
          </Button>
          {erreur && (
            <p role="alert" className="text-sm text-red-700 dark:text-red-400">
              ❌ [{erreur.code}] {erreur.message}
            </p>
          )}
        </form>
      </CardContent>
    </Card>
  )
}
