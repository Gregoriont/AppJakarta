package com.veterinaria.controlador;

import com.veterinaria.modelo.Cliente;
import com.veterinaria.modelo.Mascota;
import com.veterinaria.servicio.ClienteServicio;
import com.veterinaria.servicio.MascotaServicio;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;

/**
 * Managed Bean para el ABM de Mascotas.
 *
 * @Named      → accesible desde EL en las vistas XHTML
 * @ViewScoped → vive mientras el usuario permanece en la misma vista
 */
@Named("mascotaBean")
@ViewScoped
public class MascotaBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private MascotaServicio mascotaServicio;

    @Inject
    private ClienteServicio clienteServicio;

    // ── Estado de la vista ────────────────────────────────────────
    private List<Mascota> mascotas;
    private List<Cliente> clientes;           // para el selector de dueño
    private Mascota        mascotaSeleccionada;
    private Long           clienteIdSeleccionado;
    private String         textoBusqueda;
    private boolean        modoEdicion = false;

    // Listas fijas de opciones para los selectores
    private static final String[] ESPECIES = {
        "Perro", "Gato", "Conejo", "Ave", "Reptil", "Roedor", "Otro"
    };

    // ── Inicialización ────────────────────────────────────────────

    @PostConstruct
    public void init() {
        mascotaSeleccionada   = new Mascota();
        clienteIdSeleccionado = null;
        clientes              = clienteServicio.listarActivos();
        cargarMascotas();
    }

    // ── Carga de datos ────────────────────────────────────────────

    public void cargarMascotas() {
        mascotas = mascotaServicio.listarTodas();
    }

    // ── Acciones del formulario ───────────────────────────────────

    /**
     * Prepara el formulario para dar de alta una nueva mascota.
     */
    public void prepararNuevo() {
        mascotaSeleccionada   = new Mascota();
        clienteIdSeleccionado = null;
        textoBusqueda         = null;
        modoEdicion           = false;
    }

    /**
     * Carga la mascota seleccionada en el formulario para editarla.
     */
    public void prepararEdicion(Mascota m) {
        mascotaSeleccionada   = mascotaServicio.buscarPorId(m.getId());
        clienteIdSeleccionado = mascotaSeleccionada.getCliente().getId();
        modoEdicion           = true;
    }

    /**
     * Guarda (alta o modificación) la mascota del formulario.
     */
    public void guardar() {
        try {
            mascotaServicio.guardar(mascotaSeleccionada, clienteIdSeleccionado);
            cargarMascotas();
            mascotaSeleccionada   = new Mascota();
            clienteIdSeleccionado = null;
            modoEdicion           = false;
            addMsg(FacesMessage.SEVERITY_INFO,
                "Éxito", "Mascota guardada correctamente.");
        } catch (IllegalArgumentException e) {
            addMsg(FacesMessage.SEVERITY_WARN, "Atención", e.getMessage());
        } catch (Exception e) {
            addMsg(FacesMessage.SEVERITY_ERROR,
                "Error", "No se pudo guardar la mascota. Intente nuevamente.");
        }
    }

    /**
     * Baja física de la mascota.
     * Falla con mensaje si tiene turnos asociados (FK).
     */
    public void eliminar(Mascota m) {
        try {
            mascotaServicio.eliminar(m.getId());
            cargarMascotas();
            addMsg(FacesMessage.SEVERITY_INFO,
                "Eliminada", m.getNombre() + " fue eliminada correctamente.");
        } catch (Exception e) {
            addMsg(FacesMessage.SEVERITY_ERROR,
                "No se puede eliminar",
                m.getNombre() + " tiene turnos asociados. Use 'Desactivar'.");
        }
    }

    /**
     * Baja lógica: marca activo = false.
     */
    public void desactivar(Mascota m) {
        try {
            mascotaServicio.desactivar(m.getId());
            cargarMascotas();
            addMsg(FacesMessage.SEVERITY_INFO,
                "Desactivada", m.getNombre() + " fue desactivada.");
        } catch (Exception e) {
            addMsg(FacesMessage.SEVERITY_ERROR,
                "Error", "No se pudo desactivar la mascota.");
        }
    }

    /**
     * Reactiva una mascota desactivada.
     */
    public void activar(Mascota m) {
        try {
            mascotaServicio.activar(m.getId());
            cargarMascotas();
            addMsg(FacesMessage.SEVERITY_INFO,
                "Activada", m.getNombre() + " fue activada.");
        } catch (Exception e) {
            addMsg(FacesMessage.SEVERITY_ERROR,
                "Error", "No se pudo activar la mascota.");
        }
    }

    /**
     * Búsqueda por nombre de mascota.
     */
    public void buscar() {
        mascotas = mascotaServicio.buscar(textoBusqueda);
    }

    /**
     * Cancela la edición y restaura el estado inicial de la vista.
     */
    public void cancelar() {
        mascotaSeleccionada   = new Mascota();
        clienteIdSeleccionado = null;
        textoBusqueda         = null;
        modoEdicion           = false;
        cargarMascotas();
    }

    // ── Helper de mensajes ────────────────────────────────────────

    private void addMsg(FacesMessage.Severity sev, String resumen, String detalle) {
        FacesContext.getCurrentInstance()
            .addMessage(null, new FacesMessage(sev, resumen, detalle));
    }

    // ── Getters y Setters ─────────────────────────────────────────

    public List<Mascota> getMascotas()               { return mascotas; }
    public List<Cliente> getClientes()               { return clientes; }
    public String[]      getEspecies()               { return ESPECIES; }

    public Mascota getMascotaSeleccionada()           { return mascotaSeleccionada; }
    public void    setMascotaSeleccionada(Mascota m)  { this.mascotaSeleccionada = m; }

    public Long   getClienteIdSeleccionado()          { return clienteIdSeleccionado; }
    public void   setClienteIdSeleccionado(Long id)   { this.clienteIdSeleccionado = id; }

    public String getTextoBusqueda()                  { return textoBusqueda; }
    public void   setTextoBusqueda(String t)          { this.textoBusqueda = t; }

    public boolean isModoEdicion()                    { return modoEdicion; }
    public void    setModoEdicion(boolean m)          { this.modoEdicion = m; }
}
