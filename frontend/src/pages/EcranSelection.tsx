import type { Etudiant, Promotion } from '@/api/types'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { cn } from '@/lib/utils'
import { useState } from 'react'
import { getEtudiantCourant, setEtudiantCourant } from './selection'

interface EcranSelectionProps {
  promotions: Promotion[]
  etudiants: Etudiant[]
  onValide: (etudiantId: number) => void
  /** Accès direct formateur : pas de compte formateur (cahier des charges §3). */
  onFormateur: () => void
}

/**
 * Premier écran : sans authentification (périmètre du cahier des charges §3),
 * l'utilisateur choisit son identité dans une liste, mémorisée localement.
 */
export function EcranSelection({ promotions, etudiants, onValide, onFormateur }: EcranSelectionProps) {
  const [choix, setChoix] = useState<number | null>(getEtudiantCourant())

  const nomPromotion = promotions[0]?.nom ?? ''

  return (
    <Card className="w-full max-w-md">
      <CardHeader>
        <CardTitle>Qui es-tu ?</CardTitle>
        <CardDescription>
          Choisis ton nom dans la liste{nomPromotion && <> — promotion {nomPromotion}</>}.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <ul className="flex flex-col gap-2" role="listbox" aria-label="Liste des étudiants">
          {etudiants.map((etudiant) => (
            <li key={etudiant.id}>
              <button
                type="button"
                role="option"
                aria-selected={choix === etudiant.id}
                onClick={() => setChoix(etudiant.id)}
                className={cn(
                  'w-full rounded-lg border px-3 py-2 text-left text-sm transition-colors',
                  'hover:bg-muted focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none',
                  choix === etudiant.id
                    ? 'border-primary bg-primary/10 font-medium'
                    : 'border-border',
                )}
              >
                {etudiant.nom}
              </button>
            </li>
          ))}
        </ul>
        <Button
          className="mt-4 w-full"
          disabled={choix === null}
          onClick={() => {
            if (choix === null) return
            setEtudiantCourant(choix)
            onValide(choix)
          }}
        >
          Continuer
        </Button>
        <Button variant="secondary" className="mt-2 w-full" onClick={onFormateur}>
          Je suis formateur
        </Button>
      </CardContent>
    </Card>
  )
}
