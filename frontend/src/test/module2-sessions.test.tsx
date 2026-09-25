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
      ouvertureAt: '2026-09-25T17:00:00',
      expirationAt: '2026-09-25T17:15:00',
      clotureAt: null,
      promotionId: 1,
    }),
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
