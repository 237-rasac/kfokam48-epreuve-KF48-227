import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import type { VueEcran } from '../App'

interface EcranEtudiantProps {
  nomEtudiant: string
  onChangerEcran: (ecran: VueEcran) => void
}

/** Écran étudiant : présence et dépôt d'exercice (MODULES 3 et 5 — à venir). */
export function EcranEtudiant({ nomEtudiant, onChangerEcran }: EcranEtudiantProps) {
  return (
    <Card className="w-full max-w-md">
      <CardHeader>
        <CardTitle>Bonjour {nomEtudiant} 👋</CardTitle>
        <CardDescription>
          La saisie du code de présence (MODULE 3) et le dépôt d'exercice (MODULE 5) arrivent ici.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <p aria-live="polite" className="text-sm text-muted-foreground">
          Écran en préparation.
        </p>
        <Button
          variant="outline"
          className="mt-4"
          onClick={() => onChangerEcran('selection')}
        >
          Changer d'identité
        </Button>
      </CardContent>
    </Card>
  )
}
