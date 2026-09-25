import type { Etudiant } from '@/api/types'
import { Button } from '@/components/ui/button'
import type { VueEcran } from '../App'
import { EcranTableau } from './EcranTableau'

interface EcranFormateurProps {
  etudiants: Etudiant[]
  onChangerEcran: (ecran: VueEcran) => void
}

/** Écran formateur : le tableau de bord complet arrive au MODULE 8 (EF6). */
export function EcranFormateur({ etudiants, onChangerEcran }: EcranFormateurProps) {
  return (
    <div className="flex w-full max-w-2xl flex-col gap-4">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold">Espace formateur</h2>
        <Button variant="outline" onClick={() => onChangerEcran('selection')}>
          Changer d'identité
        </Button>
      </div>
      <EcranTableau etudiants={etudiants} />
    </div>
  )
}
