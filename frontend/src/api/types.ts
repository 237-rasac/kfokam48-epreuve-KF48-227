// Types du domaine, alignés sur api/contrat.yaml.
// Sert de référence unique pour la couche API et les futurs écrans.

export interface Promotion {
  id: number
  nom: string
}

export interface Etudiant {
  id: number
  nom: string
  promotionId: number
}

export interface Session {
  id: number
  titre: string
  code: string
  ouvertureAt: string
  expirationAt: string
  clotureAt: string | null
  promotionId: number
}

export interface Presence {
  id: number
  sessionId: number
  etudiantId: number
  source: 'ETUDIANT' | 'FORMATEUR'
  marqueeAt: string
}

export type StatutExercice = 'EN_ATTENTE' | 'RELU'

export interface Exercice {
  id: number
  sessionId: number
  etudiantId: number
  lien: string
  statut: StatutExercice
  deposeAt: string
}

/** Vue étudiant — sans identité du relecteur (EF11). */
export interface ExerciceDetail {
  id: number
  lien: string
  statut: StatutExercice
  note: number | null
  commentaire: string | null
}

export interface Relecture {
  id: number
  exerciceId: number
  relecteurId: number
  note: number | null
  commentaire: string | null
  rendueAt: string | null
}

export interface LigneTableau {
  etudiantId: number
  nom: string
  presences: number
  exercicesDeposes: number
  moyenne: number | null
  relecturesEnAttente: number
}
