import { useCallback, useEffect, useState } from 'react'

import { getTableau } from '@/api/endpoints'
import type { Etudiant, LigneTableau, Promotion } from '@/api/types'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

interface EcranTableauProps {
  promotions: Promotion[]
  etudiants: Etudiant[]
}

/**
 * MODULE 8 — EF6/RG13 : le tableau affiche par étudiant présences, exercices
 * déposés, moyenne (— si aucune note, RG21), relectures en attente et, issue
 * #20, un badge rouge si des exercices sont restés sans relecteur.
 */
export function EcranTableau({ promotions, etudiants }: EcranTableauProps) {
  const [lignes, setLignes] = useState<LigneTableau[] | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)

  const charger = useCallback(() => {
    const promotionId = promotions[0]?.id
    if (promotionId === undefined) return
    setLignes(null)
    setErreur(null)
    getTableau(promotionId)
      .then(setLignes)
      .catch(() => setErreur('Tableau indisponible pour le moment.'))
  }, [promotions])

  useEffect(() => {
    charger()
  }, [charger])

  if (erreur) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Tableau de bord</CardTitle>
        </CardHeader>
        <CardContent>
          <p role="alert" className="text-sm text-red-700 dark:text-red-400">{erreur}</p>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Tableau de bord — {promotions[0]?.nom ?? 'promotion'}</CardTitle>
        <CardDescription>
          Une ligne par étudiant : présences, exercices déposés, moyenne, relectures en attente.
        </CardDescription>
      </CardHeader>
      <CardContent>
        {lignes === null ? (
          <p aria-live="polite" className="text-sm text-muted-foreground">Chargement du tableau…</p>
        ) : lignes.length === 0 ? (
          <p className="text-sm text-muted-foreground">Aucun étudiant référencé.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-left text-muted-foreground">
                  <th className="py-2 pr-3 font-medium">Étudiant</th>
                  <th className="py-2 pr-3 font-medium">Présences</th>
                  <th className="py-2 pr-3 font-medium">Exercices</th>
                  <th className="py-2 pr-3 font-medium">Moyenne</th>
                  <th className="py-2 pr-3 font-medium">Relectures en attente</th>
                  <th className="py-2 font-medium">Sans relecteur</th>
                </tr>
              </thead>
              <tbody>
                {lignes.map((ligne) => (
                  <tr key={ligne.etudiantId} className="border-b last:border-0">
                    <td className="py-2 pr-3">{ligne.nom}</td>
                    <td className="py-2 pr-3">{ligne.presences}</td>
                    <td className="py-2 pr-3">{ligne.exercicesDeposes}</td>
                    {/* RG21 : moyenne null → « — » (pas de recalcul côté front) */}
                    <td className="py-2 pr-3 font-medium">
                      {ligne.moyenne === null ? '—' : ligne.moyenne.toFixed(2)}
                    </td>
                    <td className="py-2 pr-3">{ligne.relecturesEnAttente}</td>
                    <td className="py-2">
                      {ligne.exercicesSansRelecteur > 0 ? (
                        <span className="rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-800 dark:bg-red-900/40 dark:text-red-300">
                          {ligne.exercicesSansRelecteur}
                        </span>
                      ) : (
                        <span className="text-muted-foreground">0</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <p className="mt-2 text-xs text-muted-foreground">
              {etudiants.length} étudiant(s) dans la promotion.
            </p>
          </div>
        )}
      </CardContent>
    </Card>
  )
}
