import { describe, expect, it, afterAll, afterEach, beforeAll } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http } from 'msw'
import { setupServer } from 'msw/node'
import { cleanup } from '@testing-library/react'
import App from '@/App'

const session = { id: 10, titre: 'Cours', code: 'A7K3P9' }

let exerciceDepose: { id: number; lien: string; statut: string } | null = null
let relectureRendue = false

const serveur = setupServer(
  http.get('/api/promotions', () => Response.json([{ id: 1, nom: 'KFOKAM48' }])),
  http.get('/api/etudiants', () =>
    Response.json([
      { id: 1, nom: 'Amina Bello', promotionId: 1 },
      { id: 2, nom: 'Boris Kamdem', promotionId: 1 },
      { id: 3, nom: 'Clarisse Ngo', promotionId: 1 },
      { id: 4, nom: 'David Etoundi', promotionId: 1 },
      { id: 5, nom: 'Emma Fouda', promotionId: 1 },
    ]),
  ),
  http.post('/api/presences', () =>
    Response.json(
      {
        id: 100, sessionId: session.id, etudiantId: 1, source: 'ETUDIANT',
        marqueeAt: new Date().toISOString(),
      },
      { status: 201 },
    ),
  ),
  http.post('/api/exercices', async ({ request }) => {
    const corps = (await request.json()) as { lien: string }
    if (corps.lien.includes('mauvais')) {
      return Response.json(
        { code: 'LIEN_INVALIDE', message: 'Le lien fourni est invalide.' },
        { status: 400 },
      )
    }
    exerciceDepose = { id: 55, lien: corps.lien, statut: 'EN_ATTENTE' }
    return Response.json(
      { id: 55, sessionId: session.id, etudiantId: 1, lien: corps.lien, statut: 'EN_ATTENTE' },
      { status: 201 },
    )
  }),
  http.get('/api/exercices/55', () => {
    if (!exerciceDepose) return new Response(null, { status: 404 })
    return Response.json({
      id: 55,
      lien: exerciceDepose.lien,
      // Le statut suit l'état de la relecture (RELU dès qu'elle est rendue)
      statut: relectureRendue ? 'RELU' : exerciceDepose.statut,
      note: relectureRendue ? 15 : null,
      commentaire: relectureRendue ? 'Bon travail.' : null,
    })
  }),
  http.patch('/api/exercices/55', async ({ request }) => {
    const corps = (await request.json()) as { lien: string }
    if (relectureRendue) {
      return Response.json(
        { code: 'RELECTURE_DEJA_COMMENCEE', message: 'Une relecture a déjà commencé.' },
        { status: 409 },
      )
    }
    exerciceDepose = { id: 55, lien: corps.lien, statut: 'EN_ATTENTE' }
    return Response.json({ id: 55, lien: corps.lien, statut: 'EN_ATTENTE' })
  }),
)

beforeAll(() => serveur.listen({ onUnhandledRequest: 'error' }))
afterAll(() => serveur.close())
afterEach(() => {
  serveur.resetHandlers()
  exerciceDepose = null
  relectureRendue = false
  cleanup()
  window.localStorage.clear()
  window.sessionStorage.clear()
})

async function etudiantConnecte(utilisateur: ReturnType<typeof userEvent.setup>) {
  render(<App />)
  await utilisateur.click(await screen.findByRole('option', { name: 'Amina Bello' }))
  await utilisateur.click(screen.getByRole('button', { name: /Continuer/i }))
  await utilisateur.type(screen.getByLabelText(/Code de présence/i), 'A7K3P9')
  await utilisateur.click(screen.getByRole('button', { name: /Marquer ma présence/i }))
  await screen.findByText(/Présence enregistrée/i)
}

describe('MODULE 5 — dépôt et suivi de l\'exercice (étudiant)', () => {
  it('dépose son exercice et voit son statut EN_ATTENTE', async () => {
    const utilisateur = userEvent.setup()
    await etudiantConnecte(utilisateur)

    await utilisateur.type(
      screen.getByLabelText(/Lien de l'exercice/i),
      'https://github.com/amina/exo',
    )
    await utilisateur.click(screen.getByRole('button', { name: /Déposer mon exercice/i }))

    expect(await screen.findByText('Mon exercice')).toBeInTheDocument()
    expect(screen.getByText('En attente de relecture')).toBeInTheDocument()
    expect(
      screen.getByText('https://github.com/amina/exo', { selector: 'a' }),
    ).toBeInTheDocument()
  })

  it('affiche LIEN_INVALIDE sur un lien refusé par le backend', async () => {
    const utilisateur = userEvent.setup()
    await etudiantConnecte(utilisateur)

    await utilisateur.type(
      screen.getByLabelText(/Lien de l'exercice/i),
      'https://mauvais-lien',
    )
    await utilisateur.click(screen.getByRole('button', { name: /Déposer mon exercice/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/LIEN_INVALIDE/)
  })

  it('remplace le lien tant qu\'aucune relecture n\'a commencé (EF10)', async () => {
    const utilisateur = userEvent.setup()
    await etudiantConnecte(utilisateur)

    await utilisateur.type(screen.getByLabelText(/Lien de l'exercice/i), 'https://github.com/a/1')
    await utilisateur.click(screen.getByRole('button', { name: /Déposer mon exercice/i }))
    await screen.findByText('Mon exercice')

    await utilisateur.type(screen.getByLabelText(/Remplacer le lien/i), 'https://github.com/a/2')
    await utilisateur.click(screen.getByRole('button', { name: 'Remplacer' }))

    expect(
      await screen.findByText('https://github.com/a/2', { selector: 'a' }),
    ).toBeInTheDocument()
  })

  it('refuse le remplacement après relecture : RELECTURE_DEJA_COMMENCEE (RG10)', async () => {
    const utilisateur = userEvent.setup()
    await etudiantConnecte(utilisateur)

    await utilisateur.type(screen.getByLabelText(/Lien de l'exercice/i), 'https://github.com/a/1')
    await utilisateur.click(screen.getByRole('button', { name: /Déposer mon exercice/i }))
    await screen.findByText('Mon exercice')

    relectureRendue = true
    await utilisateur.type(screen.getByLabelText(/Remplacer le lien/i), 'https://github.com/a/2')
    await utilisateur.click(screen.getByRole('button', { name: 'Remplacer' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/RELECTURE_DEJA_COMMENCEE/)
  })

  it('affiche la note et le commentaire une fois relu, sans nom de relecteur (EF11)', async () => {
    const utilisateur = userEvent.setup()
    await etudiantConnecte(utilisateur)

    await utilisateur.type(screen.getByLabelText(/Lien de l'exercice/i), 'https://github.com/a/1')
    await utilisateur.click(screen.getByRole('button', { name: /Déposer mon exercice/i }))
    await screen.findByText('Mon exercice')

    relectureRendue = true
    cleanup()

    // La relecture est rendue : au rechargement, le détail porte la note (EF11).
    // L'identité est mémorisée en localStorage : on arrive directement sur l'écran étudiant.
    render(<App />)
    await utilisateur.type(
      await screen.findByLabelText(/Code de présence/i),
      'A7K3P9',
    )
    await utilisateur.click(screen.getByRole('button', { name: /Marquer ma présence/i }))

    expect(await screen.findByText(/Note : 15\/20/)).toBeInTheDocument()
    expect(screen.getByText(/Bon travail\./)).toBeInTheDocument()
    // EF11 : le bloc exercice n'expose ni nom ni id du relecteur
    const blocExercice = screen.getByText('Mon exercice').closest('[data-slot=card]')!
    expect(blocExercice.textContent).not.toMatch(/relecteur/i)
  })
})
