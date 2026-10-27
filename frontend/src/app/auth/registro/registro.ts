import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { erroresDeCampo, mensajeDeError } from '../../shared/errores-http';

/** Mismo patrón que valida el backend (RegistroRequest). */
const PATRON_SLUG = /^[a-z0-9]+(-[a-z0-9]+)*$/;

@Component({
  selector: 'app-registro',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
  ],
  templateUrl: './registro.html',
  styleUrl: '../auth-pagina.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Registro {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    nombreNegocio: ['', [Validators.required, Validators.maxLength(120)]],
    slug: [
      '',
      [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(80),
        Validators.pattern(PATRON_SLUG),
      ],
    ],
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(160)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
  });
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected enviar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);

    this.auth.registrar(this.formulario.getRawValue()).subscribe({
      next: () => void this.router.navigateByUrl('/panel'),
      error: (error: unknown) => {
        this.mostrarErroresDelServidor(error);
        this.enviando.set(false);
      },
    });
  }

  /** Coloca cada error 400 bajo su campo; el resto (p. ej. 409 slug repetido) va arriba. */
  private mostrarErroresDelServidor(error: unknown): void {
    const porCampo = erroresDeCampo(error);
    let asignados = 0;
    for (const [campo, mensaje] of Object.entries(porCampo)) {
      const control = this.formulario.get(campo);
      if (control) {
        control.setErrors({ servidor: mensaje });
        control.markAsTouched();
        asignados++;
      }
    }
    if (asignados === 0) {
      this.error.set(mensajeDeError(error));
    }
  }
}
