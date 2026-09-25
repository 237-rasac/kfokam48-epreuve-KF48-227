import { useState } from 'react'

import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { EcranEtudiant } from '@/pages/EcranEtudiant'
import { EcranFormateur } from '@/pages/EcranFormateur'
import { EcranRelecteur } from '@/pages/EcranRelecteur'
import { EcranSelection } from '@/pages/EcranSelection'
import { useReferentiel } from '@/pages/useReferentiel'
import { getEtudiantCourant } from '@/pages/selection'

/** Écrans disponibles : sélection d'identité, puis formateur / étudiant / relecteur. */
export type VueEcran = 'selection' | 'formateur' | 'etudiant' | 'relecteur'

const APP_TITLE = 'KFOKAM48 — Présences et relectures'

function App() {
  const referentiel = useReferentiel()
  // L'identité mémorisée (localStorage) pré-sélectionne l'écran étudiant au rechargement.
  const [vue, setVue] = useState<VueEcran>(getEtudiantCourant() !== null ? 'etudiant' : 'selection')

  const etudiantCourant =
    referentiel.etudiants?.find((e) => e.id === getEtudiantCourant()) ?? null

  if (referentiel.erreur) {
    return (
      <main className="mx-auto flex min-h-screen max-w-2xl flex-col items-center justify-center gap-6 p-6">
        <h1 className="text-2xl font-semibold">{APP_TITLE}</h1>
        <Card className="w-full">
          <CardHeader>
            <CardTitle>Connexion au backend</CardTitle>
            <CardDescription>L'API n'a pas pu être interrogée.</CardDescription>
          </CardHeader>
          <CardContent>
            <div aria-live="assertive" role="alert">
              <p className="text-red-700 dark:text-red-400">
                ❌ Erreur API [{referentiel.erreur.code}] : {referentiel.erreur.message}
              </p>
              <p className="mt-2 text-sm text-muted-foreground">
                Vérifie que le backend tourne : <code>docker compose up -d</code> dans{' '}
                <code>backend/</code>, puis l'API répond sur <code>http://localhost:8080</code>.
              </p>
              <Button className="mt-3" onClick={referentiel.recharger}>
                Réessayer
              </Button>
            </div>
          </CardContent>
        </Card>
      </main>
    )
  }

  if (!referentiel.promotions || !referentiel.etudiants) {
    return (
      <main className="mx-auto flex min-h-screen max-w-2xl flex-col items-center justify-center gap-6 p-6">
        <h1 className="text-2xl font-semibold">{APP_TITLE}</h1>
        <p aria-live="polite">Chargement du référentiel…</p>
      </main>
    )
  }

  return (
    <main className="mx-auto flex min-h-screen max-w-2xl flex-col items-center justify-center gap-6 p-6">
      <h1 className="text-2xl font-semibold">{APP_TITLE}</h1>

      {vue === 'selection' && (
        <EcranSelection
          promotions={referentiel.promotions}
          etudiants={referentiel.etudiants}
          onValide={() => setVue('etudiant')}
          onFormateur={() => setVue('formateur')}
        />
      )}

      {vue === 'formateur' && (
        <EcranFormateur
          promotions={referentiel.promotions}
          etudiants={referentiel.etudiants}
          onChangerEcran={setVue}
        />
      )}

      {vue === 'etudiant' && (
        <div className="flex w-full max-w-md flex-col gap-4">
          <EcranEtudiant
            nomEtudiant={etudiantCourant?.nom ?? 'étudiant'}
            onChangerEcran={setVue}
          />
          <Button variant="secondary" onClick={() => setVue('relecteur')}>
            Voir mes relectures (écran relecteur)
          </Button>
        </div>
      )}

      {vue === 'relecteur' && (
        <div className="flex w-full max-w-md flex-col gap-4">
          <EcranRelecteur onChangerEcran={setVue} />
          <Button
            variant="secondary"
            onClick={() => setVue(etudiantCourant ? 'etudiant' : 'selection')}
          >
            Retour
          </Button>
        </div>
      )}
    </main>
  )
}

export default App
