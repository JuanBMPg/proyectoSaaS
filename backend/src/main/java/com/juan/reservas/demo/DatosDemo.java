package com.juan.reservas.demo;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.juan.reservas.auth.Rol;
import com.juan.reservas.auth.Usuario;
import com.juan.reservas.auth.UsuarioRepository;
import com.juan.reservas.cliente.Cliente;
import com.juan.reservas.cliente.ClienteRepository;
import com.juan.reservas.comun.TokenSecreto;
import com.juan.reservas.negocio.Negocio;
import com.juan.reservas.negocio.NegocioRepository;
import com.juan.reservas.recurso.Horario;
import com.juan.reservas.recurso.HorarioRepository;
import com.juan.reservas.recurso.Recurso;
import com.juan.reservas.recurso.RecursoRepository;
import com.juan.reservas.recurso.TipoRecurso;
import com.juan.reservas.reserva.EstadoReserva;
import com.juan.reservas.reserva.Reserva;
import com.juan.reservas.reserva.ReservaRepository;
import com.juan.reservas.servicio.Servicio;
import com.juan.reservas.servicio.ServicioRepository;

/**
 * Negocio de demostración: una peluquería con dos profesionales, sus horarios, un empleado
 * y reservas en los próximos días laborables.
 * <p>
 * Es código Java y no una migración SQL para que las contraseñas se cifren con el mismo
 * PasswordEncoder que la aplicación y las reservas caigan siempre en fechas futuras.
 * Es idempotente: si el negocio ya existe no hace nada.
 */
@Component
public class DatosDemo {

    public static final String SLUG = "peluqueria-lola";
    public static final String EMAIL_PROPIETARIA = "demo@reservas.local";
    public static final String EMAIL_EMPLEADA = "marta@reservas.local";
    public static final String PASSWORD = "demo12345";

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    private static final Set<DayOfWeek> LABORABLES = Set.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

    private final NegocioRepository negocios;
    private final UsuarioRepository usuarios;
    private final ServicioRepository servicios;
    private final RecursoRepository recursos;
    private final HorarioRepository horarios;
    private final ClienteRepository clientes;
    private final ReservaRepository reservas;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DatosDemo(NegocioRepository negocios, UsuarioRepository usuarios, ServicioRepository servicios,
            RecursoRepository recursos, HorarioRepository horarios, ClienteRepository clientes,
            ReservaRepository reservas, PasswordEncoder passwordEncoder, Clock clock) {
        this.negocios = negocios;
        this.usuarios = usuarios;
        this.servicios = servicios;
        this.recursos = recursos;
        this.horarios = horarios;
        this.clientes = clientes;
        this.reservas = reservas;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /** @return true si ha creado los datos; false si el negocio de demostración ya existía */
    @Transactional
    public boolean cargar() {
        if (negocios.existsBySlug(SLUG)) {
            return false;
        }
        Instant ahora = Instant.now(clock);

        Negocio lola = negocios.save(new Negocio("Peluquería Lola", SLUG, ahora));
        lola.actualizarContacto("hola@peluquerialola.example", "910 000 000", "C/ Mayor 12, Madrid");
        usuarios.save(new Usuario(lola, EMAIL_PROPIETARIA, passwordEncoder.encode(PASSWORD), "Lola",
                Rol.PROPIETARIO, ahora));

        Servicio corte = servicios.save(new Servicio(lola, "Corte de pelo", "Lavado, corte y secado.", 30, 10, 1500));
        Servicio tinte = servicios.save(new Servicio(lola, "Tinte", "Color completo con producto sin amoniaco.", 60, 10, 3500));
        Servicio peinado = servicios.save(new Servicio(lola, "Peinado", "Para eventos y ocasiones especiales.", 45, 0, 2000));
        Servicio barba = servicios.save(new Servicio(lola, "Corte y barba", null, 45, 10, 2200));

        Recurso marta = recursos.save(new Recurso(lola, "Marta", TipoRecurso.PERSONA));
        marta.asignarServicios(List.of(corte, tinte, peinado));
        Recurso lucia = recursos.save(new Recurso(lola, "Lucía", TipoRecurso.PERSONA));
        lucia.asignarServicios(List.of(corte, peinado, barba));

        for (DayOfWeek dia : LABORABLES) {
            horarios.save(new Horario(marta, dia, LocalTime.of(9, 0), LocalTime.of(14, 0)));
            horarios.save(new Horario(marta, dia, LocalTime.of(16, 0), LocalTime.of(20, 0)));
            horarios.save(new Horario(lucia, dia, LocalTime.of(10, 0), LocalTime.of(18, 0)));
        }
        horarios.save(new Horario(marta, DayOfWeek.SATURDAY, LocalTime.of(10, 0), LocalTime.of(14, 0)));

        Usuario empleada = new Usuario(lola, EMAIL_EMPLEADA, passwordEncoder.encode(PASSWORD), "Marta López",
                Rol.EMPLEADO, ahora);
        empleada.asignarRecurso(marta.getId());
        usuarios.save(empleada);

        crearReservas(lola, ahora, marta, lucia, corte, tinte, peinado, barba);
        return true;
    }

    /** Unas cuantas citas en los próximos cinco días laborables, dentro del horario y sin solapes. */
    private void crearReservas(Negocio lola, Instant ahora, Recurso marta, Recurso lucia,
            Servicio corte, Servicio tinte, Servicio peinado, Servicio barba) {
        List<Cliente> gente = List.of(
                cliente(lola, "Ana García", "ana.garcia@example.com", "600 111 222", ahora),
                cliente(lola, "Luis Pérez", "luis.perez@example.com", null, ahora),
                cliente(lola, "Carmen Ruiz", "carmen.ruiz@example.com", "600 333 444", ahora),
                cliente(lola, "Javier Martín", "javier.martin@example.com", null, ahora));

        LocalDate dia = LocalDate.now(clock.withZone(MADRID));
        int laborables = 0;
        while (laborables < 5) {
            dia = dia.plusDays(1);
            if (!LABORABLES.contains(dia.getDayOfWeek())) {
                continue;
            }
            laborables++;
            reserva(lola, corte, marta, gente.get(laborables % 4), dia, "10:00", EstadoReserva.CONFIRMADA, ahora);
            reserva(lola, tinte, marta, gente.get((laborables + 1) % 4), dia, "11:00", EstadoReserva.CONFIRMADA, ahora);
            reserva(lola, peinado, marta, gente.get((laborables + 2) % 4), dia, "17:00",
                    laborables == 2 ? EstadoReserva.PENDIENTE : EstadoReserva.CONFIRMADA, ahora);
            reserva(lola, barba, lucia, gente.get((laborables + 3) % 4), dia, "10:30", EstadoReserva.CONFIRMADA, ahora);
            reserva(lola, corte, lucia, gente.get(laborables % 4), dia, "12:00", EstadoReserva.CONFIRMADA, ahora);
        }
    }

    private Cliente cliente(Negocio negocio, String nombre, String email, String telefono, Instant ahora) {
        return clientes.save(new Cliente(negocio, nombre, email, telefono, ahora));
    }

    private void reserva(Negocio negocio, Servicio servicio, Recurso recurso, Cliente cliente, LocalDate dia,
            String hora, EstadoReserva estado, Instant ahora) {
        Instant inicio = ZonedDateTime.of(dia, LocalTime.parse(hora), MADRID).toInstant();
        Instant fin = inicio.plus(Duration.ofMinutes(servicio.getDuracionMin()));
        Instant finBloqueo = fin.plus(Duration.ofMinutes(servicio.getMargenMin()));
        reservas.save(new Reserva(negocio, servicio, recurso, cliente, inicio, fin, finBloqueo, estado, null,
                TokenSecreto.hash(TokenSecreto.generar()), ahora));
    }
}
