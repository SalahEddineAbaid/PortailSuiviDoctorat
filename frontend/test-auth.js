/**
 * 🧪 Script de Test Automatisé - Module d'Authentification
 * 
 * Ce script teste automatiquement toutes les fonctionnalités d'authentification
 * 
 * Usage: node test-auth.js
 */

const API_URL = 'http://localhost:8081/api';
const TEST_EMAIL = `test.${Date.now()}@example.com`;
const TEST_PASSWORD = 'Test@1234567890';
const NEW_PASSWORD = 'NewTest@1234567890';

let accessToken = null;
let refreshToken = null;
let resetToken = null;

// Couleurs pour la console
const colors = {
  reset: '\x1b[0m',
  green: '\x1b[32m',
  red: '\x1b[31m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  cyan: '\x1b[36m'
};

function log(message, color = 'reset') {
  console.log(`${colors[color]}${message}${colors.reset}`);
}

function success(message) {
  log(`✅ ${message}`, 'green');
}

function error(message) {
  log(`❌ ${message}`, 'red');
}

function info(message) {
  log(`ℹ️  ${message}`, 'blue');
}

function warning(message) {
  log(`⚠️  ${message}`, 'yellow');
}

function section(title) {
  log(`\n${'='.repeat(60)}`, 'cyan');
  log(`  ${title}`, 'cyan');
  log(`${'='.repeat(60)}`, 'cyan');
}

// Fonction pour faire des requêtes HTTP
async function request(endpoint, method = 'GET', body = null, token = null) {
  const url = `${API_URL}${endpoint}`;
  const headers = {
    'Content-Type': 'application/json'
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const options = {
    method,
    headers
  };

  if (body) {
    options.body = JSON.stringify(body);
  }

  try {
    const response = await fetch(url, options);
    const data = response.status !== 204 ? await response.json() : null;
    
    return {
      status: response.status,
      ok: response.ok,
      data
    };
  } catch (err) {
    return {
      status: 0,
      ok: false,
      error: err.message
    };
  }
}

// Attendre un peu entre les tests
function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

// Tests
async function testHealthCheck() {
  section('Test 1: Health Check');
  
  const response = await request('/actuator/health');
  
  if (response.status === 0) {
    error('Backend non accessible sur http://localhost:8081');
    error('Veuillez démarrer le backend avant de lancer les tests');
    process.exit(1);
  }
  
  if (response.ok && response.data?.status === 'UP') {
    success('Backend accessible et opérationnel');
    return true;
  } else {
    error('Backend non opérationnel');
    return false;
  }
}

async function testRegister() {
  section('Test 2: Inscription');
  
  info(`Email de test: ${TEST_EMAIL}`);
  
  const response = await request('/auth/register', 'POST', {
    email: TEST_EMAIL,
    password: TEST_PASSWORD,
    firstName: 'Test',
    lastName: 'User',
    phoneNumber: '+212612345678',
    adresse: '123 Test Street',
    ville: 'Casablanca',
    pays: 'Maroc'
  });
  
  if (response.status === 201 || response.status === 200) {
    success('Inscription réussie');
    return true;
  } else if (response.status === 409) {
    warning('Email déjà utilisé (normal si le test a déjà été exécuté)');
    return true;
  } else {
    error(`Inscription échouée: ${response.status}`);
    error(JSON.stringify(response.data, null, 2));
    return false;
  }
}

async function testLogin() {
  section('Test 3: Connexion');
  
  const response = await request('/auth/login', 'POST', {
    email: TEST_EMAIL,
    password: TEST_PASSWORD
  });
  
  if (response.ok && response.data?.accessToken && response.data?.refreshToken) {
    accessToken = response.data.accessToken;
    refreshToken = response.data.refreshToken;
    success('Connexion réussie');
    info(`Access Token: ${accessToken.substring(0, 20)}...`);
    info(`Refresh Token: ${refreshToken.substring(0, 20)}...`);
    return true;
  } else {
    error(`Connexion échouée: ${response.status}`);
    error(JSON.stringify(response.data, null, 2));
    return false;
  }
}

async function testGetProfile() {
  section('Test 4: Récupération du Profil');
  
  if (!accessToken) {
    error('Pas de token disponible');
    return false;
  }
  
  const response = await request('/users/profile', 'GET', null, accessToken);
  
  if (response.ok && response.data) {
    success('Profil récupéré avec succès');
    info(`Nom: ${response.data.FirstName} ${response.data.LastName}`);
    info(`Email: ${response.data.email}`);
    info(`Rôles: ${response.data.roles?.join(', ')}`);
    return true;
  } else {
    error(`Récupération du profil échouée: ${response.status}`);
    error(JSON.stringify(response.data, null, 2));
    return false;
  }
}

async function testUpdateProfile() {
  section('Test 5: Mise à jour du Profil');
  
  if (!accessToken) {
    error('Pas de token disponible');
    return false;
  }
  
  const response = await request('/users/profile', 'PUT', {
    FirstName: 'Test-Modified',
    LastName: 'User-Modified',
    phoneNumber: '+212612345679',
    adresse: '456 New Street',
    ville: 'Rabat',
    pays: 'Maroc'
  }, accessToken);
  
  if (response.ok) {
    success('Profil mis à jour avec succès');
    return true;
  } else {
    error(`Mise à jour du profil échouée: ${response.status}`);
    error(JSON.stringify(response.data, null, 2));
    return false;
  }
}

async function testChangePassword() {
  section('Test 6: Changement de Mot de Passe');
  
  if (!accessToken) {
    error('Pas de token disponible');
    return false;
  }
  
  const response = await request('/users/change-password', 'POST', {
    oldPassword: TEST_PASSWORD,
    newPassword: NEW_PASSWORD
  }, accessToken);
  
  if (response.ok || response.status === 204) {
    success('Mot de passe changé avec succès');
    return true;
  } else {
    error(`Changement de mot de passe échoué: ${response.status}`);
    error(JSON.stringify(response.data, null, 2));
    return false;
  }
}

async function testLoginWithNewPassword() {
  section('Test 7: Connexion avec le Nouveau Mot de Passe');
  
  await sleep(1000); // Attendre un peu
  
  const response = await request('/auth/login', 'POST', {
    email: TEST_EMAIL,
    password: NEW_PASSWORD
  });
  
  if (response.ok && response.data?.accessToken) {
    accessToken = response.data.accessToken;
    refreshToken = response.data.refreshToken;
    success('Connexion avec le nouveau mot de passe réussie');
    return true;
  } else {
    error(`Connexion avec le nouveau mot de passe échouée: ${response.status}`);
    error(JSON.stringify(response.data, null, 2));
    return false;
  }
}

async function testRefreshToken() {
  section('Test 8: Rafraîchissement du Token');
  
  if (!refreshToken) {
    error('Pas de refresh token disponible');
    return false;
  }
  
  const response = await request('/auth/refresh', 'POST', {
    refreshToken: refreshToken
  });
  
  if (response.ok && response.data?.accessToken) {
    accessToken = response.data.accessToken;
    refreshToken = response.data.refreshToken;
    success('Token rafraîchi avec succès');
    info(`Nouveau Access Token: ${accessToken.substring(0, 20)}...`);
    return true;
  } else {
    error(`Rafraîchissement du token échoué: ${response.status}`);
    error(JSON.stringify(response.data, null, 2));
    return false;
  }
}

async function testForgotPassword() {
  section('Test 9: Mot de Passe Oublié');
  
  const response = await request('/users/forgot-password', 'POST', {
    email: TEST_EMAIL
  });
  
  if (response.ok || response.status === 204) {
    success('Demande de réinitialisation envoyée');
    warning('Vérifiez les logs du backend pour récupérer le token de réinitialisation');
    return true;
  } else {
    error(`Demande de réinitialisation échouée: ${response.status}`);
    error(JSON.stringify(response.data, null, 2));
    return false;
  }
}

async function testInvalidCredentials() {
  section('Test 10: Credentials Invalides (Test de Sécurité)');
  
  const response = await request('/auth/login', 'POST', {
    email: TEST_EMAIL,
    password: 'WrongPassword123!'
  });
  
  if (response.status === 401) {
    success('Connexion correctement refusée avec des credentials invalides');
    return true;
  } else {
    error('La connexion aurait dû être refusée');
    return false;
  }
}

async function testUnauthorizedAccess() {
  section('Test 11: Accès Non Autorisé (Test de Sécurité)');
  
  const response = await request('/users/profile', 'GET', null, 'invalid_token');
  
  if (response.status === 401) {
    success('Accès correctement refusé avec un token invalide');
    return true;
  } else {
    error('L\'accès aurait dû être refusé');
    return false;
  }
}

// Exécuter tous les tests
async function runAllTests() {
  log('\n🧪 TESTS AUTOMATISÉS - MODULE D\'AUTHENTIFICATION\n', 'cyan');
  
  const results = [];
  
  // Tests séquentiels
  results.push(await testHealthCheck());
  await sleep(500);
  
  results.push(await testRegister());
  await sleep(500);
  
  results.push(await testLogin());
  await sleep(500);
  
  results.push(await testGetProfile());
  await sleep(500);
  
  results.push(await testUpdateProfile());
  await sleep(500);
  
  results.push(await testChangePassword());
  await sleep(500);
  
  results.push(await testLoginWithNewPassword());
  await sleep(500);
  
  results.push(await testRefreshToken());
  await sleep(500);
  
  results.push(await testForgotPassword());
  await sleep(500);
  
  results.push(await testInvalidCredentials());
  await sleep(500);
  
  results.push(await testUnauthorizedAccess());
  
  // Résumé
  section('RÉSUMÉ DES TESTS');
  
  const passed = results.filter(r => r).length;
  const total = results.length;
  const percentage = Math.round((passed / total) * 100);
  
  log(`\nTests réussis: ${passed}/${total} (${percentage}%)`, 'cyan');
  
  if (percentage === 100) {
    success('\n🎉 TOUS LES TESTS SONT PASSÉS ! Le module d\'authentification fonctionne parfaitement.\n');
  } else if (percentage >= 80) {
    warning(`\n⚠️  ${total - passed} test(s) ont échoué. Vérifiez les erreurs ci-dessus.\n`);
  } else {
    error(`\n❌ ${total - passed} test(s) ont échoué. Des problèmes importants ont été détectés.\n`);
  }
  
  info('Pour plus de détails, consultez VERIFICATION_COMPLETE.md\n');
}

// Lancer les tests
runAllTests().catch(err => {
  error(`Erreur fatale: ${err.message}`);
  console.error(err);
  process.exit(1);
});
