// Endpoints du domaine, alignés sur api/contrat.yaml.
// Chaque fonction retourne un typé et lève une ApiError en cas de problème.

import { apiFetch } from './client'
import type {
  Etudiant,
  Exercice,
  ExerciceDetail,
  LigneTableau,
  Presence,
  Promotion,
  Relecture,
  Session,
} from './types'

// ---- Référentiel -----------------------------------------------------------

export function getPromotions(): Promise<Promotion[]> {
  return apiFetch<Promotion[]>('/promotions')
}

export function getEtudiants(promotionId?: number): Promise<Etudiant[]> {
  const query = promotionId !== undefined ? `?promotionId=${promotionId}` : ''
  return apiFetch<Etudiant[]>(`/etudiants${query}`)
}

// ---- Sessions --------------------------------------------------------------

export interface SessionCreation {
  titre: string
  promotionId: number
}

export function ouvrirSession(creation: SessionCreation): Promise<Session> {
  return apiFetch<Session>('/sessions', { method: 'POST', body: creation })
}

export function getSession(id: number): Promise<Session> {
  return apiFetch<Session>(`/sessions/${id}`)
}

export function cloturerSession(id: number): Promise<Session> {
  return apiFetch<Session>(`/sessions/${id}/cloture`, { method: 'POST' })
}

// ---- Présences -------------------------------------------------------------

export interface PresenceCreation {
  code: string
  etudiantId: number
  /** EF7 : FORMATEUR pour un ajout manuel ; absent → voie étudiant. */
  source?: 'ETUDIANT' | 'FORMATEUR'
}

export function marquerPresence(creation: PresenceCreation): Promise<Presence> {
  return apiFetch<Presence>('/presences', { method: 'POST', body: creation })
}

export function getPresencesSession(sessionId: number): Promise<Presence[]> {
  return apiFetch<Presence[]>(`/sessions/${sessionId}/presences`)
}

// ---- Exercices -------------------------------------------------------------

export interface ExerciceCreation {
  sessionId: number
  etudiantId: number
  lien: string
}

export function deposerExercice(creation: ExerciceCreation): Promise<Exercice> {
  return apiFetch<Exercice>('/exercices', { method: 'POST', body: creation })
}

export function getExercice(id: number): Promise<ExerciceDetail> {
  return apiFetch<ExerciceDetail>(`/exercices/${id}`)
}

export function remplacerLienExercice(id: number, lien: string): Promise<Exercice> {
  return apiFetch<Exercice>(`/exercices/${id}`, { method: 'PATCH', body: { lien } })
}

// ---- Relectures ------------------------------------------------------------

export interface RelectureCreation {
  note: number
  commentaire: string
  /** MODULE 6 : permet au backend de vérifier RG3 (auto-relecture) sans authentification. */
  relecteurId?: number
}

export function rendreRelecture(id: number, creation: RelectureCreation): Promise<Relecture> {
  return apiFetch<Relecture>(`/relectures/${id}`, { method: 'POST', body: creation })
}

export function getRelecturesEnAttente(etudiantId: number): Promise<Relecture[]> {
  return apiFetch<Relecture[]>(`/etudiants/${etudiantId}/relectures`)
}

// ---- Tableau de bord -------------------------------------------------------

export function getTableau(promotionId: number): Promise<LigneTableau[]> {
  return apiFetch<LigneTableau[]>(`/tableau?promotionId=${promotionId}`)
}
