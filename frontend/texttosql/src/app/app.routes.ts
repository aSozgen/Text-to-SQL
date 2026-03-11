import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/guards/auth.guard';
import { HelpComponent } from "./components/help/help.component";
import { HomeComponent } from './components/home/home.component';
import { ChatComponent } from './components/chat/chat.component';
import { SchemasComponent } from './components/schemas/schemas.component';
import { ProfileComponent } from './components/auth/profile/profile.component';
import { RegisterComponent } from './components/auth/register/register.component';
import { LoginComponent } from './components/auth/login/login.component';
import { VerifyEmailComponent } from './components/auth/verify-email/verify-email.component';
import { ForgotPasswordComponent } from './components/auth/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './components/auth/reset-password/reset-password.component';

export const routes: Routes = [
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  { path: 'home', component: HomeComponent },
  { path: 'chat', component: ChatComponent },
  { path: 'schemas', component: SchemasComponent },
  { path: 'help', component: HelpComponent },
  { path: 'profile', component: ProfileComponent,
    canActivate: [authGuard] },
  { path: 'register', component: RegisterComponent,
    canActivate: [guestGuard] },
  { path: 'login', component: LoginComponent,
    canActivate: [guestGuard] },
  { path: 'verify-email', component: VerifyEmailComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent,
    canActivate: [guestGuard] },
  { path: 'reset-password', component: ResetPasswordComponent,
    canActivate: [guestGuard] },
  { path: '**', redirectTo: 'home' }
];
