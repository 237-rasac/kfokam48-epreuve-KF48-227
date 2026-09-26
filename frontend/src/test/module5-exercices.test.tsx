import { describe, expect, it, afterAll, afterEach, beforeAll } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http } from 'msw'
import { setupServer } from 'msw/node'
import { cleanup } from '@testing-library/react'
import App from '@/App'

const session = { id: 10, titre: 'Cours', code: 'A7K3P9' }

let exerciceDepose: { id: number; lien: string; statut: string } | null = null
// État de relecture v2 : 0 = aucune rendue, 1 = une sur deux (provisoire), 2 = les deux
let relecturesRendues = 0
const relecturesAttendues = 2

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
    // Formes v2 (contrat 2.0.0) : note = moyenne des relectures rendues, flag
    // provisoire, compteurs, commentaires[] — sans identité des relecteurs
    const notes = [16, 12] // rendues dans l'ordre du tirage
    const rendues = relecturesRendues
    const moyenne = rendues === 0 ? null : rendues === 1 ? notes[0] : (notes[0] + notes[1]) / 2
    const commentaires = ['Bon travail.', 'Correct mais incomplet.'].slice(0, rendues)
    return Response.json({
      id: 55,
      lien: exerciceDepose.lien,
      statut: rendues === relecturesAttendues ? 'RELU' : 'EN_ATTENTE',
      note: moyenne,
      provisoire: rendues > 0 && rendues < relecturesAttendues,
      relecturesAttendues,
      relecturesRendues: rendues,
      commentaires,
    })
  }),
  http.patch('/api/exercices/55', async ({ request }) => {
    const corps = (await request.json()) as { lien: string }
    if (relecturesRendues > 0) {
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
  relecturesRendues = 0
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

    relecturesRendues = 1
    await utilisateur.type(screen.getByLabelText(/Remplacer le lien/i), 'https://github.com/a/2')
    await utilisateur.click(screen.getByRole('button', { name: 'Remplacer' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/RELECTURE_DEJA_COMMENCEE/)
  })

  it('affiche le badge note provisoire tant qu\'un seul relecteur a rendu (issue #42)', async () => {
    const utilisateur = userEvent.setup()
    await etudiantConnecte(utilisateur)

    await utilisateur.type(screen.getByLabelText(/Lien de l'exercice/i), 'https://github.com/a/1')
    await utilisateur.click(screen.getByRole('button', { name: /Déposer mon exercice/i }))
    await screen.findByText('Mon exercice')

    relecturesRendues = 1
    cleanup()
    render(<App />)
    await utilisateur.type(await screen.findByLabelText(/Code de présence/i), 'A7K3P9')
    await utilisateur.click(screen.getByRole('button', { name: /Marquer ma présence/i }))

    // Badge « Note provisoire (1/2 relectures rendues) » visible
    expect(await screen.findByTestId('badge-provisoire')).toHaveTextContent(/Note provisoire/)
    // Moyenne d'une seule relecture rendue affichée
    expect(screen.getByText(/Note : 16\/20/)).toBeInTheDocument()
    // Un seul commentaire visible
    expect(screen.getByText(/Bon travail\./)).toBeInTheDocument()
    expect(screen.queryByText(/Correct mais incomplet\./)).not.toBeInTheDocument()
  })

  it('affiche la moyenne définitive et les deux commentaires une fois les deux relectures rendues (issue #42)', async () => {
    const utilisateur = userEvent.setup()
    await etudiantConnecte(utilisateur)

    await utilisateur.type(screen.getByLabelText(/Lien de l'exercice/i), 'https://github.com/a/1')
    await utilisateur.click(screen.getByRole('button', { name: /Déposer mon exercice/i }))
    await screen.findByText('Mon exercice')

    relecturesRendues = 2
    cleanup()
    render(<App />)
    await utilisateur.type(await screen.findByLabelText(/Code de présence/i), 'A7K3P9')
    await utilisateur.click(screen.getByRole('button', { name: /Marquer ma présence/i }))

    expect(await screen.findByText(/Note : 14\/20/)).toBeInTheDocument() // (16+12)/2
    expect(screen.queryByTestId('badge-provisoire')).not.toBeInTheDocument()
    // Les DEUX commentaires visibles, jamais le nom des relecteurs (EF11)
    expect(screen.getByText(/Bon travail\./)).toBeInTheDocument()
    expect(screen.getByText(/Correct mais incomplet\./)).toBeInTheDocument()
    const blocExercice = screen.getByText('Mon exercice').closest('[data-slot=card]')!
    expect(blocExercice.textContent).not.toMatch(/relecteur/i)
  })

  it('affiche la note et les commentaires une fois relu, sans nom de relecteur (EF11)', async () => {
    const utilisateur = userEvent.setup()
    await etudiantConnecte(utilisateur)

    await utilisateur.type(screen.getByLabelText(/Lien de l'exercice/i), 'https://github.com/a/1')
    await utilisateur.click(screen.getByRole('button', { name: /Déposer mon exercice/i }))
    await screen.findByText('Mon exercice')

    relecturesRendues = 2
    cleanup()

    // Les relectures sont rendues : au rechargement, le détail porte la moyenne (EF11).
    // L'identité est mémorisée en localStorage : on arrive directement sur l'écran étudiant.
    render(<App />)
    await utilisateur.type(
      await screen.findByLabelText(/Code de présence/i),
      'A7K3P9',
    )
    await utilisateur.click(screen.getByRole('button', { name: /Marquer ma présence/i }))

    expect(await screen.findByText(/Note : 14\/20/)).toBeInTheDocument()
    expect(screen.getByText(/Bon travail\./)).toBeInTheDocument()
    // EF11 : le bloc exercice n'expose ni nom ni id du relecteur
    const blocExercice = screen.getByText('Mon exercice').closest('[data-slot=card]')!
    expect(blocExercice.textContent).not.toMatch(/relecteur/i)
  })
})
