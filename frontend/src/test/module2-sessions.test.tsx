import { describe, expect, it, afterAll, afterEach, beforeAll } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http } from 'msw'
import { setupServer } from 'msw/node'
import { cleanup } from '@testing-library/react'
import App from '@/App'

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
  http.post('/api/sessions', () =>
    Response.json({
      id: 10,
      titre: 'Cours Architecture',
      code: 'A7K3P9',
      ouvertureAt: '2026-09-25T17:00:00+01:00',
      expirationAt: '2026-09-25T17:15:00+01:00',
      clotureAt: null,
      promotionId: 1,
    }),
  ),
  // MODULE 8 — l'écran formateur affiche le tableau de bord (EF6) : il faut le handler
  http.get('/api/tableau', () =>
    Response.json([
      { etudiantId: 1, nom: 'Amina Bello', presences: 0, exercicesDeposes: 0, moyenne: null, relecturesEnAttente: 0, exercicesSansRelecteur: 0, moyenneProvisoire: false },
      { etudiantId: 2, nom: 'Boris Kamdem', presences: 0, exercicesDeposes: 0, moyenne: null, relecturesEnAttente: 0, exercicesSansRelecteur: 0, moyenneProvisoire: false },
      { etudiantId: 3, nom: 'Clarisse Ngo', presences: 0, exercicesDeposes: 0, moyenne: null, relecturesEnAttente: 0, exercicesSansRelecteur: 0, moyenneProvisoire: false },
      { etudiantId: 4, nom: 'David Etoundi', presences: 0, exercicesDeposes: 0, moyenne: null, relecturesEnAttente: 0, exercicesSansRelecteur: 0, moyenneProvisoire: false },
      { etudiantId: 5, nom: 'Emma Fouda', presences: 0, exercicesDeposes: 0, moyenne: null, relecturesEnAttente: 0, exercicesSansRelecteur: 0, moyenneProvisoire: false },
    ]),
  ),
)

beforeAll(() => serveur.listen({ onUnhandledRequest: 'error' }))
afterAll(() => serveur.close())
afterEach(() => {
  serveur.resetHandlers()
  cleanup()
  window.localStorage.clear()
})

describe('MODULE 2 — ouvrir une session (formateur)', () => {
  it('ouvre une session et affiche le code avec le compte à rebours', async () => {
    const utilisateur = userEvent.setup()
    render(<App />)

    await utilisateur.click(await screen.findByRole('button', { name: /Je suis formateur/i }))

    await utilisateur.type(screen.getByLabelText(/Titre de la session/i), 'Cours Architecture')
    await utilisateur.click(screen.getByRole('button', { name: /Ouvrir la session/i }))

    expect(await screen.findByText(/Session ouverte/i)).toBeInTheDocument()
    expect(screen.getByText('A7K3P9')).toBeInTheDocument()
    expect(screen.getByText(/Expire dans/i)).toBeInTheDocument()
  })

  it('compte à rebours juste quel que soit le fuseau du serveur (dates avec décalage)', async () => {
    // Le serveur (conteneur en UTC) renvoie une date avec décalage « Z » :
    // le navigateur doit afficher ~15 minutes restantes, pas 00:00.
    const maintenant = Date.now()
    serveur.use(
      http.post('/api/sessions', () =>
        Response.json({
          id: 11,
          titre: 'Cours fuseau',
          code: 'Z9Z9Z9',
          ouvertureAt: new Date(maintenant).toISOString(),
          expirationAt: new Date(maintenant + 15 * 60_000).toISOString(),
          clotureAt: null,
          promotionId: 1,
        }),
      ),
    )
    const utilisateur = userEvent.setup()
    render(<App />)

    await utilisateur.click(await screen.findByRole('button', { name: /Je suis formateur/i }))
    await utilisateur.type(screen.getByLabelText(/Titre de la session/i), 'Cours fuseau')
    await utilisateur.click(screen.getByRole('button', { name: /Ouvrir la session/i }))

    expect(await screen.findByText(/Expire dans/i)).toHaveTextContent(/Expire dans (14:[45]\d|15:00)/)
  })

  it('affiche l erreur contractuelle CHAMP_MANQUANT en cas de 400', async () => {
    serveur.use(
      http.post('/api/sessions', () =>
        Response.json(
          { code: 'CHAMP_MANQUANT', message: 'Le titre est obligatoire.' },
          { status: 400 },
        ),
      ),
    )
    const utilisateur = userEvent.setup()
    render(<App />)

    await utilisateur.click(await screen.findByRole('button', { name: /Je suis formateur/i }))
    await utilisateur.type(screen.getByLabelText(/Titre de la session/i), 'Session sans corps')
    await utilisateur.click(screen.getByRole('button', { name: /Ouvrir la session/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/CHAMP_MANQUANT/)
  })
})
