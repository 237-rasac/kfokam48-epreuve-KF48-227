import type { Etudiant } from '@/api/types'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

interface EcranTableauProps {
  etudiants: Etudiant[]
}

/** Liste simple des étudiants de démonstration (le vrai tableau arrive au MODULE 8). */
export function EcranTableau({ etudiants }: EcranTableauProps) {
  if (etudiants.length === 0) {
    return <p className="text-sm text-muted-foreground">Aucun étudiant référencé.</p>
  }
  return (
    <Card>
      <CardHeader>
        <CardTitle>Étudiants de la promotion</CardTitle>
        <CardDescription>
          Données de démonstration chargées par le backend (5 étudiants attendus).
        </CardDescription>
      </CardHeader>
      <CardContent>
        <ul className="divide-y">
          {etudiants.map((etudiant) => (
            <li key={etudiant.id} className="flex items-center justify-between py-2 text-sm">
              <span>{etudiant.nom}</span>
              <span className="text-muted-foreground">id {etudiant.id}</span>
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  )
}
