import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import Keycloak from 'keycloak-js';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent {
  private readonly keycloak = inject(Keycloak);

  get username(): string {
    return (this.keycloak.tokenParsed?.['preferred_username'] as string | undefined) ?? '';
  }

  logout(): void {
    void this.keycloak.logout({ redirectUri: window.location.origin });
  }
}
