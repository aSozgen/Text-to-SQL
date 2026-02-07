import { Routes } from '@angular/router';
import { HomeComponent } from './components/home/home.component';
import { RegisterComponent } from './components/auth/register/register.component';
import {LoginComponent} from './components/auth/login/login.component';
import {SchemasComponent} from './components/schemas/schemas.component';
import {ProfileComponent} from './components/auth/profile/profile.component';
import {HelpComponent} from './components/help/help.component';
import {ChatComponent} from './components/chat/chat.component';

export const routes: Routes = [
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  { path: 'home', component: HomeComponent },
  { path: 'chat', component: ChatComponent },
  { path: 'schemas', component: SchemasComponent },
  { path: 'help', component: HelpComponent },
  { path: 'profile', component: ProfileComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'login', component: LoginComponent },
];
