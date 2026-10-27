package com.juan.reservas.comun;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduce las excepciones a respuestas con formato Problem Details (RFC 9457).
 * <p>
 * Hereda de {@link ResponseEntityExceptionHandler}, que ya convierte en Problem Details
 * los errores propios de Spring MVC (JSON mal formado, método no permitido...).
 * Aquí se añaden los errores de dominio, los de seguridad y los de base de datos.
 */
@RestControllerAdvice
public class ManejadorGlobalErrores extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalErrores.class);

    /** SQLState de PostgreSQL para la violación de una restricción de exclusión. */
    static final String SQLSTATE_EXCLUSION_VIOLATION = "23P01";

    @ExceptionHandler(NoEncontradoException.class)
    public ProblemDetail noEncontrado(NoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "No encontrado", ex.getMessage());
    }

    @ExceptionHandler(NoAutorizadoException.class)
    public ProblemDetail noAutorizado(NoAutorizadoException ex) {
        return problema(HttpStatus.UNAUTHORIZED, "No autenticado", ex.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    public ProblemDetail conflicto(ConflictoException ex) {
        return problema(HttpStatus.CONFLICT, "Conflicto", ex.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ProblemDetail reglaNegocio(ReglaNegocioException ex) {
        return problema(HttpStatus.UNPROCESSABLE_CONTENT, "Regla de reserva incumplida", ex.getMessage());
    }

    /**
     * Si dos peticiones intentan ocupar el mismo hueco a la vez, la base de datos rechaza
     * la segunda con la restricción reserva_sin_solape. Se responde 409, no 500.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integridadDatos(DataIntegrityViolationException ex) {
        if (SQLSTATE_EXCLUSION_VIOLATION.equals(sqlState(ex))) {
            return problema(HttpStatus.CONFLICT, "Conflicto", "Ese hueco acaba de ocuparse");
        }
        log.error("Violación de integridad no controlada", ex);
        return problema(HttpStatus.CONFLICT, "Conflicto", "La operación entra en conflicto con datos existentes");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail noAutenticado(AuthenticationException ex) {
        return problema(HttpStatus.UNAUTHORIZED, "No autenticado", "Se requiere un token válido");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail accesoDenegado(AccessDeniedException ex) {
        return problema(HttpStatus.FORBIDDEN, "Acceso denegado", "Tu rol no permite esta operación");
    }

    /** Último recurso: se registra el error completo pero al cliente no se le filtran detalles internos. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail errorInesperado(Exception ex) {
        log.error("Error no controlado", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", "Ha ocurrido un error inesperado");
    }

    /** 400 con el detalle de cada campo inválido en la propiedad "errores". */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errores.putIfAbsent(e.getField(), e.getDefaultMessage()));

        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Datos no válidos",
                "La petición contiene campos no válidos");
        problema.setProperty("errores", errores);
        return ResponseEntity.badRequest().body(problema);
    }

    private static ProblemDetail problema(HttpStatus estado, String titulo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        return problema;
    }

    /** Busca en la cadena de causas la excepción JDBC original para leer su SQLState. */
    @Nullable
    private static String sqlState(Throwable ex) {
        for (Throwable causa = ex; causa != null; causa = causa.getCause()) {
            if (causa instanceof SQLException sql && sql.getSQLState() != null) {
                return sql.getSQLState();
            }
        }
        return null;
    }
}
