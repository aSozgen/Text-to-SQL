import { Component } from '@angular/core';
import {NavigationEnd, NavigationError, NavigationStart, Router, RouterOutlet} from '@angular/router';
import { NavbarComponent } from "./components/shared/navbar/navbar.component";
import { FooterComponent } from "./components/shared/footer/footer.component";
import {environment} from '../environments/environment';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, NavbarComponent, FooterComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  title = environment.title;
  isLoading = false;

  constructor(private router: Router) {
    this.router.events.subscribe(event => {
      if (event instanceof NavigationStart) {
        this.isLoading = true;
      } else if (event instanceof NavigationEnd || event instanceof NavigationError) {
        this.isLoading = false;
      }
    });
  }
}
