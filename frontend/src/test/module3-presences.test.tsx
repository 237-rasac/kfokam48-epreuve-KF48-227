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
  http.post('/api/presences', () =>
    Response.json(
      {
        id: 100,
        sessionId: 10,
        etudiantId: 1,
        source: 'ETUDIANT',
        marqueeAt: '2026-09-25T17:00:00+01:00',
      },
      { status: 201 },
    ),
  ),
)

beforeAll(() => serveur.listen({ onUnhandledRequest: 'error' }))
afterAll(() => serveur.close())
afterEach(() => {
  serveur.resetHandlers()
  cleanup()
  window.localStorage.clear()
})

async function allerAEcranEtudiant(utilisateur: ReturnType<typeof userEvent.setup>) {
  render(<App />)
  await utilisateur.click(await screen.findByRole('option', { name: 'Amina Bello' }))
  await utilisateur.click(screen.getByRole('button', { name: /Continuer/i }))
}

describe('MODULE 3 — marquer sa présence (étudiant)', () => {
  it('marque sa présence et affiche la confirmation', async () => {
    const utilisateur = userEvent.setup()
    await allerAEcranEtudiant(utilisateur)

    await utilisateur.type(screen.getByLabelText(/Code de présence/i), 'A7K3P9')
    await utilisateur.click(screen.getByRole('button', { name: /Marquer ma présence/i }))

    expect(await screen.findByText(/Présence enregistrée/i)).toBeInTheDocument()
  })

  it('affiche CODE_INCONNU pour un code erroné', async () => {
    serveur.use(
      http.post('/api/presences', () =>
        Response.json({ code: 'CODE_INCONNU', message: 'Code inconnu.' }, { status: 400 }),
      ),
    )
    const utilisateur = userEvent.setup()
    await allerAEcranEtudiant(utilisateur)

    await utilisateur.type(screen.getByLabelText(/Code de présence/i), 'ZZZZZZ')
    await utilisateur.click(screen.getByRole('button', { name: /Marquer ma présence/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/CODE_INCONNU/)
  })

  it('affiche CODE_EXPIRE pour un code expiré (RG1)', async () => {
    serveur.use(
      http.post('/api/presences', () =>
        Response.json(
          { code: 'CODE_EXPIRE', message: 'Le code de présence a expiré.' },
          { status: 410 },
        ),
      ),
    )
    const utilisateur = userEvent.setup()
    await allerAEcranEtudiant(utilisateur)

    await utilisateur.type(screen.getByLabelText(/Code de présence/i), 'A7K3P9')
    await utilisateur.click(screen.getByRole('button', { name: /Marquer ma présence/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/CODE_EXPIRE/)
  })

  it('traitage DEJA_PRESENT comme une réussite : létudiant peut déposer (MODULE 5)', async () => {
    serveur.use(
      http.post('/api/presences', () =>
        Response.json(
          { code: 'DEJA_PRESENT', message: 'Présence déjà enregistrée.' },
          { status: 409 },
        ),
      ),
    )
    const utilisateur = userEvent.setup()
    await allerAEcranEtudiant(utilisateur)

    await utilisateur.type(screen.getByLabelText(/Code de présence/i), 'A7K3P9')
    await utilisateur.click(screen.getByRole('button', { name: /Marquer ma présence/i }))

    expect(
      await screen.findByText(/Tu es déjà marqué présent/i),
    ).toBeInTheDocument()
  })

  it('affiche ETUDIANT_BLOQUE avec aide après 5 erreurs (EF12/RG14)', async () => {
    serveur.use(
      http.post('/api/presences', () =>
        Response.json(
          { code: 'ETUDIANT_BLOQUE', message: "Trop d'erreurs, réessayez dans 2 minutes." },
          { status: 429 },
        ),
      ),
    )
    const utilisateur = userEvent.setup()
    await allerAEcranEtudiant(utilisateur)

    await utilisateur.type(screen.getByLabelText(/Code de présence/i), 'ZZZZZZ')
    await utilisateur.click(screen.getByRole('button', { name: /Marquer ma présence/i }))

    const alerte = await screen.findByRole('alert')
    expect(alerte).toHaveTextContent(/ETUDIANT_BLOQUE/)
    expect(alerte).toHaveTextContent(/Patientez 2 minutes/i)
  })
})
