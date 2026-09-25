import { describe, expect, it, afterAll, afterEach, beforeAll } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http } from 'msw'
import { setupServer } from 'msw/node'
import { cleanup } from '@testing-library/react'
import App from '@/App'

const sessionOuverte = {
  id: 10,
  titre: 'Cours Architecture',
  code: 'A7K3P9',
  ouvertureAt: new Date().toISOString(),
  expirationAt: new Date(Date.now() + 15 * 60_000).toISOString(),
  clotureAt: null,
  promotionId: 1,
}

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
  http.post('/api/sessions', () => Response.json(sessionOuverte, { status: 201 })),
  http.post('/api/presences', async ({ request }) => {
    const corps = (await request.json()) as { etudiantId: number; source?: string }
    const presence = {
      id: 100 + corps.etudiantId,
      sessionId: 10,
      etudiantId: corps.etudiantId,
      source: corps.source ?? 'ETUDIANT',
      marqueeAt: new Date().toISOString(),
    }
    presencesStore.push(presence)
    return Response.json(presence, { status: 201 })
  }),
  http.get('/api/sessions/10/presences', () => Response.json(presencesStore)),
  // MODULE 8/10 — l'écran formateur affiche aussi le tableau (EF6) et le panel
  // « exercices sans relecteur » (RG8) : ces handlers évitent les requêtes non
  // interceptées qui créaient des alertes parasites (plusieurs role="alert").
  http.get('/api/tableau', () =>
    Response.json([
      { etudiantId: 1, nom: 'Amina Bello', presences: 0, exercicesDeposes: 0, moyenne: null, relecturesEnAttente: 0, exercicesSansRelecteur: 0 },
      { etudiantId: 2, nom: 'Boris Kamdem', presences: 0, exercicesDeposes: 0, moyenne: null, relecturesEnAttente: 0, exercicesSansRelecteur: 0 },
      { etudiantId: 3, nom: 'Clarisse Ngo', presences: 0, exercicesDeposes: 0, moyenne: null, relecturesEnAttente: 0, exercicesSansRelecteur: 0 },
      { etudiantId: 4, nom: 'David Etoundi', presences: 0, exercicesDeposes: 0, moyenne: null, relecturesEnAttente: 0, exercicesSansRelecteur: 0 },
      { etudiantId: 5, nom: 'Emma Fouda', presences: 0, exercicesDeposes: 0, moyenne: null, relecturesEnAttente: 0, exercicesSansRelecteur: 0 },
    ]),
  ),
  http.get('/api/sessions/10/exercices-sans-relecteur', () => Response.json([])),
)

// Petit store pour que GET reflète les POST (le panneau recharge la liste après ajout)
const presencesStore: Array<{
  id: number
  sessionId: number
  etudiantId: number
  source: string
  marqueeAt: string
}> = []

beforeAll(() => serveur.listen({ onUnhandledRequest: 'error' }))
afterAll(() => serveur.close())
afterEach(() => {
  serveur.resetHandlers()
  presencesStore.length = 0
  cleanup()
  window.localStorage.clear()
})

async function ouvrirSessionFormateur(utilisateur: ReturnType<typeof userEvent.setup>) {
  render(<App />)
  await utilisateur.click(await screen.findByRole('button', { name: /Je suis formateur/i }))
  await utilisateur.type(screen.getByLabelText(/Titre de la session/i), 'Cours Architecture')
  await utilisateur.click(screen.getByRole('button', { name: /Ouvrir la session/i }))
  await screen.findByText(/Session ouverte/i)
}

describe('MODULE 4 — ajout manuel d une présence (formateur)', () => {
  it('ajoute une présence manuellement et affiche le badge formateur', async () => {
    const utilisateur = userEvent.setup()
    await ouvrirSessionFormateur(utilisateur)

    await utilisateur.selectOptions(screen.getByLabelText(/Ajouter un étudiant présent/i), '3')
    await utilisateur.click(screen.getByRole('button', { name: 'Ajouter' }))

    // La présence apparaît dans la liste des présences (scopée, pas le <option> ni le tableau)
    const liste = screen.getByLabelText('Liste des présences de la session')
    expect(await within(liste).findByText('Clarisse Ngo')).toBeInTheDocument()
    expect(within(liste).getByText(/ajouté par le formateur/i)).toBeInTheDocument()
  })

  it('signale un doublon DEJA_PRESENT (RG15)', async () => {
    serveur.use(
      http.post('/api/presences', () =>
        Response.json(
          { code: 'DEJA_PRESENT', message: 'Présence déjà enregistrée.' },
          { status: 409 },
        ),
      ),
    )
    const utilisateur = userEvent.setup()
    await ouvrirSessionFormateur(utilisateur)

    await utilisateur.selectOptions(screen.getByLabelText(/Ajouter un étudiant présent/i), '2')
    await utilisateur.click(screen.getByRole('button', { name: 'Ajouter' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/DEJA_PRESENT/)
  })

  it('signale une session clôturée SESSION_CLOTUREE (RG2)', async () => {
    serveur.use(
      http.post('/api/presences', () =>
        Response.json(
          { code: 'SESSION_CLOTUREE', message: 'La session est clôturée.' },
          { status: 410 },
        ),
      ),
    )
    const utilisateur = userEvent.setup()
    await ouvrirSessionFormateur(utilisateur)

    await utilisateur.selectOptions(screen.getByLabelText(/Ajouter un étudiant présent/i), '2')
    await utilisateur.click(screen.getByRole('button', { name: 'Ajouter' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/SESSION_CLOTUREE/)
  })
})
