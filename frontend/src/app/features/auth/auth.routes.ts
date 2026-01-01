import { Routes } from '@angular/router';
import { Login } from './login/login';
import { Register } from './register/register';
import { ForgotPassword } from './forgot-password/forgot-password';
import { ProfileComponent } from './profile/profile.component';
import { authGuard } from '../../core/guards/auth.guard';

/**
 * 🔐 Routes du module d'authentification
 */
export const authRoutes: Routes = [
  {
    path: 'login',
    component: Login,
    title: 'Connexion - Portail Doctoral'
  },
  {
    path: 'register',
    component: Register,
    title: 'Inscription - Portail Doctoral'
  },
  {
    path: 'forgot-password',
    component: ForgotPassword,
    title: 'Mot de passe oublié - Portail Doctoral'
  },
  {
    path: 'profile',
    component: ProfileComponent,
    canActivate: [authGuard],
    title: 'Mon Profil - Portail Doctoral'
  }
];
