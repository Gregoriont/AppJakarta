package com.veterinaria.dao;

import com.veterinaria.modelo.Mascota;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;

/**
 * DAO para la entidad Mascota.
 * Todas las consultas usan JOIN FETCH para traer el Cliente asociado
 * en una sola consulta SQL y evitar el problema de N+1 queries.
 */
@ApplicationScoped
public class MascotaDAO extends GenericDAO<Mascota, Long> {

    @Override
    protected Class<Mascota> getEntityClass() {
        return Mascota.class;
    }

    // ── Consultas específicas ─────────────────────────────────────

    /**
     * Lista todas las mascotas con JOIN FETCH al cliente (evita N+1).
     */
    public List<Mascota> listarTodas() {
        return em.createNamedQuery("Mascota.findAll", Mascota.class)
                 .getResultList();
    }

    /**
     * Lista solo mascotas activas con JOIN FETCH al cliente.
     */
    public List<Mascota> listarActivas() {
        return em.createNamedQuery("Mascota.findActivas", Mascota.class)
                 .getResultList();
    }

    /**
     * Mascotas activas de un cliente específico.
     */
    public List<Mascota> listarPorCliente(Long clienteId) {
        return em.createNamedQuery("Mascota.findByCliente", Mascota.class)
                 .setParameter("clienteId", clienteId)
                 .getResultList();
    }

    /**
     * Búsqueda parcial por nombre de mascota (case-insensitive).
     */
    public List<Mascota> buscarPorNombre(String texto) {
        String patron = "%" + texto.toLowerCase().trim() + "%";
        return em.createNamedQuery("Mascota.buscarPorNombre", Mascota.class)
                 .setParameter("patron", patron)
                 .getResultList();
    }

    /**
     * Baja lógica: marca activo = false.
     */
    @Transactional
    public void desactivar(Long id) {
        Mascota m = em.find(Mascota.class, id);
        if (m != null) {
            m.setActivo(false);
            em.merge(m);
        }
    }

    /**
     * Reactiva una mascota previamente desactivada.
     */
    @Transactional
    public void activar(Long id) {
        Mascota m = em.find(Mascota.class, id);
        if (m != null) {
            m.setActivo(true);
            em.merge(m);
        }
    }
}
