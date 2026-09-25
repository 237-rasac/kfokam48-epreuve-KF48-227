// Passe E2E complète KFOKAM48 — scénario du jalon v0.1, rejoué avant soumission.
// Usage : node e2e/scenario.mjs   (backend sur http://localhost:8080, données démo V2 neuves)
const BASE = 'http://localhost:8080/api';
let pass = 0, fail = 0;
const echecs = [];

function check(num, label, cond, detail = '') {
  if (cond) { pass++; console.log(`PASS  ${num}. ${label}`); }
  else { fail++; echecs.push(`${num}. ${label} -> ${detail}`); console.log(`FAIL  ${num}. ${label} -> ${detail}`); }
}

async function call(method, path, body) {
  const res = await fetch(BASE + path, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : {},
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  let json = null;
  try { json = text ? JSON.parse(text) : null; } catch { /* corps non JSON */ }
  return { status: res.status, json, text };
}

const rfc3339 = (s) => typeof s === 'string' && /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?(Z|[+-]\d{2}:\d{2})$/.test(s);

console.log(`Node ${process.version} — cible ${BASE}\n`);

// 1. Référentiel : 1 promotion, 5 étudiants (V2)
const promos = await call('GET', '/promotions');
const etus = await call('GET', '/etudiants');
check(1, 'Référentiel : 1 promotion, 5 étudiants', promos.status === 200 && promos.json.length === 1 && etus.status === 200 && etus.json.length === 5,
  `promos=${promos.status}/${promos.json?.length} etus=${etus.status}/${etus.json?.length}`);
const parNom = Object.fromEntries(etus.json.map((e) => [e.nom.split(' ')[0].toLowerCase(), e.id]));
const ALICE = parNom.amina, BORIS = parNom.boris, CLARA = parNom.clarisse, DAVID = parNom.david, EMMA = parNom.emma;
check(2, 'Étudiants de démo présents (Amina, Boris, Clarisse, David, Emma)',
  [ALICE, BORIS, CLARA, DAVID, EMMA].every(Number.isFinite), JSON.stringify(parNom));

// 3-4. Ouverture de session (EF1) : code unique, expiration 15 min (RG1), dates RFC 3339
const s1 = await call('POST', '/sessions', { titre: 'E2E passe finale', promotionId: 1 });
check(3, 'POST /api/sessions → 201 avec code à 6 caractères',
  s1.status === 201 && typeof s1.json.code === 'string' && s1.json.code.length === 6,
  `status=${s1.status} body=${s1.text.slice(0, 120)}`);
const SID = s1.json.id, CODE = s1.json.code;
const dureeMin = (Date.parse(s1.json.expirationAt) - Date.parse(s1.json.ouvertureAt)) / 60000;
check(4, 'Dates RFC 3339 avec fuseau + expiration à 15 min (RG1)',
  rfc3339(s1.json.ouvertureAt) && rfc3339(s1.json.expirationAt) && Math.round(dureeMin) === 15,
  `ouvertureAt=${s1.json.ouvertureAt} expirationAt=${s1.json.expirationAt} duree=${dureeMin}min`);

// 5. Consultation de la session
const g1 = await call('GET', `/sessions/${SID}`);
check(5, 'GET /api/sessions/{id} → même code', g1.status === 200 && g1.json.code === CODE, `status=${g1.status}`);

// 6. Présence étudiant par code (EF2)
const p1 = await call('POST', '/presences', { code: CODE, etudiantId: ALICE });
check(6, 'POST /api/presences (étudiant) → 201 source=ETUDIANT',
  p1.status === 201 && p1.json.source === 'ETUDIANT', `status=${p1.status} body=${p1.text.slice(0, 120)}`);

// 7. Liste des présences
const pl1 = await call('GET', `/sessions/${SID}/presences`);
check(7, 'GET présences → 1 présence', pl1.status === 200 && pl1.json.length === 1, `status=${pl1.status} n=${pl1.json?.length}`);

// 8. Doublon → 409 DEJA_PRESENT (RG15)
const p2 = await call('POST', '/presences', { code: CODE, etudiantId: ALICE });
check(8, 'Doublon de présence → 409 DEJA_PRESENT (RG15)',
  p2.status === 409 && p2.json.code === 'DEJA_PRESENT', `status=${p2.status} body=${p2.text.slice(0, 120)}`);

// 9. Ajout manuel formateur (EF7/RG11)
const p3 = await call('POST', '/presences', { code: CODE, etudiantId: BORIS, source: 'FORMATEUR' });
check(9, 'Ajout manuel formateur → 201 source=FORMATEUR (EF7)',
  p3.status === 201 && p3.json.source === 'FORMATEUR', `status=${p3.status} body=${p3.text.slice(0, 120)}`);
await call('POST', '/presences', { code: CODE, etudiantId: CLARA, source: 'FORMATEUR' }); // 3e présent

// 10. Dépôt d'exercice (EF3) + assignation aléatoire (EF4/RG5) — pool = présents hors auteur
const L1 = 'https://github.com/amina/exercice-e2e';
const ex1 = await call('POST', '/exercices', { sessionId: SID, etudiantId: ALICE, lien: L1 });
check(10, 'POST /api/exercices → 201 EN_ATTENTE relecteurAssignee=true (EF3/EF4)',
  ex1.status === 201 && ex1.json.statut === 'EN_ATTENTE' && ex1.json.relecteurAssignee === true,
  `status=${ex1.status} body=${ex1.text.slice(0, 160)}`);
const EX1 = ex1.json.id;

// 11. Identifier le relecteur assigné (boris ou clara) via les relectures en attente
let REVIEWER = null, RID1 = null;
for (const cand of [BORIS, CLARA]) {
  const lst = await call('GET', `/etudiants/${cand}/relectures`);
  const hit = (lst.json || []).find((r) => r.exerciceId === EX1);
  if (hit) { REVIEWER = cand; RID1 = hit.id; break; }
}
check(11, 'Relecteur assigné parmi les présents hors auteur (RG5)',
  REVIEWER !== null && REVIEWER !== ALICE, `reviewer=${REVIEWER} rid=${RID1}`);

// 12. Remplacement du lien (EF10/RG10) avant relecture
const L2 = 'https://github.com/amina/exercice-e2e-v2';
const pat = await call('PATCH', `/exercices/${EX1}`, { lien: L2 });
check(12, 'PATCH lien → 200 lien remplacé (EF10)',
  pat.status === 200 && pat.json.lien === L2, `status=${pat.status} body=${pat.text.slice(0, 120)}`);

// 13. Note décimale → 400 NOTE_INVALIDE (RG6, correctif BigDecimal)
const n1 = await call('POST', `/relectures/${RID1}`, { note: 15.5, commentaire: 'test', relecteurId: REVIEWER });
check(13, 'Note décimale 15.5 → 400 NOTE_INVALIDE (RG6)',
  n1.status === 400 && n1.json.code === 'NOTE_INVALIDE', `status=${n1.status} body=${n1.text.slice(0, 120)}`);

// 14. Appelant non assigné → 403 RELECTURE_NON_ASSIGNEE (RG3bis)
const n2 = await call('POST', `/relectures/${RID1}`, { note: 10, commentaire: 'test', relecteurId: DAVID });
check(14, 'Relecteur non assigné → 403 RELECTURE_NON_ASSIGNEE',
  n2.status === 403 && n2.json.code === 'RELECTURE_NON_ASSIGNEE', `status=${n2.status} body=${n2.text.slice(0, 120)}`);

// 15. Rendu valide (EF5) : exercice → RELU
const n3 = await call('POST', `/relectures/${RID1}`, { note: 17, commentaire: 'Très bon travail.', relecteurId: REVIEWER });
check(15, 'Rendu relecture note 17 → 200 (EF5)', n3.status === 200 && n3.json.note === 17,
  `status=${n3.status} body=${n3.text.slice(0, 120)}`);

// 16. EF11 : statut RELU + note, jamais l'identité du relecteur
const d1 = await call('GET', `/exercices/${EX1}`);
check(16, "EF11 : note visible, identité du relecteur absente de la réponse",
  d1.status === 200 && d1.json.statut === 'RELU' && d1.json.note === 17 && !('relecteurId' in d1.json) && !('relecteur' in d1.json),
  `status=${d1.status} body=${d1.text.slice(0, 200)}`);

// 17. Modification avant clôture (EF9/RG7) : note 14 + historique
const n4 = await call('POST', `/relectures/${RID1}`, { note: 14, commentaire: 'Corrigé après entretien.', relecteurId: REVIEWER });
check(17, 'EF9 : modification de la note → 200, note=14',
  n4.status === 200 && n4.json.note === 14, `status=${n4.status} body=${n4.text.slice(0, 120)}`);
const hist = await call('GET', `/etudiants/${REVIEWER}/relectures/rendues`);
check(18, 'EF9 : relecture listée comme rendue (bouton Modifier)',
  hist.status === 200 && hist.json.some((r) => r.id === RID1 && r.note === 14),
  `status=${hist.status} n=${hist.json?.length}`);

// 19. GET /api/relectures/{id} (correctif revue croisée)
const gr = await call('GET', `/relectures/${RID1}`);
check(19, 'GET /api/relectures/{id} → 200 (correctif revue croisée)',
  gr.status === 200 && gr.json.id === RID1, `status=${gr.status}`);

// 20. 2e dépôt (Boris) : pool = présents hors auteur, hors relecteur actif
const ex2 = await call('POST', '/exercices', { sessionId: SID, etudiantId: BORIS, lien: 'https://github.com/boris/exercice-e2e' });
const EX2 = ex2.json?.id;
let REVIEWER2 = null, RID2 = null;
if (ex2.status === 201) {
  for (const cand of [ALICE, CLARA]) {
    const lst = await call('GET', `/etudiants/${cand}/relectures`);
    const hit = (lst.json || []).find((r) => r.exerciceId === EX2);
    if (hit) { REVIEWER2 = cand; RID2 = hit.id; break; }
  }
}
check(20, '2e dépôt → 201 avec relecteur assigné (pool RG5)',
  ex2.status === 201 && ex2.json.relecteurAssignee === true && REVIEWER2 !== null && REVIEWER2 !== BORIS,
  `status=${ex2.status} reviewer2=${REVIEWER2}`);

// 21. EF11 avant rendu : note et commentaire null
const d2 = await call('GET', `/exercices/${EX2}`);
check(21, 'EF11 avant rendu : EN_ATTENTE, note=null, commentaire=null',
  d2.status === 200 && d2.json.statut === 'EN_ATTENTE' && d2.json.note === null && d2.json.commentaire === null,
  `status=${d2.status} body=${d2.text.slice(0, 160)}`);

// 22. Tableau (EF6/RG13/RG21) — cumul avec les données de démo V2 :
// Amina = 2 présences démo + 1 E2E, 1 exercice démo sans relecture + 1 E2E relu (note 14) ; Boris garde la moyenne démo 15.
const tab = await call('GET', `/tableau?promotionId=1`);
const ligneAmina = (tab.json || []).find((l) => l.etudiantId === ALICE);
const ligneBoris = (tab.json || []).find((l) => l.etudiantId === BORIS);
const auMoinsUnNull = (tab.json || []).some((l) => l.moyenne === null);
check(22, 'Tableau : 5 lignes, cumuls démo+E2E (amina 3 présences / 2 exercices / moyenne 14), moyenne null quelque part',
  tab.status === 200 && tab.json.length === 5 && ligneAmina?.moyenne === 14
    && ligneAmina?.presences === 3 && ligneAmina?.exercicesDeposes === 2
    && ligneAmina?.exercicesSansRelecteur === 1
    && ligneBoris?.moyenne === 15 && auMoinsUnNull,
  `status=${tab.status} amina=${JSON.stringify(ligneAmina)} boris=${JSON.stringify(ligneBoris)}`);

// 23. Clôture (EF8) + double clôture → 409
const cl = await call('POST', `/sessions/${SID}/cloture`);
check(23, 'POST clôture → 200 avec clotureAt (EF8)',
  cl.status === 200 && typeof cl.json.clotureAt === 'string' && cl.json.clotureAt.length > 0,
  `status=${cl.status} body=${cl.text.slice(0, 140)}`);
const cl2 = await call('POST', `/sessions/${SID}/cloture`);
check(24, 'Double clôture → 409 SESSION_DEJA_CLOTUREE',
  cl2.status === 409 && cl2.json.code === 'SESSION_DEJA_CLOTUREE', `status=${cl2.status} body=${cl2.text.slice(0, 120)}`);

// 25. Verrouillage aval : présence → 410, dépôt → 410, PATCH → 410 (RG2/RG9/RG10)
const v1 = await call('POST', '/presences', { code: CODE, etudiantId: DAVID });
const v2 = await call('POST', '/exercices', { sessionId: SID, etudiantId: DAVID, lien: 'https://github.com/david/exercice-e2e' });
const v3 = await call('PATCH', `/exercices/${EX1}`, { lien: 'https://github.com/amina/apres-cloture' });
check(25, 'Après clôture : présence 410, dépôt 410, PATCH 410 (SESSION_CLOTUREE)',
  v1.status === 410 && v1.json.code === 'SESSION_CLOTUREE'
    && v2.status === 410 && v2.json.code === 'SESSION_CLOTUREE'
    && v3.status === 410 && v3.json.code === 'SESSION_CLOTUREE',
  `pres=${v1.status}/${v1.json?.code} dep=${v2.status}/${v2.json?.code} patch=${v3.status}/${v3.json?.code}`);

// 26. Relecture verrouillée après clôture → 409 RELECTURE_VERROUILLEE
const v4 = await call('POST', `/relectures/${RID2}`, { note: 12, commentaire: 'test', relecteurId: REVIEWER2 });
check(26, 'Relecture après clôture → 409 RELECTURE_VERROUILLEE',
  v4.status === 409 && v4.json.code === 'RELECTURE_VERROUILLEE', `status=${v4.status} body=${v4.text.slice(0, 120)}`);

// 27. EF12/RG14 : 5 erreurs de code → 429 ETUDIANT_BLOQUE, levée par le formateur
const s2 = await call('POST', '/sessions', { titre: 'E2E session blocage', promotionId: 1 });
const SID2 = s2.json.id, CODE2 = s2.json.code;
// Les 5 erreurs renvoient 400 CODE_INCONNU ; c'est la 5e qui atteint le seuil : le blocage 429 démarre à la 6e tentative.
const echecsCode = [];
for (let i = 0; i < 5; i++) echecsCode.push(await call('POST', '/presences', { code: 'ZZZZZZ', etudiantId: EMMA }));
const bloque = await call('POST', '/presences', { code: CODE2, etudiantId: EMMA });
check(27, 'EF12 : 5 codes inconnus → 400, puis 429 ETUDIANT_BLOQUE dès la 6e tentative',
  echecsCode.every((r) => r.status === 400 && r.json.code === 'CODE_INCONNU')
    && bloque.status === 429 && bloque.json.code === 'ETUDIANT_BLOQUE',
  `5 erreurs=[${echecsCode.map((r) => r.status).join(',')}] 6e=${bloque.status}/${bloque.json?.code}`);

// 28. La voie formateur outrepasse le blocage et l'expiration (EF7/RG11)
const fb = await call('POST', '/presences', { code: CODE2, etudiantId: EMMA, source: 'FORMATEUR' });
check(28, 'Formateur : ajout manuel malgré le blocage → 201 (EF7 outrepasse EF12)',
  fb.status === 201 && fb.json.source === 'FORMATEUR', `status=${fb.status} body=${fb.text.slice(0, 120)}`);

// 29. Code expiré sur la voie étudiant → 410 CODE_EXPIRE (RG1) : la session de démo 1 a expiré en mars
const exp = await call('POST', '/presences', { code: 'A7K3P9', etudiantId: EMMA });
check(29, 'Code de la session démo expirée → 410 CODE_EXPIRE (RG1)',
  exp.status === 410 && exp.json.code === 'CODE_EXPIRE', `status=${exp.status} body=${exp.text.slice(0, 120)}`);

// 30. Format d'erreur unique { code, message } (ENF4) + 405 METHODE_NON_SUPPORTEE
const nf = await call('GET', '/sessions/999999');
const m405 = await call('DELETE', '/promotions');
check(30, 'Format { code, message } partout ; 404 SESSION_INCONNUE et 405 METHODE_NON_SUPPORTEE',
  nf.status === 404 && nf.json.code === 'SESSION_INCONNUE' && 'message' in nf.json
    && m405.status === 405 && m405.json.code === 'METHODE_NON_SUPPORTEE',
  `404=${nf.status}/${nf.json?.code} 405=${m405.status}/${m405.json?.code}`);

console.log(`\n===== RÉSULTAT : ${pass} PASS / ${fail} FAIL sur ${pass + fail} vérifications =====`);
if (fail > 0) { console.log('Échecs :\n - ' + echecs.join('\n - ')); process.exit(1); }
