package com.utn.tp_spring_grupal.common;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduce las excepciones a respuestas amigables para el cliente.
 * El detalle técnico completo queda en el log del servidor.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** Cuerpo único de error: el frontend muestra `mensaje` tal cual en un toast. */
    public record ErrorRespuesta(String mensaje) {
    }

    // Regla de negocio violada (estado no editable, ya anulada, etc.) → 409.
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorRespuesta> reglaNegocio(ReglaNegocioException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorRespuesta(mensajeSeguro(ex.getMessage())));
    }

    // Datos del request inválidos con mensaje propio (cantidad <= 0, sin renglones, etc.) → 400.
    @ExceptionHandler(ValidacionException.class)
    public ResponseEntity<ErrorRespuesta> validacion(ValidacionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorRespuesta(mensajeSeguro(ex.getMessage())));
    }

    // Los services usan NullPointerException para "No se encontró X..." → 404.
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorRespuesta> recursoNoEncontrado(NullPointerException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorRespuesta(mensajeSeguro(ex.getMessage())));
    }

    // Errores de negocio con mensaje conocido → 404; el resto → 500 amigable.
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorRespuesta> errorNegocio(RuntimeException ex) {
        String mensaje = mensajeSeguro(ex.getMessage());
        if (mensaje.startsWith("Recurso no encontrado") || mensaje.startsWith("No se encontr")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorRespuesta(mensaje));
        }
        log.error("Error no controlado en la API", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorRespuesta("Ocurrió un error inesperado."));
    }

    // Parámetro mal formado (p. ej. fecha o número inválido) → 400.
    @ExceptionHandler({ IllegalArgumentException.class, MethodArgumentTypeMismatchException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class })
    public ResponseEntity<ErrorRespuesta> parametroInvalido(Exception ex) {
        log.warn("Parámetro inválido: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorRespuesta("Parámetro inválido: revisá fechas, números y estados enviados."));
    }

    // Cualquier otro error → 500 amigable, detalle en el log (excepto 404 de rutas).
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> errorGenerico(Exception ex) {
        // Interceptamos el error del favicon o rutas inexistentes por el nombre de la clase
        // Esto evita tener que importar la clase y lidiar con el error del IDE
        if (ex.getClass().getSimpleName().equals("NoResourceFoundException")) {
            log.warn("Recurso no encontrado (404): {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorRespuesta("Ruta o recurso no encontrado."));
        }

        // Si es cualquier otro error, se loguea como siempre y devuelve 500
        log.error("Error no controlado en la API", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorRespuesta("Ocurrió un error inesperado."));
    }

    private String mensajeSeguro(String mensaje) {
        return mensaje != null && !mensaje.isBlank() ? mensaje : "Solicitud inválida.";
    }
}