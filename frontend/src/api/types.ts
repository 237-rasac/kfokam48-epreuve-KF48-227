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

/**
 * Vue étudiant v2 — sans identité des relecteurs (EF11/RG17, contrat v2.0.0).
 * note = moyenne des relectures rendues ; provisoire tant qu'une deuxième
 * relecture est attendue et pas encore rendue.
 */
export interface ExerciceDetail {
  id: number
  lien: string
  statut: StatutExercice
  note: number | null
  provisoire: boolean
  relecturesAttendues: number
  relecturesRendues: number
  commentaires: string[]
}

export interface Relecture {
  id: number
  exerciceId: number
  relecteurId: number
  note: number | null
  commentaire: string | null
  rendueAt: string | null
}

/** MODULE 10 — exercice resté sans relecteur (RG8), liste du formateur. */
export interface ExerciceSansRelecteur {
  id: number
  sessionId: number
  etudiantId: number
  lien: string
  deposeAt: string
}

export interface LigneTableau {
  etudiantId: number
  nom: string
  presences: number
  exercicesDeposes: number
  /** RG21 : null si aucune note reçue — le front affiche « — ». */
  moyenne: number | null
  relecturesEnAttente: number
  /** Issue #20 : exercices EN_ATTENTE sans relecteur. */
  exercicesSansRelecteur: number
  /** Issue #41 : la moyenne hérite du caractère provisoire d'un exercice. */
  moyenneProvisoire: boolean
}
