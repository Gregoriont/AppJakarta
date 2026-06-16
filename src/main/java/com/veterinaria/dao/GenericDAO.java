package com.veterinaria.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;

/**
 * DAO genérico abstracto con las operaciones CRUD básicas.
 * El EntityManager es inyectado por el contenedor GlassFish (JTA).
 *
 * @param <T>  Tipo de la entidad JPA
 * @param <ID> Tipo de la clave primaria
 */
public abstract class GenericDAO<T, ID> {

    // Inyección del EntityManager gestionado por el contenedor
    @PersistenceContext(unitName = "VeterinariaaPU")
    protected EntityManager em;

    /**
     * Cada subclase devuelve su clase de entidad concreta.
     * Necesario para las operaciones genéricas (find, JPQL dinámico).
     */
    protected abstract Class<T> getEntityClass();

    // ── Escritura ─────────────────────────────────────────────────

    /**
     * Persiste una entidad nueva en la base de datos (INSERT).
     */
    @Transactional
    public void guardar(T entidad) {
        em.persist(entidad);
    }

    /**
     * Fusiona el estado de una entidad existente (UPDATE).
     * Devuelve la instancia gestionada actualizada.
     */
    @Transactional
    public T actualizar(T entidad) {
        return em.merge(entidad);
    }

    /**
     * Elimina un registro por su ID (DELETE físico).
     * Si el ID no existe, no hace nada.
     */
    @Transactional
    public void eliminar(ID id) {
        T entidad = em.find(getEntityClass(), id);
        if (entidad != null) {
            em.remove(entidad);
        }
    }

    // ── Lectura ───────────────────────────────────────────────────

    /**
     * Busca una entidad por su clave primaria.
     * Devuelve null si no existe.
     */
    public T buscarPorId(ID id) {
        return em.find(getEntityClass(), id);
    }

    /**
     * Lista todas las entidades del tipo sin ordenamiento específico.
     * Las subclases pueden sobreescribir con NamedQueries ordenadas.
     */
    public List<T> listarTodos() {
        String jpql = "SELECT e FROM "
                    + getEntityClass().getSimpleName()
                    + " e";
        return em.createQuery(jpql, getEntityClass()).getResultList();
    }

    /**
     * Cuenta el total de registros de la entidad.
     */
    public long contarTodos() {
        String jpql = "SELECT COUNT(e) FROM "
                    + getEntityClass().getSimpleName()
                    + " e";
        return (long) em.createQuery(jpql).getSingleResult();
    }
}
