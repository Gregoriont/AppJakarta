package com.veterinaria.servicio;

import com.veterinaria.dao.ClienteDAO;
import com.veterinaria.modelo.Cliente;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

/**
 * Servicio de negocio para Cliente.
 * Encapsula las reglas de negocio y delega la persistencia al DAO.
 *
 * @ApplicationScoped → única instancia durante la vida de la aplicación.
 */
@ApplicationScoped
public class ClienteServicio {

    @Inject
    private ClienteDAO clienteDAO;

    // ── Consultas ─────────────────────────────────────────────────

    public List<Cliente> listarTodos() {
        return clienteDAO.listarTodos();
    }

    public List<Cliente> listarActivos() {
        return clienteDAO.listarActivos();
    }

    public Cliente buscarPorId(Long id) {
        return clienteDAO.buscarPorId(id);
    }

    /**
     * Búsqueda flexible: busca por nombre, apellido o documento.
     * Si el texto está vacío devuelve todos.
     */
    public List<Cliente> buscar(String texto) {
        if (texto == null || texto.isBlank()) {
            return clienteDAO.listarTodos();
        }
        return clienteDAO.buscarPorTexto(texto.trim());
    }

    // ── Comandos ──────────────────────────────────────────────────

    /**
     * Alta o modificación de un cliente.
     *
     * Reglas de negocio:
     *  - El documento debe ser único entre todos los clientes.
     *  - El email debe ser único entre todos los clientes.
     */
    @Transactional
    public void guardar(Cliente cliente) {

        // Validar unicidad de documento
        clienteDAO.buscarPorDocumento(cliente.getDocumento())
            .ifPresent(existente -> {
                boolean mismo = existente.getId() != null
                        && existente.getId().equals(cliente.getId());
                if (!mismo) {
                    throw new IllegalArgumentException(
                        "Ya existe un cliente con el documento: "
                        + cliente.getDocumento());
                }
            });

        // Validar unicidad de email
        clienteDAO.buscarPorEmail(cliente.getEmail())
            .ifPresent(existente -> {
                boolean mismo = existente.getId() != null
                        && existente.getId().equals(cliente.getId());
                if (!mismo) {
                    throw new IllegalArgumentException(
                        "Ya existe un cliente con el email: "
                        + cliente.getEmail());
                }
            });

        if (cliente.getId() == null) {
            clienteDAO.guardar(cliente);
        } else {
            clienteDAO.actualizar(cliente);
        }
    }

    /**
     * Baja física del cliente.
     * Lanzará excepción si tiene mascotas asociadas (FK constraint).
     */
    @Transactional
    public void eliminar(Long id) {
        clienteDAO.eliminar(id);
    }

    /**
     * Baja lógica: marca activo = false sin eliminar el registro.
     */
    @Transactional
    public void desactivar(Long id) {
        clienteDAO.desactivar(id);
    }

    /**
     * Reactiva un cliente desactivado.
     */
    @Transactional
    public void activar(Long id) {
        clienteDAO.activar(id);
    }
}
