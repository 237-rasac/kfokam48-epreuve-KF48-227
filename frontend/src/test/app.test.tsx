import { describe, expect, it, afterAll, afterEach, beforeAll } from 'vitest'
import { render, screen } from '@testing-library/react'
import { http } from 'msw'
import { setupServer } from 'msw/node'
import App from '@/App'

const server = setupServer(
  http.get('/api/promotions', () =>
    Response.json([{ id: 1, nom: 'KFOKAM48' }]),
  ),
)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterAll(() => server.close())
afterEach(() => server.resetHandlers())

describe('Coquille App', () => {
  it('affiche le titre et la promotion reçue de l\'API', async () => {
    render(<App />)
    expect(
      screen.getByRole('heading', { name: /KFOKAM48 — Présences et relectures/i }),
    ).toBeInTheDocument()
    expect(await screen.findByText(/API joignable/i)).toBeInTheDocument()
    expect(screen.getByText('KFOKAM48')).toBeInTheDocument()
  })

  it('affiche une erreur normalisée { code, message } quand l\'API échoue', async () => {
    server.use(
      http.get('/api/promotions', () =>
        Response.json(
          { code: 'ERREUR_INTERNE', message: 'Erreur interne.' },
          { status: 500 },
        ),
      ),
    )
    render(<App />)
    expect(await screen.findByRole('alert')).toHaveTextContent(/ERREUR_INTERNE/)
  })
})
