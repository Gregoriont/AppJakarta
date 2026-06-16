package com.veterinaria.controlador;

import com.veterinaria.modelo.Mascota;
import com.veterinaria.modelo.Turno;
import com.veterinaria.servicio.MascotaServicio;
import com.veterinaria.servicio.TurnoServicio;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;

/**
 * Managed Bean para la gestión de Turnos.
 * Permite reservar nuevos turnos y gestionar el ciclo de vida:
 *   PENDIENTE → CONFIRMADO → REALIZADO
 *            ↘ CANCELADO
 *
 * @Named      → accesible desde EL en las vistas XHTML
 * @ViewScoped → vive mientras el usuario permanece en la misma vista
 */
@Named("turnoBean")
@ViewScoped
public class TurnoBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private TurnoServicio   turnoServicio;

    @Inject
    private MascotaServicio mascotaServicio;

    // ── Estado de la vista ────────────────────────────────────────
    private List<Turno>   turnos;
    private List<Mascota> mascotas;          // para el selector de la reserva
    private Turno          turnoSeleccionado;
    private Long           mascotaIdSeleccionada;
    private boolean        soloVigentes = false;

    // ── Inicialización ────────────────────────────────────────────

    @PostConstruct
    public void init() {
        turnoSeleccionado    = new Turno();
        mascotaIdSeleccionada = null;
        mascotas             = mascotaServicio.listarActivas();
        cargarTurnos();
    }

    // ── Carga de datos ────────────────────────────────────────────

    public void cargarTurnos() {
        turnos = soloVigentes
                 ? turnoServicio.listarVigentes()
                 : turnoServicio.listarTodos();
    }

    // ── Acciones del formulario ───────────────────────────────────

    /**
     * Limpia el formulario de reserva para un nuevo turno.
     */
    public void prepararNuevo() {
        turnoSeleccionado    = new Turno();
        mascotaIdSeleccionada = null;
    }

    /**
     * Reserva un nuevo turno.
     * Delega la validación al TurnoServicio.
     */
    public void reservar() {
        try {
            turnoServicio.reservar(turnoSeleccionado, mascotaIdSeleccionada);
            cargarTurnos();
            prepararNuevo();
            addMsg(FacesMessage.SEVERITY_INFO,
                "Turno reservado",
                "El turno fue registrado con estado PENDIENTE.");
        } catch (IllegalArgumentException e) {
            addMsg(FacesMessage.SEVERITY_WARN, "Atención", e.getMessage());
        } catch (IllegalStateException e) {
            addMsg(FacesMessage.SEVERITY_WARN, "Estado inválido", e.getMessage());
        } catch (Exception e) {
            addMsg(FacesMessage.SEVERITY_ERROR,
                "Error", "No se pudo reservar el turno. Intente nuevamente.");
        }
    }

    // ── Transiciones de estado ────────────────────────────────────

    /**
     * PENDIENTE → CONFIRMADO
     */
    public void confirmar(Turno t) {
        try {
            turnoServicio.confirmar(t.getId());
            cargarTurnos();
            addMsg(FacesMessage.SEVERITY_INFO,
                "Confirmado",
                "El turno de " + t.getMascota().getNombre() + " fue confirmado.");
        } catch (IllegalStateException e) {
            addMsg(FacesMessage.SEVERITY_WARN, "No permitido", e.getMessage());
        } catch (Exception e) {
            addMsg(FacesMessage.SEVERITY_ERROR,
                "Error", "No se pudo confirmar el turno.");
        }
    }

    /**
     * CONFIRMADO → REALIZADO
     */
    public void marcarRealizado(Turno t) {
        try {
            turnoServicio.marcarRealizado(t.getId());
            cargarTurnos();
            addMsg(FacesMessage.SEVERITY_INFO,
                "Realizado",
                "El turno de " + t.getMascota().getNombre()
                + " fue marcado como realizado.");
        } catch (IllegalStateException e) {
            addMsg(FacesMessage.SEVERITY_WARN, "No permitido", e.getMessage());
        } catch (Exception e) {
            addMsg(FacesMessage.SEVERITY_ERROR,
                "Error", "No se pudo marcar el turno como realizado.");
        }
    }

    /**
     * PENDIENTE o CONFIRMADO → CANCELADO
     */
    public void cancelarTurno(Turno t) {
        try {
            turnoServicio.cancelar(t.getId());
            cargarTurnos();
            addMsg(FacesMessage.SEVERITY_INFO,
                "Cancelado",
                "El turno de " + t.getMascota().getNombre() + " fue cancelado.");
        } catch (IllegalStateException e) {
            addMsg(FacesMessage.SEVERITY_WARN, "No permitido", e.getMessage());
        } catch (Exception e) {
            addMsg(FacesMessage.SEVERITY_ERROR,
                "Error", "No se pudo cancelar el turno.");
        }
    }

    /**
     * Baja física del registro del turno.
     */
    public void eliminar(Turno t) {
        try {
            turnoServicio.eliminar(t.getId());
            cargarTurnos();
            addMsg(FacesMessage.SEVERITY_INFO,
                "Eliminado", "El turno fue eliminado del sistema.");
        } catch (Exception e) {
            addMsg(FacesMessage.SEVERITY_ERROR,
                "Error", "No se pudo eliminar el turno.");
        }
    }

    /**
     * Alterna entre ver todos los turnos / solo los vigentes.
     */
    public void toggleFiltro() {
        soloVigentes = !soloVigentes;
        cargarTurnos();
    }

    // ── Helper de mensajes ────────────────────────────────────────

    private void addMsg(FacesMessage.Severity sev, String resumen, String detalle) {
        FacesContext.getCurrentInstance()
            .addMessage(null, new FacesMessage(sev, resumen, detalle));
    }

    // ── Getters y Setters ─────────────────────────────────────────

    public List<Turno>   getTurnos()                     { return turnos; }
    public List<Mascota> getMascotas()                   { return mascotas; }

    public Turno  getTurnoSeleccionado()                 { return turnoSeleccionado; }
    public void   setTurnoSeleccionado(Turno t)          { this.turnoSeleccionado = t; }

    public Long   getMascotaIdSeleccionada()             { return mascotaIdSeleccionada; }
    public void   setMascotaIdSeleccionada(Long id)      { this.mascotaIdSeleccionada = id; }

    public boolean isSoloVigentes()                      { return soloVigentes; }
    public void    setSoloVigentes(boolean v)            { this.soloVigentes = v; }

    /** Texto dinámico para el botón de filtro */
    public String getTextoFiltro() {
        return soloVigentes ? "Ver todos los turnos" : "Ver solo vigentes";
    }
}
