import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import type { VueEcran } from '../App'

/** Écran relecteur : relectures assignées (MODULE 6 — à venir). */
export function EcranRelecteur({ onChangerEcran }: { onChangerEcran: (ecran: VueEcran) => void }) {
  return (
    <Card className="w-full max-w-md">
      <CardHeader>
        <CardTitle>Mes relectures</CardTitle>
        <CardDescription>Les exercices à relire apparaîtront ici.</CardDescription>
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
