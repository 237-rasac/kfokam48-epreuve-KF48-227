// Hook référentiel : charge promotions + étudiants pour les écrans MODULE 1.
// Les erreurs sont normalisées { code, message } via ApiError (ENF4).

import { useEffect, useState } from 'react'

import { ApiError } from '@/api/client'
import { getEtudiants, getPromotions } from '@/api/endpoints'
import type { Etudiant, Promotion } from '@/api/types'

export interface ErreurNormalisee {
  code: string
  message: string
}

export interface ReferentielEtat {
  promotions: Promotion[] | null
  etudiants: Etudiant[] | null
  erreur: ErreurNormalisee | null
  recharger: () => void
}

export function useReferentiel(): ReferentielEtat {
  const [promotions, setPromotions] = useState<Promotion[] | null>(null)
  const [etudiants, setEtudiants] = useState<Etudiant[] | null>(null)
  const [erreur, setErreur] = useState<ErreurNormalisee | null>(null)
  const [tentative, setTentative] = useState(0)

  useEffect(() => {
    let cancelled = false
    setErreur(null)
    Promise.all([getPromotions(), getEtudiants()])
      .then(([lesPromotions, lesEtudiants]) => {
        if (cancelled) return
        setPromotions(lesPromotions)
        setEtudiants(lesEtudiants)
      })
      .catch((error: unknown) => {
        if (cancelled) return
        if (error instanceof ApiError) {
          setErreur({ code: error.code, message: error.message })
        } else {
          setErreur({ code: 'INATTENDU', message: String(error) })
        }
      })
    return () => {
      cancelled = true
    }
  }, [tentative])

  return { promotions, etudiants, erreur, recharger: () => setTentative((t) => t + 1) }
}
