// Étudiant « connecté » : pas d'authentification au périmètre (cahier des charges §3),
// l'utilisateur choisit son identité dans une liste et le choix est mémorisé localement.

const CLE = 'kfokam48-etudiant-id'

/** Identifiant de l'étudiant sélectionné, ou null si aucun choix n'a été fait. */
export function getEtudiantCourant(): number | null {
  const brut = window.localStorage.getItem(CLE)
  if (brut === null) return null
  const id = Number(brut)
  return Number.isInteger(id) && id > 0 ? id : null
}

export function setEtudiantCourant(id: number): void {
  window.localStorage.setItem(CLE, String(id))
}
