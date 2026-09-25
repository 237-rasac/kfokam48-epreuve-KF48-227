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
)

beforeAll(() => serveur.listen({ onUnhandledRequest: 'error' }))
afterAll(() => serveur.close())
afterEach(() => {
  serveur.resetHandlers()
  cleanup()
  window.localStorage.clear()
})

describe('MODULE 1 — écrans', () => {
  it('affiche la sélection puis les 5 étudiants côté formateur', async () => {
    const utilisateur = userEvent.setup()
    render(<App />)

    // Écran de sélection
    expect(await screen.findByText(/Choisis ton nom/i)).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'Amina Bello' })).toBeInTheDocument()

    // Accès formateur
    await utilisateur.click(screen.getByRole('button', { name: /Je suis formateur/i }))
    expect(screen.getByText('Espace formateur')).toBeInTheDocument()
    expect(screen.getByText('Amina Bello')).toBeInTheDocument()
    expect(screen.getByText('Emma Fouda')).toBeInTheDocument()
  })

  it('mémorise l\'étudiant choisi et montre l\'écran étudiant au rechargement', async () => {
    const utilisateur = userEvent.setup()
    render(<App />)

    await utilisateur.click(await screen.findByRole('option', { name: 'Amina Bello' }))
    await utilisateur.click(screen.getByRole('button', { name: /Continuer/i }))

    expect(screen.getByText(/Bonjour Amina Bello/i)).toBeInTheDocument()
    expect(window.localStorage.getItem('kfokam48-etudiant-id')).toBe('1')
  })

  it('affiche l\'erreur normalisée { code, message } quand l\'API échoue', async () => {
    serveur.use(
      http.get('/api/promotions', () =>
        Response.json({ code: 'ERREUR_INTERNE', message: 'Erreur interne.' }, { status: 500 }),
      ),
    )
    render(<App />)
    expect(await screen.findByRole('alert')).toHaveTextContent(/ERREUR_INTERNE/)
  })
})
