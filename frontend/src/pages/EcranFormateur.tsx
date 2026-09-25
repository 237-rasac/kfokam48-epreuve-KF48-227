import { useEffect, useState } from 'react'

import type { Promotion, Session } from '@/api/types'
import { cloturerSession, ouvrirSession } from '@/api/endpoints'
import { ApiError } from '@/api/client'
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
import { EcranTableau } from './EcranTableau'
import { PanelPresencesSession } from './PanelPresencesSession'

interface EcranFormateurProps {
  promotions: Promotion[]
  etudiants: import('@/api/types').Etudiant[]
  /** Session ouverte en cours : le tableau complet la remplace après ouverture. */
  onChangerEcran: (ecran: VueEcran) => void
}

type EtatSession =
  | { kind: 'idle' }
  | { kind: 'creation' }
  | { kind: 'ok'; session: Session }
  | { kind: 'erreur'; code: string; message: string }

/** Compte à rebours avant expiration du code (RG1 — 15 minutes). */
function useCompteARebours(expirationAt: string | null): string {
  const [reste, setReste] = useState('--:--')
  useEffect(() => {
    if (!expirationAt) return
    const cible = new Date(expirationAt).getTime()
    const calculer = () => {
      const secondes = Math.max(0, Math.floor((cible - Date.now()) / 1000))
      const m = Math.floor(secondes / 60)
      const s = secondes % 60
      setReste(`${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`)
    }
    calculer()
    const interval = setInterval(calculer, 1000)
    return () => clearInterval(interval)
  }, [expirationAt])
  return reste
}

function CarteSession({ session, onCloture }: { session: Session; onCloture: (session: Session) => void }) {
  const reste = useCompteARebours(session.expirationAt)
  const [confirmation, setConfirmation] = useState(false)
  const [enCours, setEnCours] = useState(false)
  const [erreur, setErreur] = useState<string | null>(null)
  const cloturee = session.clotureAt !== null

  const cloturer = async () => {
    setEnCours(true)
    setErreur(null)
    try {
      const sessionCloturee = await cloturerSession(session.id)
      onCloture(sessionCloturee)
    } catch (error) {
      const code = error instanceof ApiError ? error.code : 'INATTENDU'
      const detail = error instanceof ApiError ? error.message : String(error)
      setErreur(`[${code}] ${detail}`)
    } finally {
      setEnCours(false)
      setConfirmation(false)
    }
  }

  return (
    <Card className="w-full">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          {cloturee ? 'Session clôturée : ' : 'Session ouverte : '}
          {session.titre}
          {cloturee && (
            <span className="rounded-full bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground">
              ❄ gelée
            </span>
          )}
        </CardTitle>
        <CardDescription>
          {cloturee
            ? 'Plus aucune présence, dépôt ou relecture n\'est accepté (EF8).'
            : 'Transmets ce code à tes étudiants.'}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <p className="text-center font-mono text-4xl font-bold tracking-[0.3em]" aria-live="polite">
          {session.code}
        </p>
        {!cloturee && (
          <>
            <p className="mt-3 text-center text-sm text-muted-foreground">
              Expire dans <span className="font-semibold text-foreground">{reste}</span> ·{' '}
              {new Date(session.expirationAt).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })}
            </p>
            {!confirmation ? (
              <Button
                variant="destructive"
                className="mt-4 w-full"
                onClick={() => setConfirmation(true)}
              >
                Clôturer la session
              </Button>
            ) : (
              <div className="mt-4 flex gap-2">
                <Button variant="destructive" className="flex-1" onClick={cloturer} disabled={enCours}>
                  {enCours ? 'Clôture…' : 'Confirmer la clôture'}
                </Button>
                <Button variant="outline" className="flex-1" onClick={() => setConfirmation(false)}>
                  Annuler
                </Button>
              </div>
            )}
            {erreur && (
              <p role="alert" className="mt-2 text-sm text-red-700 dark:text-red-400">❌ {erreur}</p>
            )}
          </>
        )}
      </CardContent>
    </Card>
  )
}

/** Écran formateur : ouvrir une session et obtenir un code (EF1, MODULE 2). */
export function EcranFormateur({ promotions, etudiants, onChangerEcran }: EcranFormateurProps) {
  const [titre, setTitre] = useState('')
  const [promotionId, setPromotionId] = useState<number | null>(promotions[0]?.id ?? null)
  const [etat, setEtat] = useState<EtatSession>({ kind: 'idle' })

  const soumettre = async (event: React.FormEvent) => {
    event.preventDefault()
    if (titre.trim() === '' || promotionId === null) return
    setEtat({ kind: 'creation' })
    try {
      const session = await ouvrirSession({ titre: titre.trim(), promotionId })
      setEtat({ kind: 'ok', session })
      setTitre('')
    } catch (error) {
      if (error instanceof ApiError) {
        setEtat({ kind: 'erreur', code: error.code, message: error.message })
      } else {
        setEtat({ kind: 'erreur', code: 'INATTENDU', message: String(error) })
      }
    }
  }

  return (
    <div className="flex w-full max-w-2xl flex-col gap-4">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold">Espace formateur</h2>
        <Button variant="outline" onClick={() => onChangerEcran('selection')}>
          Changer d'identité
        </Button>
      </div>

      <Card className="w-full">
        <CardHeader>
          <CardTitle>Ouvrir une session</CardTitle>
          <CardDescription>
            Un code de présence valable 15 minutes est généré (RG1).
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={soumettre} className="flex flex-col gap-3">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="titre-session">Titre de la session</Label>
              <Input
                id="titre-session"
                value={titre}
                onChange={(e) => setTitre(e.target.value)}
                placeholder="Cours Java — 12 mars"
                required
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="promotion-session">Promotion</Label>
              <select
                id="promotion-session"
                value={promotionId ?? ''}
                onChange={(e) => setPromotionId(Number(e.target.value))}
                className="h-8 rounded-lg border border-input bg-transparent px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
              >
                {promotions.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.nom}
                  </option>
                ))}
              </select>
            </div>
            <Button type="submit" disabled={titre.trim() === '' || etat.kind === 'creation'}>
              {etat.kind === 'creation' ? 'Création…' : 'Ouvrir la session'}
            </Button>
            {etat.kind === 'erreur' && (
              <p role="alert" className="text-sm text-red-700 dark:text-red-400">
                ❌ Erreur [{etat.code}] : {etat.message}
              </p>
            )}
          </form>
        </CardContent>
      </Card>

      {etat.kind === 'ok' && (
        <>
          <CarteSession
            session={etat.session}
            onCloture={(sessionCloturee) => setEtat({ kind: 'ok', session: sessionCloturee })}
          />
          <PanelPresencesSession session={etat.session} etudiants={etudiants} />
        </>
      )}

      {/* MODULE 8 — EF6 : tableau de bord par étudiant, servi par l'API */}
      <EcranTableau promotions={promotions} etudiants={etudiants} />
    </div>
  )
}
