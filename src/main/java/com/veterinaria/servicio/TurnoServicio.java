package com.veterinaria.servicio;

import com.veterinaria.dao.MascotaDAO;
import com.veterinaria.dao.TurnoDAO;
import com.veterinaria.modelo.Mascota;
import com.veterinaria.modelo.Turno;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio de negocio para Turno.
 * Gestiona el ciclo de vida completo:
 *   PENDIENTE → CONFIRMADO → REALIZADO
 *            ↘ CANCELADO
 */
@ApplicationScoped
public class TurnoServicio {

    @Inject
    private TurnoDAO turnoDAO;

    @Inject
    private MascotaDAO mascotaDAO;

    // ── Consultas ─────────────────────────────────────────────────

    public List<Turno> listarTodos() {
        return turnoDAO.listarTodos();
    }

    /**
     * Solo turnos PENDIENTE y CONFIRMADO, ordenados por fecha asc.
     */
    public List<Turno> listarVigentes() {
        return turnoDAO.listarVigentes();
    }

    public Turno buscarPorId(Long id) {
        return turnoDAO.buscarPorId(id);
    }

    public List<Turno> listarPorMascota(Long mascotaId) {
        return turnoDAO.listarPorMascota(mascotaId);
    }

    // ── Comandos ──────────────────────────────────────────────────

    /**
     * Reserva un nuevo turno.
     *
     * Reglas de negocio:
     *  - La fecha/hora debe ser posterior al momento actual.
     *  - La mascota debe existir y estar activa.
     *
     * @param turno     entidad con fechaHora, motivo y observaciones ya seteados
     * @param mascotaId ID de la mascota seleccionada en el formulario
     */
    @Transactional
    public void reservar(Turno turno, Long mascotaId) {

        // Validar fecha futura
        if (turno.getFechaHora() == null) {
            throw new IllegalArgumentException(
                "La fecha y hora del turno son obligatorias.");
        }
        if (turno.getFechaHora().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                "No se puede reservar un turno en el pasado.");
        }

        // Validar mascota
        if (mascotaId == null) {
            throw new IllegalArgumentException(
                "Debe seleccionar una mascota.");
        }
        Mascota mascota = mascotaDAO.buscarPorId(mascotaId);
        if (mascota == null) {
            throw new IllegalArgumentException(
                "La mascota seleccionada no existe.");
        }
        if (!mascota.isActivo()) {
            throw new IllegalArgumentException(
                "La mascota seleccionada se encuentra inactiva.");
        }

        turno.setMascota(mascota);
        turno.setEstado(Turno.PENDIENTE);
        turnoDAO.guardar(turno);
    }

    /**
     * Confirma un turno que está en estado PENDIENTE.
     */
    @Transactional
    public void confirmar(Long id) {
        Turno turno = obtenerYValidar(id);
        validarTransicion(turno, Turno.PENDIENTE, Turno.CONFIRMADO);
        turnoDAO.cambiarEstado(id, Turno.CONFIRMADO);
    }

    /**
     * Marca un turno CONFIRMADO como REALIZADO.
     */
    @Transactional
    public void marcarRealizado(Long id) {
        Turno turno = obtenerYValidar(id);
        validarTransicion(turno, Turno.CONFIRMADO, Turno.REALIZADO);
        turnoDAO.cambiarEstado(id, Turno.REALIZADO);
    }

    /**
     * Cancela un turno PENDIENTE o CONFIRMADO.
     */
    @Transactional
    public void cancelar(Long id) {
        Turno turno = obtenerYValidar(id);
        if (!turno.isModificable()) {
            throw new IllegalStateException(
                "No se puede cancelar un turno en estado: "
                + turno.getEstado() + ".");
        }
        turnoDAO.cambiarEstado(id, Turno.CANCELADO);
    }

    /**
     * Baja física del turno (DELETE).
     */
    @Transactional
    public void eliminar(Long id) {
        turnoDAO.eliminar(id);
    }

    // ── Helpers privados ──────────────────────────────────────────

    private Turno obtenerYValidar(Long id) {
        Turno t = turnoDAO.buscarPorId(id);
        if (t == null) {
            throw new IllegalArgumentException("Turno no encontrado: " + id);
        }
        return t;
    }

    private void validarTransicion(Turno turno,
                                   String estadoRequerido,
                                   String estadoDestino) {
        if (!estadoRequerido.equals(turno.getEstado())) {
            throw new IllegalStateException(
                "El turno debe estar en estado " + estadoRequerido
                + " para pasar a " + estadoDestino
                + ". Estado actual: " + turno.getEstado() + ".");
        }
    }
}
