package com.veterinaria.servicio;

import com.veterinaria.dao.ClienteDAO;
import com.veterinaria.dao.MascotaDAO;
import com.veterinaria.modelo.Cliente;
import com.veterinaria.modelo.Mascota;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

/**
 * Servicio de negocio para Mascota.
 * Encapsula las reglas de negocio y delega la persistencia al DAO.
 */
@ApplicationScoped
public class MascotaServicio {

    @Inject
    private MascotaDAO mascotaDAO;

    @Inject
    private ClienteDAO clienteDAO;

    // ── Consultas ─────────────────────────────────────────────────

    public List<Mascota> listarTodas() {
        return mascotaDAO.listarTodas();
    }

    public List<Mascota> listarActivas() {
        return mascotaDAO.listarActivas();
    }

    public Mascota buscarPorId(Long id) {
        return mascotaDAO.buscarPorId(id);
    }

    /**
     * Mascotas activas de un cliente específico.
     */
    public List<Mascota> listarPorCliente(Long clienteId) {
        return mascotaDAO.listarPorCliente(clienteId);
    }

    /**
     * Búsqueda por nombre de mascota. Si está vacío devuelve todas.
     */
    public List<Mascota> buscar(String texto) {
        if (texto == null || texto.isBlank()) {
            return mascotaDAO.listarTodas();
        }
        return mascotaDAO.buscarPorNombre(texto.trim());
    }

    // ── Comandos ──────────────────────────────────────────────────

    /**
     * Alta o modificación de una mascota.
     *
     * Reglas de negocio:
     *  - El clienteId debe corresponder a un cliente existente.
     *  - El cliente dueño debe estar activo.
     *
     * @param mascota   entidad a persistir (sin relación resuelta)
     * @param clienteId ID del dueño seleccionado en el formulario
     */
    @Transactional
    public void guardar(Mascota mascota, Long clienteId) {

        if (clienteId == null) {
            throw new IllegalArgumentException(
                "Debe seleccionar el dueño de la mascota.");
        }

        Cliente cliente = clienteDAO.buscarPorId(clienteId);
        if (cliente == null) {
            throw new IllegalArgumentException(
                "El cliente seleccionado no existe.");
        }
        if (!cliente.isActivo()) {
            throw new IllegalArgumentException(
                "No se puede asignar una mascota a un cliente inactivo.");
        }

        mascota.setCliente(cliente);

        if (mascota.getId() == null) {
            mascotaDAO.guardar(mascota);
        } else {
            mascotaDAO.actualizar(mascota);
        }
    }

    /**
     * Baja física de una mascota.
     * Lanzará excepción si tiene turnos asociados (FK constraint).
     */
    @Transactional
    public void eliminar(Long id) {
        mascotaDAO.eliminar(id);
    }

    /**
     * Baja lógica: marca activo = false.
     */
    @Transactional
    public void desactivar(Long id) {
        mascotaDAO.desactivar(id);
    }

    /**
     * Reactiva una mascota desactivada.
     */
    @Transactional
    public void activar(Long id) {
        mascotaDAO.activar(id);
    }
}
