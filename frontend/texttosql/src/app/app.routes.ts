import { Routes } from '@angular/router';
import { HomeComponent } from './components/shared/home/home.component';
import { RegisterComponent } from './components/auth/register/register.component';
import {LoginComponent} from './components/auth/login/login.component';
import {SchemasComponent} from './components/schemas/schemas.component';
import {ProfileComponent} from './components/auth/profile/profile.component';

export const routes: Routes = [
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  { path: 'home', component: HomeComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'login', component: LoginComponent },
  { path: 'schemas', component: SchemasComponent },
  { path: 'profile', component: ProfileComponent },
];
