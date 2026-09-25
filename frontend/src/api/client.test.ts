import { describe, expect, it, afterAll, afterEach, beforeAll } from 'vitest'
import { http } from 'msw'
import { setupServer } from 'msw/node'
import { ApiError, apiFetch } from './client'

const server = setupServer(
  http.get('/api/ok', () => Response.json({ valeur: 42 })),
  http.get('/api/erreur', () =>
    Response.json({ code: 'CODE_EXPIRE', message: 'Le code de présence a expiré.' }, { status: 410 }),
  ),
)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterAll(() => server.close())
afterEach(() => server.resetHandlers())

describe('apiFetch', () => {
  it('retourne le JSON typé en cas de succès', async () => {
    const data = await apiFetch<{ valeur: number }>('/ok')
    expect(data).toEqual({ valeur: 42 })
  })

  it('lève une ApiError avec code et message du contrat (ENF4)', async () => {
    const error = await apiFetch('/erreur').catch((e: unknown) => e)
    expect(error).toBeInstanceOf(ApiError)
    const apiError = error as ApiError
    expect(apiError.status).toBe(410)
    expect(apiError.code).toBe('CODE_EXPIRE')
    expect(apiError.message).toBe('Le code de présence a expiré.')
  })

  it('lève RESEAU_INJOIGNABLE quand le serveur est éteint', async () => {
    server.use(
      http.get('/api/ko', () => Response.error()),
    )
    const error = await apiFetch('/ko').catch((e: unknown) => e)
    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).code).toBe('RESEAU_INJOIGNABLE')
  })

  it('envoie un POST avec corps JSON', async () => {
    let received: unknown = null
    server.use(
      http.post('/api/echo', async ({ request }) => {
        received = await request.json()
        return Response.json({ recu: true }, { status: 201 })
      }),
    )
    const data = await apiFetch<{ recu: boolean }>('/echo', {
      method: 'POST',
      body: { code: 'A7K3P9' },
    })
    expect(data).toEqual({ recu: true })
    expect(received).toEqual({ code: 'A7K3P9' })
  })
})
