import { useState } from 'react'

import { ApiError } from '@/api/client'
import { marquerPresence } from '@/api/endpoints'
import type { Presence } from '@/api/types'
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
import { PanelExerciceEtudiant } from './PanelExerciceEtudiant'

interface EcranEtudiantProps {
  etudiantId: number
  nomEtudiant: string
  onChangerEcran: (ecran: VueEcran) => void
}

type EtatPresence =
  | { kind: 'idle' }
  | { kind: 'envoi' }
  | { kind: 'ok'; presence: Presence }
  | { kind: 'okDejaLa' }
  | { kind: 'erreur'; code: string; message: string }

/** Écran étudiant : présence (EF2/EF12 — MODULE 3) puis exercice (MODULE 5). */
export function EcranEtudiant({ etudiantId, nomEtudiant, onChangerEcran }: EcranEtudiantProps) {
  const [code, setCode] = useState('')
  const [etat, setEtat] = useState<EtatPresence>({ kind: 'idle' })

  const presenceReussie = etat.kind === 'ok'
  const sessionId = presenceReussie ? etat.presence.sessionId : null

  const soumettre = async (event: React.FormEvent) => {
    event.preventDefault()
    if (code.trim() === '') return
    setEtat({ kind: 'envoi' })
    try {
      const presence = await marquerPresence({ code: code.trim().toUpperCase(), etudiantId })
      setEtat({ kind: 'ok', presence })
      setCode('')
    } catch (error) {
      if (error instanceof ApiError) {
        // Un étudiant déjà présent peut continuer vers son exercice
        if (error.code === 'DEJA_PRESENT') {
          setEtat({ kind: 'okDejaLa' })
        } else {
          setEtat({ kind: 'erreur', code: error.code, message: error.message })
        }
      } else {
        setEtat({ kind: 'erreur', code: 'INATTENDU', message: String(error) })
      }
    }
  }

  return (
    <div className="flex w-full max-w-md flex-col gap-4">
      <Card className="w-full">
        <CardHeader>
          <CardTitle>Bonjour {nomEtudiant} 👋</CardTitle>
          <CardDescription>
            Saisis le code de présence communiqué par le formateur.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={soumettre} className="flex flex-col gap-3">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="code-presence">Code de présence</Label>
              <Input
                id="code-presence"
                value={code}
                onChange={(e) => setCode(e.target.value.toUpperCase())}
                placeholder="A7K3P9"
                className="text-center font-mono text-lg tracking-[0.3em] uppercase"
                maxLength={10}
                required
              />
            </div>
            <Button type="submit" disabled={code.trim() === '' || etat.kind === 'envoi'}>
              {etat.kind === 'envoi' ? 'Envoi…' : 'Marquer ma présence'}
            </Button>

            {presenceReussie && (
              <p aria-live="polite" className="text-sm font-medium text-green-700 dark:text-green-400">
                Présence enregistrée ✅ Tu peux déposer ton exercice ci-dessous.
              </p>
            )}
            {etat.kind === 'okDejaLa' && (
              <p aria-live="polite" className="text-sm font-medium text-green-700 dark:text-green-400">
                Tu es déjà marqué présent ✅ Tu peux déposer ton exercice ci-dessous.
              </p>
            )}
            {etat.kind === 'erreur' && (
              <div role="alert" aria-live="assertive">
                <p className="text-sm text-red-700 dark:text-red-400">
                  ❌ [{etat.code}] {etat.message}
                </p>
                {etat.code === 'ETUDIANT_BLOQUE' && (
                  <p className="mt-1 text-xs text-muted-foreground">
                    Patientez 2 minutes avant de réessayer.
                  </p>
                )}
              </div>
            )}
          </form>

          <Button variant="outline" className="mt-3" onClick={() => onChangerEcran('selection')}>
            Changer d'identité
          </Button>
        </CardContent>
      </Card>

      {sessionId !== null && (
        <PanelExerciceEtudiant sessionId={sessionId} etudiantId={etudiantId} />
      )}
    </div>
  )
}
