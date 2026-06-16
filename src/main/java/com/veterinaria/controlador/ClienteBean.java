package com.veterinaria.controlador;

import com.veterinaria.modelo.Cliente;
import com.veterinaria.servicio.ClienteServicio;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;

/**
 * Managed Bean para el ABM de Clientes.
 *
 * @Named      → accesible desde Expression Language en las vistas XHTML
 * @ViewScoped → vive mientras el usuario permanece en la misma vista JSF
 */
@Named("clienteBean")
@ViewScoped
public class ClienteBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ClienteServicio clienteServicio;

    // ── Estado de la vista ────────────────────────────────────────
    private List<Cliente> clientes;
    private Cliente        clienteSeleccionado;
    private String         textoBusqueda;
    private boolean        modoEdicion = false;

    // ── Inicialización ────────────────────────────────────────────

    /**
     * Se ejecuta una vez al crear el bean (al cargar la vista).
     */
    @PostConstruct
    public void init() {
        clienteSeleccionado = new Cliente();
        cargarClientes();
    }

    // ── Carga de datos ────────────────────────────────────────────

    public void cargarClientes() {
        clientes = clienteServicio.listarTodos();
    }

    // ── Acciones del formulario ───────────────────────────────────

    /**
     * Prepara el formulario para dar de alta un nuevo cliente.
     */
    public void prepararNuevo() {
        clienteSeleccionado = new Cliente();
        textoBusqueda       = null;
        modoEdicion         = false;
    }

    /**
     * Carga el cliente en el formulario para editarlo.
     * Recarga desde BD para garantizar datos frescos.
     */
    public void prepararEdicion(Cliente c) {
        clienteSeleccionado = clienteServicio.buscarPorId(c.getId());
        modoEdicion         = true;
    }

    /**
     * Guarda (alta o modificación) el cliente del formulario.
     */
    public void guardar() {
        try {
            clienteServicio.guardar(clienteSeleccionado);
            cargarClientes();
            clienteSeleccionado = new Cliente();
            modoEdicion         = false;
            addMsg(FacesMessage.SEVERITY_INFO,
                "Éxito", "Cliente guardado correctamente.");
        } catch (IllegalArgumentException e) {
            addMsg(FacesMessage.SEVERITY_WARN, "Atención", e.getMessage());
        } catch (Exception e) {
            addMsg(FacesMessage.SEVERITY_ERROR,
                "Error", "No se pudo guardar el cliente. Intente nuevamente.");
        }
    }

    /**
     * Baja física del cliente.
     * Falla con mensaje si tiene mascotas asociadas (FK).
     */
    public void eliminar(Cliente c) {
        try {
            clienteServicio.eliminar(c.getId());
            cargarClientes();
            addMsg(FacesMessage.SEVERITY_INFO,
                "Eliminado", "El cliente fue eliminado correctamente.");
        } catch (Exception e) {
            addMsg(FacesMessage.SEVERITY_ERROR,
                "No se puede eliminar",
                "El cliente tiene mascotas asociadas. Use 'Desactivar'.");
        }
    }

    /**
     * Baja lógica: pasa activo = false.
     */
    public void desactivar(Cliente c) {
        try {
            clienteServicio.desactivar(c.getId());
            cargarClientes();
            addMsg(FacesMessage.SEVERITY_INFO,
                "Desactivado",
                c.getNombreCompleto() + " fue desactivado.");
        } catch (Exception e) {
            addMsg(FacesMessage.SEVERITY_ERROR,
                "Error", "No se pudo desactivar el cliente.");
        }
    }

    /**
     * Reactiva un cliente desactivado.
     */
    public void activar(Cliente c) {
        try {
            clienteServicio.activar(c.getId());
            cargarClientes();
            addMsg(FacesMessage.SEVERITY_INFO,
                "Activado",
                c.getNombreCompleto() + " fue activado.");
        } catch (Exception e) {
            addMsg(FacesMessage.SEVERITY_ERROR,
                "Error", "No se pudo activar el cliente.");
        }
    }

    /**
     * Búsqueda por nombre, apellido o documento.
     */
    public void buscar() {
        clientes = clienteServicio.buscar(textoBusqueda);
    }

    /**
     * Cancela la edición y restaura el estado inicial de la vista.
     */
    public void cancelar() {
        clienteSeleccionado = new Cliente();
        textoBusqueda       = null;
        modoEdicion         = false;
        cargarClientes();
    }

    // ── Helper de mensajes ────────────────────────────────────────

    private void addMsg(FacesMessage.Severity sev, String resumen, String detalle) {
        FacesContext.getCurrentInstance()
            .addMessage(null, new FacesMessage(sev, resumen, detalle));
    }

    // ── Getters y Setters ─────────────────────────────────────────

    public List<Cliente> getClientes()             { return clientes; }

    public Cliente getClienteSeleccionado()        { return clienteSeleccionado; }
    public void setClienteSeleccionado(Cliente c)  { this.clienteSeleccionado = c; }

    public String getTextoBusqueda()               { return textoBusqueda; }
    public void setTextoBusqueda(String t)         { this.textoBusqueda = t; }

    public boolean isModoEdicion()                 { return modoEdicion; }
    public void setModoEdicion(boolean m)          { this.modoEdicion = m; }
}
