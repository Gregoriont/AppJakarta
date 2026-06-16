package com.veterinaria.modelo;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cliente")
@NamedQueries({
    @NamedQuery(
        name  = "Cliente.findAll",
        query = "SELECT c FROM Cliente c ORDER BY c.apellido, c.nombre"),
    @NamedQuery(
        name  = "Cliente.findActivos",
        query = "SELECT c FROM Cliente c WHERE c.activo = true ORDER BY c.apellido, c.nombre"),
    @NamedQuery(
        name  = "Cliente.findByDocumento",
        query = "SELECT c FROM Cliente c WHERE c.documento = :documento"),
    @NamedQuery(
        name  = "Cliente.findByEmail",
        query = "SELECT c FROM Cliente c WHERE c.email = :email"),
    @NamedQuery(
        name  = "Cliente.buscarPorTexto",
        query = "SELECT c FROM Cliente c "
              + "WHERE LOWER(c.nombre)    LIKE :patron "
              + "   OR LOWER(c.apellido)  LIKE :patron "
              + "   OR LOWER(c.documento) LIKE :patron "
              + "ORDER BY c.apellido, c.nombre")
})
public class Cliente implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El documento es obligatorio")
    @Size(max = 20, message = "El documento no puede superar 20 caracteres")
    @Column(nullable = false, unique = true, length = 20)
    private String documento;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    @Column(nullable = false, length = 100)
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede superar 100 caracteres")
    @Column(nullable = false, length = 100)
    private String apellido;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    @Size(max = 150, message = "El email no puede superar 150 caracteres")
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
    @Column(length = 20)
    private String telefono;

    @Size(max = 200, message = "La dirección no puede superar 200 caracteres")
    @Column(length = 200)
    private String direccion;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private LocalDateTime fechaAlta;

    // Relación: un cliente tiene muchas mascotas
    @OneToMany(
        mappedBy      = "cliente",
        cascade       = CascadeType.ALL,
        fetch         = FetchType.LAZY,
        orphanRemoval = true
    )
    private List<Mascota> mascotas = new ArrayList<>();

    // ── Callback JPA ──────────────────────────────────────────────
    @PrePersist
    protected void onCreate() {
        this.fechaAlta = LocalDateTime.now();
    }

    // ── Getters y Setters ─────────────────────────────────────────
    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }

    public String getDocumento()               { return documento; }
    public void setDocumento(String d)         { this.documento = d; }

    public String getNombre()                  { return nombre; }
    public void setNombre(String nombre)       { this.nombre = nombre; }

    public String getApellido()                { return apellido; }
    public void setApellido(String apellido)   { this.apellido = apellido; }

    public String getEmail()                   { return email; }
    public void setEmail(String email)         { this.email = email; }

    public String getTelefono()                { return telefono; }
    public void setTelefono(String t)          { this.telefono = t; }

    public String getDireccion()               { return direccion; }
    public void setDireccion(String d)         { this.direccion = d; }

    public boolean isActivo()                  { return activo; }
    public void setActivo(boolean activo)      { this.activo = activo; }

    public LocalDateTime getFechaAlta()        { return fechaAlta; }
    public void setFechaAlta(LocalDateTime f)  { this.fechaAlta = f; }

    public List<Mascota> getMascotas()         { return mascotas; }
    public void setMascotas(List<Mascota> m)   { this.mascotas = m; }

    // ── Helpers de vista ──────────────────────────────────────────
    public String getNombreCompleto() {
        return apellido + ", " + nombre;
    }

    @Override
    public String toString() {
        return getNombreCompleto() + " [" + documento + "]";
    }
}
