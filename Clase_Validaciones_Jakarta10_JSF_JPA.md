# Clase Completa: Validaciones en los Distintos Niveles
## Aplicaciones con Jakarta Server Faces (JSF), JPA y Jakarta EE 10

---

**Cátedra:** Desarrollo de Aplicaciones Web  
**Tecnologías:** Jakarta EE 10, Jakarta Server Faces (JSF 4.0), JPA 3.1, Bean Validation 3.0  
**Docente:** [Nombre del Docente]  
**Fecha:** Junio 2026

---

## 1. Introducción

### 1.1 ¿Por qué validar en múltiples niveles?

En una aplicación empresarial, la validación no es un paso único sino una **estrategia de defensa en profundidad** (*defense in depth*). Cada capa de la arquitectura tiene responsabilidades específicas y puede recibir datos desde distintas fuentes (formularios web, APIs REST, procesos batch, etc.).

**Principio fundamental:** Nunca confiar en que la capa anterior ya validó correctamente.

### 1.2 Arquitectura de Referencia

```
┌─────────────────────────────────────────────────────────┐
│                    CLIENTE (Navegador)                    │
└─────────────────────┬───────────────────────────────────┘
                      │ HTTP Request
┌─────────────────────▼───────────────────────────────────┐
│  NIVEL 1: Vista (JSF)          - Validación de entrada  │
├─────────────────────────────────────────────────────────┤
│  NIVEL 2: Bean Validation      - Reglas sobre entidad   │
├─────────────────────────────────────────────────────────┤
│  NIVEL 3: Controlador/Servicio - Reglas de negocio      │
├─────────────────────────────────────────────────────────┤
│  NIVEL 4: Base de Datos        - Integridad física      │
├─────────────────────────────────────────────────────────┤
│  NIVEL 5: Seguridad            - Acceso y permisos      │
└─────────────────────────────────────────────────────────┘
```


### 1.3 Tabla Resumen de Niveles

| Nivel | Dónde se realiza | Objetivo | Tecnología en Jakarta 10 |
|-------|-------------------|-----------|--------------------------|
| 1 | Vista (JSF) | Verificar datos ingresados por el usuario | JSF Validators, `f:validateXxx` |
| 2 | Bean Validation | Validar reglas sobre la entidad | Jakarta Bean Validation 3.0 (`jakarta.validation`) |
| 3 | Controlador / Servicio | Aplicar reglas de negocio | CDI Beans, EJB, validación programática |
| 4 | Base de Datos | Garantizar integridad física | JPA Constraints, DDL Constraints |
| 5 | Seguridad | Controlar acceso y permisos | Jakarta Security 3.0, `@RolesAllowed` |

---

## 2. NIVEL 1: Validación en la Vista (JSF)

### 2.1 Definición

La validación en la vista es la **primera línea de defensa**. Se ejecuta durante la fase de *Process Validations* del ciclo de vida de JSF (fase 3 de 6). Su objetivo es rechazar rápidamente datos mal formados antes de que lleguen al modelo.

### 2.2 Ciclo de Vida de JSF y la Validación

```
Fase 1: Restore View
Fase 2: Apply Request Values
Fase 3: Process Validations  ← AQUÍ SE EJECUTAN LAS VALIDACIONES
Fase 4: Update Model Values
Fase 5: Invoke Application
Fase 6: Render Response
```

**Concepto clave:** Si la validación falla en Fase 3, JSF salta directamente a Fase 6 (Render Response), mostrando los mensajes de error sin ejecutar la lógica de negocio.

### 2.3 Tipos de Validación en JSF

#### 2.3.1 Validadores Estándar (Built-in)

JSF proporciona validadores incorporados como tags en el namespace `jakarta.faces.core`:


```xml
<!-- Ejemplo: Formulario de registro de estudiante -->
<h:form id="registroForm">
    
    <!-- Validación de longitud -->
    <h:outputLabel for="nombre" value="Nombre:" />
    <h:inputText id="nombre" value="#{estudianteBean.nombre}" required="true"
                 requiredMessage="El nombre es obligatorio">
        <f:validateLength minimum="2" maximum="100" />
    </h:inputText>
    <h:message for="nombre" styleClass="error" />
    
    <!-- Validación de rango numérico -->
    <h:outputLabel for="edad" value="Edad:" />
    <h:inputText id="edad" value="#{estudianteBean.edad}" required="true">
        <f:validateLongRange minimum="16" maximum="99" />
    </h:inputText>
    <h:message for="edad" styleClass="error" />
    
    <!-- Validación de patrón (expresión regular) -->
    <h:outputLabel for="email" value="Email:" />
    <h:inputText id="email" value="#{estudianteBean.email}" required="true">
        <f:validateRegex pattern="^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$" />
    </h:inputText>
    <h:message for="email" styleClass="error" />
    
    <!-- Validación de rango para decimales -->
    <h:outputLabel for="promedio" value="Promedio:" />
    <h:inputText id="promedio" value="#{estudianteBean.promedio}">
        <f:validateDoubleRange minimum="0.0" maximum="10.0" />
    </h:inputText>
    <h:message for="promedio" styleClass="error" />
    
    <h:commandButton value="Registrar" action="#{estudianteBean.registrar}" />
    <h:messages globalOnly="true" styleClass="error-global" />
</h:form>
```

**Validadores estándar disponibles:**

| Tag | Descripción | Atributos principales |
|-----|-------------|----------------------|
| `f:validateLength` | Valida longitud de String | `minimum`, `maximum` |
| `f:validateLongRange` | Valida rango de enteros | `minimum`, `maximum` |
| `f:validateDoubleRange` | Valida rango de decimales | `minimum`, `maximum` |
| `f:validateRegex` | Valida contra expresión regular | `pattern` |
| `f:validateRequired` | Marca campo como obligatorio | - |
| `f:validateBean` | Activa Bean Validation | `validationGroups` |


#### 2.3.2 Atributo `required`

```xml
<!-- El atributo required="true" verifica que el campo no esté vacío -->
<h:inputText id="dni" value="#{estudianteBean.dni}" 
             required="true"
             requiredMessage="El DNI es obligatorio"
             validatorMessage="Formato de DNI inválido">
    <f:validateRegex pattern="\\d{7,8}" />
</h:inputText>
```

**Concepto:** `required="true"` se evalúa ANTES que los validadores. Si el campo está vacío y es requerido, no se ejecutan los demás validadores.

#### 2.3.3 Validadores Personalizados (Custom Validators)

Cuando los validadores estándar no son suficientes, creamos validadores propios:

```java
package ar.edu.universidad.validators;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.Validator;
import jakarta.faces.validator.ValidatorException;

/**
 * Validador personalizado para CUIT/CUIL argentino.
 * Verifica formato y dígito verificador.
 */
@FacesValidator("cuitValidator")
public class CuitValidator implements Validator<String> {

    @Override
    public void validate(FacesContext context, UIComponent component, String value)
            throws ValidatorException {
        
        if (value == null || value.isBlank()) {
            return; // Si es vacío, dejamos que 'required' lo maneje
        }
        
        // Eliminar guiones
        String cuit = value.replace("-", "");
        
        // Verificar longitud
        if (cuit.length() != 11) {
            throw new ValidatorException(
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "CUIT inválido", "El CUIT debe tener 11 dígitos")
            );
        }
        
        // Verificar que sean todos dígitos
        if (!cuit.matches("\\d{11}")) {
            throw new ValidatorException(
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "CUIT inválido", "El CUIT solo debe contener números")
            );
        }
        
        // Verificar dígito verificador
        if (!verificarDigito(cuit)) {
            throw new ValidatorException(
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "CUIT inválido", "El dígito verificador no es correcto")
            );
        }
    }
    
    private boolean verificarDigito(String cuit) {
        int[] multiplicadores = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
        int suma = 0;
        for (int i = 0; i < 10; i++) {
            suma += Character.getNumericValue(cuit.charAt(i)) * multiplicadores[i];
        }
        int resto = suma % 11;
        int digitoEsperado = (resto == 0) ? 0 : (resto == 1) ? 9 : 11 - resto;
        return digitoEsperado == Character.getNumericValue(cuit.charAt(10));
    }
}
```

**Uso en la vista:**

```xml
<h:inputText id="cuit" value="#{proveedorBean.cuit}">
    <f:validator validatorId="cuitValidator" />
</h:inputText>
```


#### 2.3.4 Validación con Método del Bean (Inline Validator)

```xml
<h:inputText id="username" value="#{registroBean.username}"
             validator="#{registroBean.validarUsername}" />
```

```java
@Named
@RequestScoped
public class RegistroBean {
    
    private String username;
    
    public void validarUsername(FacesContext context, UIComponent component, Object value)
            throws ValidatorException {
        String user = (String) value;
        if (user != null && user.contains(" ")) {
            throw new ValidatorException(
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Username inválido", "No se permiten espacios en el nombre de usuario")
            );
        }
    }
    
    // getters y setters...
}
```

#### 2.3.5 Validación Cross-Field (Campos Cruzados)

JSF no soporta nativamente la validación entre múltiples campos en la fase de validación. Se resuelve en la fase de *Invoke Application*:

```java
@Named
@RequestScoped
public class CambioPasswordBean {
    
    private String password;
    private String confirmPassword;
    
    public String cambiarPassword() {
        // Validación cross-field en el método de acción
        if (!password.equals(confirmPassword)) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error", "Las contraseñas no coinciden"));
            return null; // Permanece en la misma página
        }
        // Lógica de cambio de password...
        return "exito?faces-redirect=true";
    }
}
```

#### 2.3.6 Conversores vs Validadores

**Diferencia conceptual importante:**

| Concepto | Conversor | Validador |
|----------|-----------|-----------|
| Propósito | Transformar tipo de dato | Verificar regla de negocio |
| Fase JSF | Apply Request Values (Fase 2) | Process Validations (Fase 3) |
| Ejemplo | String → Date, String → Integer | Rango, longitud, formato |
| Interface | `Converter<T>` | `Validator<T>` |

```java
// Ejemplo de Conversor
@FacesConverter("localDateConverter")
public class LocalDateConverter implements Converter<LocalDate> {
    
    private static final DateTimeFormatter FORMATTER = 
        DateTimeFormatter.ofPattern("dd/MM/yyyy");
    
    @Override
    public LocalDate getAsObject(FacesContext context, UIComponent component, String value) {
        try {
            return LocalDate.parse(value, FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ConverterException(
                new FacesMessage("Formato de fecha inválido. Use dd/MM/yyyy"));
        }
    }
    
    @Override
    public String getAsString(FacesContext context, UIComponent component, LocalDate value) {
        return value != null ? value.format(FORMATTER) : "";
    }
}
```

---


## 3. NIVEL 2: Bean Validation (Jakarta Validation 3.0)

### 3.1 Definición

Bean Validation es una especificación que permite definir **restricciones (constraints) directamente sobre las propiedades de las entidades** mediante anotaciones. Es independiente de la capa de presentación y se integra automáticamente con JSF y JPA.

**Paquete:** `jakarta.validation`  
**Especificación:** Jakarta Bean Validation 3.0 (parte de Jakarta EE 10)  
**Implementación de referencia:** Hibernate Validator 8.x

### 3.2 Ventaja Principal: Validación Declarativa y Reutilizable

Las reglas se definen UNA VEZ en la entidad y se aplican automáticamente en:
- JSF (durante Process Validations)
- JPA (antes del `persist`/`update`)
- Servicios REST (con `@Valid`)
- Cualquier punto donde se invoque el `Validator`

### 3.3 Anotaciones Estándar de Bean Validation

```java
package ar.edu.universidad.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "estudiantes")
public class Estudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- VALIDACIONES DE PRESENCIA ---
    
    @NotNull(message = "El nombre no puede ser nulo")
    @NotBlank(message = "El nombre no puede estar vacío")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre {min} y {max} caracteres")
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 100)
    @Column(name = "apellido", nullable = false, length = 100)
    private String apellido;

    // --- VALIDACIONES DE FORMATO ---
    
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    @Column(name = "email", unique = true)
    private String email;

    @Pattern(regexp = "\\d{7,8}", message = "El DNI debe tener 7 u 8 dígitos")
    @Column(name = "dni", unique = true)
    private String dni;

    // --- VALIDACIONES NUMÉRICAS ---
    
    @Min(value = 16, message = "La edad mínima es {value} años")
    @Max(value = 99, message = "La edad máxima es {value} años")
    @Column(name = "edad")
    private Integer edad;

    @DecimalMin(value = "0.0", message = "El promedio no puede ser negativo")
    @DecimalMax(value = "10.0", message = "El promedio no puede superar {value}")
    @Digits(integer = 2, fraction = 2, message = "Formato: máximo 2 enteros y 2 decimales")
    @Column(name = "promedio", precision = 4, scale = 2)
    private BigDecimal promedio;

    @Positive(message = "El legajo debe ser un número positivo")
    @Column(name = "legajo", unique = true)
    private Integer legajo;

    // --- VALIDACIONES TEMPORALES ---
    
    @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @PastOrPresent(message = "La fecha de ingreso no puede ser futura")
    @Column(name = "fecha_ingreso")
    private LocalDate fechaIngreso;

    @Future(message = "La fecha de graduación estimada debe ser futura")
    @Column(name = "fecha_graduacion_estimada")
    private LocalDate fechaGraduacionEstimada;

    // --- VALIDACIONES DE COLECCIONES ---
    
    @NotEmpty(message = "Debe inscribirse en al menos una materia")
    @Size(max = 7, message = "No puede inscribirse en más de {max} materias")
    @ManyToMany
    private List<Materia> materias;

    // Constructores, getters, setters...
}
```


### 3.4 Tabla Completa de Anotaciones de Bean Validation

| Anotación | Aplica a | Descripción |
|-----------|----------|-------------|
| `@NotNull` | Cualquier tipo | No permite `null` |
| `@Null` | Cualquier tipo | Debe ser `null` |
| `@NotBlank` | String | No `null`, no vacío, no solo espacios |
| `@NotEmpty` | String, Collection, Map, Array | No `null` y no vacío |
| `@Size(min, max)` | String, Collection, Map, Array | Tamaño dentro del rango |
| `@Min(value)` | Números | Valor mínimo |
| `@Max(value)` | Números | Valor máximo |
| `@DecimalMin` | BigDecimal, String | Mínimo decimal |
| `@DecimalMax` | BigDecimal, String | Máximo decimal |
| `@Positive` | Números | Estrictamente > 0 |
| `@PositiveOrZero` | Números | >= 0 |
| `@Negative` | Números | Estrictamente < 0 |
| `@NegativeOrZero` | Números | <= 0 |
| `@Digits(integer, fraction)` | Números | Dígitos enteros y decimales |
| `@Past` | Fechas | Antes de ahora |
| `@PastOrPresent` | Fechas | Antes o igual a ahora |
| `@Future` | Fechas | Después de ahora |
| `@FutureOrPresent` | Fechas | Después o igual a ahora |
| `@Email` | String | Formato email válido |
| `@Pattern(regexp)` | String | Coincide con regex |
| `@AssertTrue` | boolean | Debe ser `true` |
| `@AssertFalse` | boolean | Debe ser `false` |

### 3.5 Validadores Personalizados (Custom Constraints)

#### Paso 1: Definir la anotación

```java
package ar.edu.universidad.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validador personalizado para verificar que un legajo
 * tenga el formato correcto de la universidad.
 * Formato: año(2 dígitos) + carrera(2 dígitos) + secuencia(4 dígitos)
 */
@Documented
@Constraint(validatedBy = LegajoUniversitarioValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface LegajoUniversitario {
    
    String message() default "El formato del legajo es inválido (esperado: XX-XX-XXXX)";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
```

#### Paso 2: Implementar el validador

```java
package ar.edu.universidad.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class LegajoUniversitarioValidator 
        implements ConstraintValidator<LegajoUniversitario, String> {

    @Override
    public void initialize(LegajoUniversitario constraintAnnotation) {
        // Inicialización si es necesario
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Null lo maneja @NotNull
        }
        // Formato: 24-05-0001 (año-carrera-secuencia)
        return value.matches("\\d{2}-\\d{2}-\\d{4}");
    }
}
```

#### Paso 3: Usar en la entidad

```java
@LegajoUniversitario
@Column(name = "legajo_formateado")
private String legajoFormateado;
```


### 3.6 Validación a Nivel de Clase (Class-Level Constraint)

Para validaciones que involucran múltiples campos de una misma entidad:

```java
// Anotación
@Documented
@Constraint(validatedBy = FechasCoherentesValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface FechasCoherentes {
    String message() default "La fecha de fin debe ser posterior a la fecha de inicio";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// Validador
public class FechasCoherentesValidator 
        implements ConstraintValidator<FechasCoherentes, Inscripcion> {

    @Override
    public boolean isValid(Inscripcion inscripcion, ConstraintValidatorContext context) {
        if (inscripcion.getFechaInicio() == null || inscripcion.getFechaFin() == null) {
            return true;
        }
        return inscripcion.getFechaFin().isAfter(inscripcion.getFechaInicio());
    }
}

// Uso en la entidad
@Entity
@FechasCoherentes
public class Inscripcion {
    @NotNull private LocalDate fechaInicio;
    @NotNull private LocalDate fechaFin;
    // ...
}
```

### 3.7 Grupos de Validación (Validation Groups)

Los grupos permiten ejecutar diferentes conjuntos de validaciones según el contexto:

```java
// Definición de grupos (interfaces marcadoras)
public interface CreacionGroup {}
public interface ActualizacionGroup {}
public interface InscripcionGroup {}

// Uso en la entidad
@Entity
public class Estudiante {

    @NotNull(groups = ActualizacionGroup.class)
    @Positive(groups = ActualizacionGroup.class)
    private Long id; // Solo requerido al actualizar

    @NotBlank(groups = {CreacionGroup.class, ActualizacionGroup.class})
    private String nombre;
    
    @NotNull(groups = InscripcionGroup.class)
    @Size(min = 1, groups = InscripcionGroup.class)
    private List<Materia> materias; // Solo requerido al inscribir
}
```

**Uso en JSF con grupos:**

```xml
<!-- Solo aplica validaciones del grupo CreacionGroup -->
<h:inputText id="nombre" value="#{estudianteBean.estudiante.nombre}">
    <f:validateBean validationGroups="ar.edu.universidad.validation.CreacionGroup" />
</h:inputText>
```

### 3.8 Integración Automática JSF + Bean Validation

En Jakarta EE 10, JSF integra automáticamente Bean Validation. Las anotaciones de la entidad se validan durante la fase *Process Validations* sin configuración adicional:

```xml
<!-- Las anotaciones @NotBlank, @Email, etc. se aplican automáticamente -->
<h:inputText id="email" value="#{estudianteBean.estudiante.email}" />
<h:message for="email" />
```

**Para deshabilitar esta integración (raro, pero posible):**

```xml
<!-- En faces-config.xml -->
<faces-config>
    <application>
        <default-validators/>  <!-- Elimina BeanValidator del conjunto por defecto -->
    </application>
</faces-config>
```

---


## 4. NIVEL 3: Validación en Controlador / Servicio (Reglas de Negocio)

### 4.1 Definición

La validación en la capa de servicio implementa **reglas de negocio** que no pueden expresarse como simples restricciones de formato o rango. Estas reglas típicamente:

- Requieren consultar la base de datos
- Dependen del estado actual del sistema
- Involucran lógica compleja entre múltiples entidades
- Pueden variar según el contexto de la operación

### 4.2 Ejemplos de Reglas de Negocio

- Un estudiante no puede inscribirse en más materias si tiene cuotas impagas
- No se puede inscribir en una materia sin tener las correlativas aprobadas
- El cupo máximo de un curso no puede ser superado
- Un docente no puede tener superposición de horarios

### 4.3 Patrón de Implementación con CDI

```java
package ar.edu.universidad.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ar.edu.universidad.entities.*;
import ar.edu.universidad.exceptions.*;
import ar.edu.universidad.repositories.*;
import java.util.List;

@ApplicationScoped
public class InscripcionService {

    @Inject
    private EstudianteRepository estudianteRepo;
    
    @Inject
    private MateriaRepository materiaRepo;
    
    @Inject
    private InscripcionRepository inscripcionRepo;
    
    @Inject
    private CuotaRepository cuotaRepo;

    /**
     * Inscribe un estudiante en una materia aplicando todas las reglas de negocio.
     * 
     * @throws NegocioException si alguna regla de negocio es violada
     */
    @Transactional
    public Inscripcion inscribir(Long estudianteId, Long materiaId) {
        
        // 1. Verificar existencia de entidades
        Estudiante estudiante = estudianteRepo.findById(estudianteId)
            .orElseThrow(() -> new NegocioException("Estudiante no encontrado"));
        
        Materia materia = materiaRepo.findById(materiaId)
            .orElseThrow(() -> new NegocioException("Materia no encontrada"));
        
        // 2. REGLA: No debe estar ya inscripto
        if (inscripcionRepo.existeInscripcion(estudianteId, materiaId)) {
            throw new NegocioException("El estudiante ya está inscripto en esta materia");
        }
        
        // 3. REGLA: Debe tener las correlativas aprobadas
        List<Materia> correlativasPendientes = 
            materiaRepo.findCorrelativasNoAprobadas(estudianteId, materiaId);
        if (!correlativasPendientes.isEmpty()) {
            throw new CorrelativasException(
                "Faltan aprobar correlativas: " + correlativasPendientes);
        }
        
        // 4. REGLA: No debe tener cuotas impagas (más de 2 meses)
        long cuotasImpagas = cuotaRepo.contarCuotasVencidas(estudianteId);
        if (cuotasImpagas > 2) {
            throw new DeudaException(
                "No puede inscribirse con " + cuotasImpagas + " cuotas impagas");
        }
        
        // 5. REGLA: Verificar cupo disponible
        int inscriptos = inscripcionRepo.contarInscriptos(materiaId);
        if (inscriptos >= materia.getCupoMaximo()) {
            throw new CupoExcedidoException(
                "El cupo de la materia está completo (" + materia.getCupoMaximo() + ")");
        }
        
        // 6. REGLA: No puede tener más de 7 materias en el cuatrimestre
        int materiasActuales = inscripcionRepo.contarMateriasActuales(estudianteId);
        if (materiasActuales >= 7) {
            throw new NegocioException(
                "No puede inscribirse en más de 7 materias por cuatrimestre");
        }
        
        // Si todas las reglas pasan, crear la inscripción
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setEstudiante(estudiante);
        inscripcion.setMateria(materia);
        inscripcion.setFechaInscripcion(LocalDate.now());
        inscripcion.setEstado(EstadoInscripcion.ACTIVA);
        
        return inscripcionRepo.save(inscripcion);
    }
}
```


### 4.4 Excepciones Personalizadas para Reglas de Negocio

```java
package ar.edu.universidad.exceptions;

/**
 * Excepción base para errores de reglas de negocio.
 */
public class NegocioException extends RuntimeException {
    
    private String campo;
    
    public NegocioException(String message) {
        super(message);
    }
    
    public NegocioException(String campo, String message) {
        super(message);
        this.campo = campo;
    }
    
    public String getCampo() { return campo; }
}

/**
 * Excepción específica para problemas de correlativas.
 */
public class CorrelativasException extends NegocioException {
    
    private List<Materia> correlativasFaltantes;
    
    public CorrelativasException(String message) {
        super(message);
    }
    
    public CorrelativasException(String message, List<Materia> faltantes) {
        super(message);
        this.correlativasFaltantes = faltantes;
    }
}
```

### 4.5 Integración con el Bean de JSF (Controlador)

```java
package ar.edu.universidad.beans;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import ar.edu.universidad.exceptions.*;
import ar.edu.universidad.services.InscripcionService;

@Named
@RequestScoped
public class InscripcionBean {
    
    @Inject
    private InscripcionService inscripcionService;
    
    @Inject
    private FacesContext facesContext;
    
    private Long estudianteId;
    private Long materiaId;
    
    /**
     * Método de acción que captura excepciones de negocio
     * y las traduce a mensajes JSF.
     */
    public String inscribir() {
        try {
            inscripcionService.inscribir(estudianteId, materiaId);
            
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Inscripción exitosa", "Se ha registrado la inscripción"));
            
            return "confirmacion?faces-redirect=true";
            
        } catch (CorrelativasException e) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Correlativas pendientes", e.getMessage()));
            return null;
            
        } catch (CupoExcedidoException e) {
            facesContext.addMessage("inscripcionForm:materia",
                new FacesMessage(FacesMessage.SEVERITY_WARN,
                    "Sin cupo", e.getMessage()));
            return null;
            
        } catch (NegocioException e) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error de inscripción", e.getMessage()));
            return null;
        }
    }
    
    // Getters y setters...
}
```

### 4.6 Validación Programática con el API de Bean Validation

También se puede invocar Bean Validation de forma programática desde los servicios:

```java
@ApplicationScoped
public class ValidacionService {
    
    @Inject
    private Validator validator; // jakarta.validation.Validator (inyectado vía CDI)
    
    /**
     * Valida una entidad programáticamente y lanza excepción si hay errores.
     */
    public <T> void validar(T entidad, Class<?>... grupos) {
        Set<ConstraintViolation<T>> violaciones = validator.validate(entidad, grupos);
        
        if (!violaciones.isEmpty()) {
            StringBuilder sb = new StringBuilder("Errores de validación:\n");
            for (ConstraintViolation<T> v : violaciones) {
                sb.append("- ").append(v.getPropertyPath())
                  .append(": ").append(v.getMessage()).append("\n");
            }
            throw new NegocioException(sb.toString());
        }
    }
}
```

---


## 5. NIVEL 4: Validación en Base de Datos (Integridad Física)

### 5.1 Definición

La validación a nivel de base de datos es la **última barrera** de protección de la integridad de los datos. Se implementa mediante restricciones (*constraints*) SQL que el motor de base de datos verifica en cada operación DML (INSERT, UPDATE, DELETE).

**Principio:** Aunque las capas superiores validen, la base de datos SIEMPRE debe protegerse a sí misma. Esto cubre escenarios como:
- Acceso directo a la BD (scripts, herramientas DBA)
- Bugs en la aplicación que saltean validaciones
- Múltiples aplicaciones compartiendo la misma BD
- Condiciones de carrera (race conditions)

### 5.2 Tipos de Constraints en Base de Datos

| Constraint | Propósito | Ejemplo |
|-----------|-----------|---------|
| `NOT NULL` | Campo obligatorio | `nombre VARCHAR(100) NOT NULL` |
| `UNIQUE` | Valor sin duplicados | `UNIQUE(email)` |
| `PRIMARY KEY` | Identificador único | `PRIMARY KEY(id)` |
| `FOREIGN KEY` | Integridad referencial | `REFERENCES carreras(id)` |
| `CHECK` | Condición booleana | `CHECK(edad >= 16)` |
| `DEFAULT` | Valor por defecto | `DEFAULT CURRENT_TIMESTAMP` |

### 5.3 Mapeo JPA ↔ Constraints de BD

```java
@Entity
@Table(name = "estudiantes", 
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_email", columnNames = {"email"}),
           @UniqueConstraint(name = "uk_dni", columnNames = {"dni"})
       })
public class Estudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // NOT NULL constraint
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    // UNIQUE + NOT NULL
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    // UNIQUE
    @Column(name = "dni", unique = true, length = 8)
    private String dni;

    // CHECK constraint (Jakarta EE 10 / JPA 3.1)
    @Column(name = "edad")
    // El CHECK se define en la tabla o con scripts DDL
    private Integer edad;

    // FOREIGN KEY (relación)
    @ManyToOne(optional = false) // Genera NOT NULL en FK
    @JoinColumn(name = "carrera_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_estudiante_carrera"))
    private Carrera carrera;

    // Precisión numérica
    @Column(name = "promedio", precision = 4, scale = 2)
    private BigDecimal promedio;
    
    // Timestamp automático
    @Column(name = "fecha_creacion", updatable = false)
    @CreationTimestamp
    private LocalDateTime fechaCreacion;
}
```

### 5.4 Definición Explícita de Constraints con DDL

Cuando JPA no puede generar automáticamente un constraint, lo definimos en un script de migración:

```sql
-- Script DDL complementario (ej: V1__crear_tablas.sql para Flyway)

CREATE TABLE estudiantes (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    dni VARCHAR(8) NOT NULL,
    edad INTEGER,
    promedio NUMERIC(4,2),
    legajo INTEGER,
    carrera_id BIGINT NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    activo BOOLEAN DEFAULT TRUE,
    
    -- Constraints
    CONSTRAINT uk_estudiante_email UNIQUE (email),
    CONSTRAINT uk_estudiante_dni UNIQUE (dni),
    CONSTRAINT uk_estudiante_legajo UNIQUE (legajo),
    CONSTRAINT fk_estudiante_carrera 
        FOREIGN KEY (carrera_id) REFERENCES carreras(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_estudiante_edad 
        CHECK (edad >= 16 AND edad <= 99),
    CONSTRAINT ck_estudiante_promedio 
        CHECK (promedio >= 0.0 AND promedio <= 10.0),
    CONSTRAINT ck_estudiante_legajo_positivo 
        CHECK (legajo > 0)
);

-- Índices para performance en consultas frecuentes
CREATE INDEX idx_estudiante_apellido ON estudiantes(apellido);
CREATE INDEX idx_estudiante_carrera ON estudiantes(carrera_id);
```


### 5.5 Manejo de Excepciones de BD en JPA

Cuando una constraint de BD es violada, JPA lanza excepciones que debemos capturar:

```java
@ApplicationScoped
public class EstudianteService {
    
    @Inject
    private EntityManager em;
    
    @Transactional
    public Estudiante crear(Estudiante estudiante) {
        try {
            em.persist(estudiante);
            em.flush(); // Forzar la escritura para capturar errores de BD
            return estudiante;
            
        } catch (PersistenceException e) {
            if (esViolacionDeUnicidad(e)) {
                throw new NegocioException(
                    "Ya existe un registro con ese email o DNI");
            }
            if (esViolacionDeFK(e)) {
                throw new NegocioException(
                    "La carrera especificada no existe");
            }
            if (esViolacionDeCheck(e)) {
                throw new NegocioException(
                    "Los datos no cumplen las restricciones de la base de datos");
            }
            throw new NegocioException("Error al guardar: " + e.getMessage());
        }
    }
    
    private boolean esViolacionDeUnicidad(PersistenceException e) {
        return e.getCause() instanceof ConstraintViolationException ||
               (e.getMessage() != null && 
                e.getMessage().toLowerCase().contains("unique"));
    }
    
    private boolean esViolacionDeFK(PersistenceException e) {
        return e.getMessage() != null && 
               e.getMessage().toLowerCase().contains("foreign key");
    }
    
    private boolean esViolacionDeCheck(PersistenceException e) {
        return e.getMessage() != null && 
               e.getMessage().toLowerCase().contains("check");
    }
}
```

### 5.6 JPA Callbacks para Validación Pre-Persistencia

JPA ofrece callbacks que se ejecutan en momentos específicos del ciclo de vida:

```java
@Entity
@EntityListeners(AuditoriaListener.class)
public class Estudiante {
    
    // ...
    
    /**
     * Se ejecuta ANTES de persistir (INSERT).
     * Última oportunidad para validar antes de la BD.
     */
    @PrePersist
    public void prePersist() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
        if (this.activo == null) {
            this.activo = true;
        }
        // Normalización de datos
        if (this.email != null) {
            this.email = this.email.toLowerCase().trim();
        }
        if (this.nombre != null) {
            this.nombre = capitalizar(this.nombre.trim());
        }
    }
    
    /**
     * Se ejecuta ANTES de actualizar (UPDATE).
     */
    @PreUpdate
    public void preUpdate() {
        this.fechaModificacion = LocalDateTime.now();
    }
    
    private String capitalizar(String texto) {
        if (texto == null || texto.isBlank()) return texto;
        return texto.substring(0, 1).toUpperCase() + texto.substring(1).toLowerCase();
    }
}
```

### 5.7 Relación Bean Validation ↔ JPA

**Importante:** JPA integra automáticamente Bean Validation. Antes de ejecutar `persist()` o `merge()`, el `EntityManager` valida las anotaciones de Bean Validation:

```
persist() → Bean Validation → SQL INSERT → DB Constraints
```

Si Bean Validation falla, se lanza `ConstraintViolationException` ANTES de intentar el SQL.

---


## 6. NIVEL 5: Validación de Seguridad (Acceso y Permisos)

### 6.1 Definición

La validación de seguridad controla **quién puede hacer qué** dentro de la aplicación. Se divide en:

- **Autenticación:** Verificar la identidad del usuario (¿quién eres?)
- **Autorización:** Verificar permisos (¿qué puedes hacer?)
- **Protección de datos:** Sanitización de entradas, prevención de ataques

### 6.2 Jakarta Security 3.0

Jakarta EE 10 incluye Jakarta Security 3.0, que unifica la gestión de seguridad:

```java
package ar.edu.universidad.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.authentication.mechanism.http.CustomFormAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.LoginToContinue;

/**
 * Configuración del mecanismo de autenticación por formulario personalizado.
 */
@CustomFormAuthenticationMechanismDefinition(
    loginToContinue = @LoginToContinue(
        loginPage = "/login.xhtml",
        errorPage = "/login-error.xhtml",
        useForwardToLogin = false
    )
)
@ApplicationScoped
public class SecurityConfig {
    // La presencia de esta clase activa la configuración
}
```

### 6.3 Identity Store (Almacén de Identidades)

```java
package ar.edu.universidad.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;
import java.util.Set;

@ApplicationScoped
public class UniversidadIdentityStore implements IdentityStore {

    @Inject
    private UsuarioService usuarioService;

    @Override
    public CredentialValidationResult validate(Credential credential) {
        if (credential instanceof UsernamePasswordCredential upc) {
            String username = upc.getCaller();
            String password = upc.getPasswordAsString();
            
            Usuario usuario = usuarioService.autenticar(username, password);
            
            if (usuario != null) {
                Set<String> roles = usuario.getRoles().stream()
                    .map(Rol::getNombre)
                    .collect(Collectors.toSet());
                    
                return new CredentialValidationResult(username, roles);
            }
        }
        return CredentialValidationResult.INVALID_RESULT;
    }
}
```

### 6.4 Autorización Declarativa con Anotaciones

```java
package ar.edu.universidad.services;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.DenyAll;
import jakarta.ejb.Stateless;

@Stateless
@DeclareRoles({"ADMIN", "DOCENTE", "ESTUDIANTE", "BEDEL"})
public class GestionAcademicaService {

    /**
     * Solo administradores pueden crear carreras.
     */
    @RolesAllowed("ADMIN")
    public Carrera crearCarrera(Carrera carrera) {
        // Solo accesible por ADMIN
        return carreraRepo.save(carrera);
    }
    
    /**
     * Docentes y Admin pueden cargar notas.
     */
    @RolesAllowed({"ADMIN", "DOCENTE"})
    public void cargarNota(Long estudianteId, Long materiaId, BigDecimal nota) {
        // Validación adicional: el docente solo puede cargar notas
        // de SUS materias (validación de negocio + seguridad)
        // ...
    }
    
    /**
     * Cualquier usuario autenticado puede consultar el plan de estudios.
     */
    @PermitAll
    public List<Materia> consultarPlanEstudios(Long carreraId) {
        return materiaRepo.findByCarrera(carreraId);
    }
    
    /**
     * Funcionalidad deshabilitada temporalmente.
     */
    @DenyAll
    public void eliminarHistorico() {
        // No accesible por nadie
    }
    
    /**
     * Solo el estudiante puede ver sus propias notas,
     * o un docente/admin.
     */
    @RolesAllowed({"ADMIN", "DOCENTE", "ESTUDIANTE"})
    public List<Nota> consultarNotas(Long estudianteId) {
        // Validación adicional: si es ESTUDIANTE, 
        // verificar que consulta sus propias notas
        return notaRepo.findByEstudiante(estudianteId);
    }
}
```


### 6.5 Seguridad en la Vista JSF

```xml
<!-- Renderizado condicional según rol -->
<h:panelGroup rendered="#{facesContext.externalContext.isUserInRole('ADMIN')}">
    <h:commandButton value="Eliminar Estudiante" 
                     action="#{adminBean.eliminar}" 
                     styleClass="btn-danger" />
</h:panelGroup>

<h:panelGroup rendered="#{facesContext.externalContext.isUserInRole('DOCENTE')}">
    <h:commandButton value="Cargar Notas" 
                     action="#{docenteBean.cargarNotas}" />
</h:panelGroup>

<!-- Alternativa usando SecurityContext de Jakarta Security -->
<h:panelGroup rendered="#{securityBean.esAdmin}">
    <!-- Contenido solo para admin -->
</h:panelGroup>
```

```java
@Named
@RequestScoped
public class SecurityBean {
    
    @Inject
    private SecurityContext securityContext;
    
    public boolean isEsAdmin() {
        return securityContext.isCallerInRole("ADMIN");
    }
    
    public boolean isEsDocente() {
        return securityContext.isCallerInRole("DOCENTE");
    }
    
    public String getUsuarioActual() {
        return securityContext.getCallerPrincipal().getName();
    }
}
```

### 6.6 Protección contra Ataques Comunes

#### 6.6.1 Cross-Site Scripting (XSS)

JSF protege por defecto contra XSS porque **escapa automáticamente** la salida:

```xml
<!-- SEGURO: JSF escapa HTML automáticamente -->
<h:outputText value="#{bean.nombre}" />

<!-- PELIGROSO: escape="false" permite HTML sin escapar (evitar si es posible) -->
<h:outputText value="#{bean.contenidoHtml}" escape="false" />
```

#### 6.6.2 Cross-Site Request Forgery (CSRF)

JSF incluye protección CSRF nativa mediante el `ViewState`:

```xml
<!-- El ViewState actúa como token CSRF -->
<h:form>
    <!-- Jakarta Faces genera automáticamente un campo oculto 
         jakarta.faces.ViewState que cambia por sesión/vista -->
    <h:commandButton value="Acción Protegida" action="#{bean.accion}" />
</h:form>
```

Para protección adicional:

```xml
<!-- En web.xml -->
<context-param>
    <param-name>jakarta.faces.ENABLE_WEBSOCKET_ENDPOINT</param-name>
    <param-value>true</param-value>
</context-param>
```

#### 6.6.3 SQL Injection

JPA protege contra SQL Injection al usar parámetros con nombre:

```java
// SEGURO: Parámetros con nombre (parametrized queries)
@NamedQuery(name = "Estudiante.findByDni",
    query = "SELECT e FROM Estudiante e WHERE e.dni = :dni")

// En el repository:
public Optional<Estudiante> findByDni(String dni) {
    return em.createNamedQuery("Estudiante.findByDni", Estudiante.class)
             .setParameter("dni", dni)
             .getResultStream()
             .findFirst();
}

// PELIGROSO: Concatenación de strings (NUNCA hacer esto)
// String query = "SELECT e FROM Estudiante e WHERE e.dni = '" + dni + "'";
```

#### 6.6.4 Configuración de Seguridad en web.xml

```xml
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee" version="6.0">
    
    <!-- Restricción de seguridad para área académica -->
    <security-constraint>
        <web-resource-collection>
            <web-resource-name>Area Académica</web-resource-name>
            <url-pattern>/academica/*</url-pattern>
            <http-method>GET</http-method>
            <http-method>POST</http-method>
        </web-resource-collection>
        <auth-constraint>
            <role-name>DOCENTE</role-name>
            <role-name>ADMIN</role-name>
        </auth-constraint>
        <user-data-constraint>
            <transport-guarantee>CONFIDENTIAL</transport-guarantee>
        </user-data-constraint>
    </security-constraint>
    
    <!-- Restricción para área de administración -->
    <security-constraint>
        <web-resource-collection>
            <web-resource-name>Administración</web-resource-name>
            <url-pattern>/admin/*</url-pattern>
        </web-resource-collection>
        <auth-constraint>
            <role-name>ADMIN</role-name>
        </auth-constraint>
    </security-constraint>
    
    <!-- Definición de roles -->
    <security-role>
        <role-name>ADMIN</role-name>
    </security-role>
    <security-role>
        <role-name>DOCENTE</role-name>
    </security-role>
    <security-role>
        <role-name>ESTUDIANTE</role-name>
    </security-role>
    <security-role>
        <role-name>BEDEL</role-name>
    </security-role>
    
</web-app>
```

---


## 7. Integración de Todos los Niveles: Ejemplo Completo

### 7.1 Caso de Uso: Registro de Estudiante

Veamos cómo los 5 niveles trabajan juntos en un flujo completo de registro:

```
USUARIO → [Nivel 5: ¿Tiene permiso?] 
       → [Nivel 1: ¿Datos bien formados?] 
       → [Nivel 2: ¿Entidad válida?]
       → [Nivel 3: ¿Reglas de negocio OK?] 
       → [Nivel 4: ¿BD acepta?] 
       → ÉXITO
```

### 7.2 La Entidad Completa

```java
package ar.edu.universidad.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import ar.edu.universidad.validation.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "estudiantes",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_email", columnNames = "email"),
           @UniqueConstraint(name = "uk_dni", columnNames = "dni"),
           @UniqueConstraint(name = "uk_legajo", columnNames = "legajo")
       })
@FechasCoherentes // Validación a nivel de clase (Nivel 2)
public class Estudiante implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "Nombre: entre {min} y {max} caracteres")
    @Column(nullable = false, length = 100)
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 100)
    @Column(nullable = false, length = 100)
    private String apellido;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email inválido")
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "\\d{7,8}", message = "DNI: 7 u 8 dígitos numéricos")
    @Column(unique = true, length = 8)
    private String dni;

    @NotNull(message = "La edad es obligatoria")
    @Min(value = 16, message = "Edad mínima: {value}")
    @Max(value = 99, message = "Edad máxima: {value}")
    @Column
    private Integer edad;

    @Past(message = "La fecha de nacimiento debe ser pasada")
    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @ManyToOne(optional = false)
    @JoinColumn(name = "carrera_id", nullable = false)
    @NotNull(message = "Debe seleccionar una carrera")
    private Carrera carrera;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "activo")
    private Boolean activo;

    @PrePersist
    public void prePersist() {
        this.fechaCreacion = LocalDateTime.now();
        this.activo = true;
        normalizar();
    }

    @PreUpdate
    public void preUpdate() {
        normalizar();
    }

    private void normalizar() {
        if (email != null) this.email = email.toLowerCase().trim();
        if (nombre != null) this.nombre = nombre.trim();
        if (apellido != null) this.apellido = apellido.trim();
    }

    // Constructores, getters, setters, equals, hashCode, toString...
}
```


### 7.3 La Vista JSF Completa

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:h="jakarta.faces.html"
      xmlns:f="jakarta.faces.core"
      xmlns:ui="jakarta.faces.facelets">

<h:head>
    <title>Registro de Estudiante</title>
    <h:outputStylesheet name="css/estilos.css" />
</h:head>

<h:body>
    <h:form id="registroForm">
        <h1>Registro de Estudiante</h1>
        
        <!-- Mensajes globales -->
        <h:messages globalOnly="true" styleClass="mensajes-globales" />
        
        <!-- NIVEL 1: Validación en Vista -->
        <div class="campo">
            <h:outputLabel for="nombre" value="Nombre: *" />
            <h:inputText id="nombre" 
                         value="#{registroEstudianteBean.estudiante.nombre}"
                         required="true"
                         requiredMessage="Ingrese el nombre">
                <f:validateLength minimum="2" maximum="100" />
            </h:inputText>
            <h:message for="nombre" styleClass="error" />
        </div>
        
        <div class="campo">
            <h:outputLabel for="apellido" value="Apellido: *" />
            <h:inputText id="apellido" 
                         value="#{registroEstudianteBean.estudiante.apellido}"
                         required="true">
                <f:validateLength minimum="2" maximum="100" />
            </h:inputText>
            <h:message for="apellido" styleClass="error" />
        </div>
        
        <div class="campo">
            <h:outputLabel for="email" value="Email: *" />
            <h:inputText id="email" 
                         value="#{registroEstudianteBean.estudiante.email}"
                         required="true">
                <!-- Bean Validation @Email se aplica automáticamente -->
            </h:inputText>
            <h:message for="email" styleClass="error" />
        </div>
        
        <div class="campo">
            <h:outputLabel for="dni" value="DNI: *" />
            <h:inputText id="dni" 
                         value="#{registroEstudianteBean.estudiante.dni}"
                         required="true"
                         validatorMessage="DNI: solo números, 7 u 8 dígitos">
                <f:validateRegex pattern="\d{7,8}" />
            </h:inputText>
            <h:message for="dni" styleClass="error" />
        </div>
        
        <div class="campo">
            <h:outputLabel for="edad" value="Edad: *" />
            <h:inputText id="edad" 
                         value="#{registroEstudianteBean.estudiante.edad}"
                         required="true">
                <!-- Bean Validation @Min/@Max se aplican automáticamente -->
            </h:inputText>
            <h:message for="edad" styleClass="error" />
        </div>
        
        <div class="campo">
            <h:outputLabel for="fechaNac" value="Fecha Nacimiento: *" />
            <h:inputText id="fechaNac" 
                         value="#{registroEstudianteBean.estudiante.fechaNacimiento}"
                         required="true">
                <f:convertDateTime pattern="dd/MM/yyyy" type="localDate" />
            </h:inputText>
            <h:message for="fechaNac" styleClass="error" />
        </div>
        
        <div class="campo">
            <h:outputLabel for="carrera" value="Carrera: *" />
            <h:selectOneMenu id="carrera" 
                             value="#{registroEstudianteBean.estudiante.carrera}"
                             required="true"
                             requiredMessage="Seleccione una carrera">
                <f:selectItem itemLabel="-- Seleccione --" noSelectionOption="true" />
                <f:selectItems value="#{registroEstudianteBean.carreras}" 
                               var="c" 
                               itemValue="#{c}" 
                               itemLabel="#{c.nombre}" />
                <f:converter converterId="carreraConverter" />
            </h:selectOneMenu>
            <h:message for="carrera" styleClass="error" />
        </div>
        
        <!-- Botón de envío - NIVEL 5: Solo ADMIN y BEDEL pueden registrar -->
        <h:panelGroup rendered="#{facesContext.externalContext.isUserInRole('ADMIN') or 
                                  facesContext.externalContext.isUserInRole('BEDEL')}">
            <h:commandButton value="Registrar Estudiante" 
                             action="#{registroEstudianteBean.registrar}"
                             styleClass="btn-primary" />
        </h:panelGroup>
        
    </h:form>
</h:body>
</html>
```


### 7.4 El Bean Controlador

```java
package ar.edu.universidad.beans;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import ar.edu.universidad.entities.*;
import ar.edu.universidad.exceptions.NegocioException;
import ar.edu.universidad.services.EstudianteService;
import ar.edu.universidad.services.CarreraService;
import java.util.List;

@Named
@RequestScoped
public class RegistroEstudianteBean {

    @Inject
    private EstudianteService estudianteService;
    
    @Inject
    private CarreraService carreraService;
    
    @Inject
    private FacesContext facesContext;

    private Estudiante estudiante;
    private List<Carrera> carreras;

    @PostConstruct
    public void init() {
        estudiante = new Estudiante();
        carreras = carreraService.listarActivas();
    }

    /**
     * Método de acción: se ejecuta DESPUÉS de Nivel 1 y Nivel 2.
     * Aquí capturamos errores del Nivel 3 (reglas de negocio) 
     * y Nivel 4 (base de datos).
     */
    public String registrar() {
        try {
            // NIVEL 3 y 4: El servicio aplica reglas de negocio y persiste
            estudianteService.registrar(estudiante);
            
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Éxito", "Estudiante registrado correctamente"));
            
            return "confirmacion?faces-redirect=true";
            
        } catch (NegocioException e) {
            facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error de registro", e.getMessage()));
            return null; // Permanece en la página
        }
    }

    // Getters y Setters
    public Estudiante getEstudiante() { return estudiante; }
    public void setEstudiante(Estudiante estudiante) { this.estudiante = estudiante; }
    public List<Carrera> getCarreras() { return carreras; }
}
```

### 7.5 El Servicio con Reglas de Negocio

```java
package ar.edu.universidad.services;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Validator;
import ar.edu.universidad.entities.Estudiante;
import ar.edu.universidad.exceptions.NegocioException;
import ar.edu.universidad.repositories.EstudianteRepository;

@ApplicationScoped
public class EstudianteService {

    @Inject
    private EstudianteRepository estudianteRepo;
    
    @Inject
    private Validator validator;

    /**
     * NIVEL 5: Solo ADMIN y BEDEL pueden registrar estudiantes.
     * NIVEL 3: Aplica reglas de negocio.
     * NIVEL 4: Persiste (constraints de BD se verifican).
     */
    @RolesAllowed({"ADMIN", "BEDEL"})
    @Transactional
    public Estudiante registrar(Estudiante estudiante) {
        
        // NIVEL 3: Regla - No puede existir otro estudiante con mismo DNI
        if (estudianteRepo.existsByDni(estudiante.getDni())) {
            throw new NegocioException("dni",
                "Ya existe un estudiante con DNI " + estudiante.getDni());
        }
        
        // NIVEL 3: Regla - No puede existir otro estudiante con mismo email
        if (estudianteRepo.existsByEmail(estudiante.getEmail())) {
            throw new NegocioException("email",
                "Ya existe un estudiante con email " + estudiante.getEmail());
        }
        
        // NIVEL 3: Regla - La edad debe ser coherente con la fecha de nacimiento
        if (estudiante.getFechaNacimiento() != null && estudiante.getEdad() != null) {
            int edadCalculada = Period.between(
                estudiante.getFechaNacimiento(), LocalDate.now()).getYears();
            if (Math.abs(edadCalculada - estudiante.getEdad()) > 1) {
                throw new NegocioException(
                    "La edad no es coherente con la fecha de nacimiento");
            }
        }
        
        // NIVEL 3: Regla - Verificar período de inscripción abierto
        if (!periodoInscripcionAbierto()) {
            throw new NegocioException(
                "El período de inscripción no está abierto actualmente");
        }
        
        // NIVEL 4: persist → Bean Validation → SQL INSERT → DB Constraints
        return estudianteRepo.save(estudiante);
    }
    
    private boolean periodoInscripcionAbierto() {
        LocalDate hoy = LocalDate.now();
        // Períodos: Feb-Mar y Jul-Ago
        int mes = hoy.getMonthValue();
        return (mes >= 2 && mes <= 3) || (mes >= 7 && mes <= 8);
    }
}
```

---


## 8. Flujo Completo de Validación: Diagrama de Secuencia

```
USUARIO ingresa datos en formulario JSF
         │
         ▼
┌────────────────────────────────────────────────────────────────┐
│ NIVEL 5: SEGURIDAD                                             │
│ ¿El usuario está autenticado? ¿Tiene rol ADMIN o BEDEL?       │
│ → Si NO: Redirigir a login.xhtml / mostrar error 403          │
│ → Si SÍ: Continuar                                            │
└────────────────────────────┬───────────────────────────────────┘
                             ▼
┌────────────────────────────────────────────────────────────────┐
│ FASE 2 JSF: Apply Request Values                               │
│ - Conversión de tipos (String → Integer, String → LocalDate)   │
│ → Si falla conversión: mensaje de error, saltar a Fase 6       │
└────────────────────────────┬───────────────────────────────────┘
                             ▼
┌────────────────────────────────────────────────────────────────┐
│ NIVEL 1: VALIDACIÓN EN VISTA (Fase 3 JSF)                      │
│ - required="true" → ¿campo vacío?                              │
│ - f:validateLength → ¿longitud correcta?                       │
│ - f:validateRegex → ¿formato correcto?                         │
│ - Bean Validation automática (@NotBlank, @Email, @Min, etc.)   │
│ → Si falla: mensaje de error, saltar a Fase 6 (Render)        │
└────────────────────────────┬───────────────────────────────────┘
                             ▼
┌────────────────────────────────────────────────────────────────┐
│ NIVEL 2: BEAN VALIDATION (integrada en Fase 3)                 │
│ - @NotNull, @Size, @Pattern, @Past, etc.                       │
│ - Validadores personalizados (@LegajoUniversitario)            │
│ - Validación a nivel de clase (@FechasCoherentes)              │
│ → Si falla: mensaje de error, saltar a Fase 6                 │
└────────────────────────────┬───────────────────────────────────┘
                             ▼
┌────────────────────────────────────────────────────────────────┐
│ FASE 4 JSF: Update Model Values                                │
│ - Los valores validados se setean en el bean/entidad           │
└────────────────────────────┬───────────────────────────────────┘
                             ▼
┌────────────────────────────────────────────────────────────────┐
│ FASE 5 JSF: Invoke Application                                 │
│ → Se ejecuta el método de acción: registrar()                  │
└────────────────────────────┬───────────────────────────────────┘
                             ▼
┌────────────────────────────────────────────────────────────────┐
│ NIVEL 3: VALIDACIÓN EN SERVICIO (Reglas de Negocio)            │
│ - ¿DNI ya existe? (consulta a BD)                              │
│ - ¿Email ya existe?                                            │
│ - ¿Edad coherente con fecha nacimiento?                        │
│ - ¿Período de inscripción abierto?                             │
│ → Si falla: NegocioException → mensaje JSF, return null        │
└────────────────────────────┬───────────────────────────────────┘
                             ▼
┌────────────────────────────────────────────────────────────────┐
│ NIVEL 4: PERSISTENCIA (JPA + BD)                               │
│ - @PrePersist: normalización de datos                          │
│ - Bean Validation pre-persist (segunda verificación)           │
│ - SQL INSERT ejecutado                                         │
│ - DB Constraints: UNIQUE, NOT NULL, CHECK, FK                  │
│ → Si falla: PersistenceException → NegocioException → mensaje  │
└────────────────────────────┬───────────────────────────────────┘
                             ▼
┌────────────────────────────────────────────────────────────────┐
│ ÉXITO: Redirect a confirmacion.xhtml                           │
└────────────────────────────────────────────────────────────────┘
```

---


## 9. Buenas Prácticas y Patrones

### 9.1 Principio DRY (Don't Repeat Yourself)

| Malo ❌ | Bueno ✅ |
|---------|----------|
| Repetir `min=2, max=100` en JSF y en la entidad | Definir en Bean Validation y dejar que JSF lo use automáticamente |
| Validar email con regex en JSF Y en el servicio | Usar `@Email` una vez en la entidad |
| Verificar NOT NULL en todas las capas manualmente | `@NotNull` + `nullable=false` + `required=true` |

### 9.2 Cuándo Validar en Cada Nivel

| Tipo de validación | Nivel recomendado |
|-------------------|-------------------|
| Campo obligatorio | Nivel 1 (required) + Nivel 2 (@NotNull) + Nivel 4 (NOT NULL) |
| Formato de dato | Nivel 2 (@Pattern, @Email) |
| Rango de valores | Nivel 2 (@Min, @Max) + Nivel 4 (CHECK) |
| Unicidad | Nivel 3 (consulta previa) + Nivel 4 (UNIQUE) |
| Integridad referencial | Nivel 3 (verificar existencia) + Nivel 4 (FK) |
| Reglas de negocio complejas | Nivel 3 exclusivamente |
| Control de acceso | Nivel 5 exclusivamente |

### 9.3 Mensajes de Error: Buenas Prácticas

```java
// ❌ MALO: Mensaje genérico
@NotNull(message = "Error")

// ❌ MALO: Mensaje técnico
@NotNull(message = "javax.validation.constraints.NotNull.message")

// ✅ BUENO: Mensaje claro para el usuario
@NotNull(message = "El nombre es obligatorio")

// ✅ MEJOR: Mensaje con interpolación
@Size(min = 2, max = 100, message = "El nombre debe tener entre {min} y {max} caracteres")

// ✅ EXCELENTE: Mensajes externalizados (i18n)
@NotNull(message = "{estudiante.nombre.requerido}")
// Con archivo ValidationMessages.properties:
// estudiante.nombre.requerido=El nombre del estudiante es obligatorio
```

### 9.4 Orden de Prioridad en los Mensajes JSF

JSF prioriza los mensajes según este orden:
1. `requiredMessage` del componente (si `required="true"` falla)
2. `validatorMessage` del componente (si un validador falla)
3. `converterMessage` del componente (si la conversión falla)
4. Mensaje propio del validador/conversor
5. Mensaje de Bean Validation

### 9.5 Anti-Patrones a Evitar

| Anti-Patrón | Problema | Solución |
|-------------|----------|----------|
| Validar solo en JavaScript (cliente) | Se puede saltear fácilmente | Siempre validar en servidor |
| No validar en BD | Datos corruptos ante bugs | Siempre tener constraints en DDL |
| Confiar solo en Bean Validation | No cubre reglas complejas | Complementar con Nivel 3 |
| Lanzar excepciones genéricas | El usuario no entiende el error | Excepciones específicas con mensajes claros |
| Validar lo mismo en todos los niveles | Código repetido, difícil de mantener | Cada nivel valida lo que le corresponde |

---


## 10. Configuración del Proyecto

### 10.1 Dependencias (pom.xml para Maven)

```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>ar.edu.universidad</groupId>
    <artifactId>sistema-academico</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>war</packaging>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <jakarta.ee.version>10.0.0</jakarta.ee.version>
    </properties>

    <dependencies>
        <!-- Jakarta EE 10 API (incluye JSF, JPA, CDI, Bean Validation, Security) -->
        <dependency>
            <groupId>jakarta.platform</groupId>
            <artifactId>jakarta.jakartaee-api</artifactId>
            <version>${jakarta.ee.version}</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <finalName>sistema-academico</finalName>
    </build>
</project>
```

**Nota:** Con `scope=provided`, el servidor de aplicaciones (WildFly 27+, Payara 6, GlassFish 7) provee todas las implementaciones.

### 10.2 Estructura del Proyecto

```
sistema-academico/
├── src/
│   └── main/
│       ├── java/
│       │   └── ar/edu/universidad/
│       │       ├── entities/           # Entidades JPA + Bean Validation
│       │       │   ├── Estudiante.java
│       │       │   ├── Materia.java
│       │       │   ├── Carrera.java
│       │       │   └── Inscripcion.java
│       │       ├── repositories/       # Acceso a datos (JPA)
│       │       │   ├── EstudianteRepository.java
│       │       │   └── MateriaRepository.java
│       │       ├── services/           # Lógica de negocio (Nivel 3)
│       │       │   ├── EstudianteService.java
│       │       │   └── InscripcionService.java
│       │       ├── beans/              # Managed Beans JSF (Controladores)
│       │       │   ├── RegistroEstudianteBean.java
│       │       │   └── InscripcionBean.java
│       │       ├── validators/         # Validadores JSF personalizados (Nivel 1)
│       │       │   └── CuitValidator.java
│       │       ├── validation/         # Bean Validation personalizada (Nivel 2)
│       │       │   ├── LegajoUniversitario.java
│       │       │   ├── LegajoUniversitarioValidator.java
│       │       │   ├── FechasCoherentes.java
│       │       │   └── FechasCoherentesValidator.java
│       │       ├── security/           # Seguridad (Nivel 5)
│       │       │   ├── SecurityConfig.java
│       │       │   └── UniversidadIdentityStore.java
│       │       ├── exceptions/         # Excepciones de negocio
│       │       │   ├── NegocioException.java
│       │       │   └── CorrelativasException.java
│       │       └── converters/         # Conversores JSF
│       │           ├── LocalDateConverter.java
│       │           └── CarreraConverter.java
│       ├── resources/
│       │   ├── META-INF/
│       │   │   └── persistence.xml     # Configuración JPA
│       │   └── ValidationMessages.properties  # Mensajes i18n
│       └── webapp/
│           ├── WEB-INF/
│           │   ├── web.xml
│           │   ├── faces-config.xml
│           │   └── beans.xml           # Activar CDI
│           ├── resources/
│           │   └── css/estilos.css
│           ├── registro.xhtml
│           ├── inscripcion.xhtml
│           ├── login.xhtml
│           └── confirmacion.xhtml
└── pom.xml
```

### 10.3 persistence.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<persistence xmlns="https://jakarta.ee/xml/ns/persistence"
             version="3.0">
    <persistence-unit name="universidadPU" transaction-type="JTA">
        <jta-data-source>java:jboss/datasources/UniversidadDS</jta-data-source>
        <properties>
            <!-- Bean Validation integrada con JPA -->
            <property name="jakarta.persistence.validation.mode" value="AUTO" />
            <!-- AUTO = valida en pre-persist y pre-update -->
            
            <property name="jakarta.persistence.schema-generation.database.action" 
                      value="drop-and-create" />
            <!-- En producción usar: none (usar Flyway/Liquibase) -->
        </properties>
    </persistence-unit>
</persistence>
```

**Modos de validación JPA:**
- `AUTO`: Valida en persist y update (por defecto)
- `CALLBACK`: Igual que AUTO
- `NONE`: Deshabilita Bean Validation en JPA

---


## 11. Resumen Comparativo Final

### 11.1 ¿Qué valida cada nivel?

| Pregunta que responde | Nivel |
|-----------------------|-------|
| ¿El campo tiene dato? | 1, 2, 4 |
| ¿El formato es correcto? | 1, 2 |
| ¿El valor está en rango? | 1, 2, 4 |
| ¿Es único en el sistema? | 3, 4 |
| ¿Las relaciones existen? | 3, 4 |
| ¿Cumple reglas del dominio? | 3 |
| ¿El usuario tiene permiso? | 5 |
| ¿Los datos son seguros? | 1, 5 |

### 11.2 ¿Qué pasa si falla cada nivel?

| Nivel | Qué ocurre al fallar | UX (experiencia del usuario) |
|-------|----------------------|------------------------------|
| 1 (Vista) | No se ejecuta la acción, se muestran mensajes junto a los campos | Feedback inmediato, sin reload |
| 2 (Bean Validation) | Similar a Nivel 1 por la integración JSF+BV | Mensajes específicos por campo |
| 3 (Servicio) | Se captura excepción, se muestra mensaje global | Mensaje en la parte superior |
| 4 (Base de Datos) | PersistenceException → mensaje genérico | Menos amigable, último recurso |
| 5 (Seguridad) | Redirect a login o error 403 | Bloqueo total de acceso |

### 11.3 Tecnologías por Nivel en Jakarta EE 10

| Nivel | Especificación | Paquete/Namespace |
|-------|---------------|-------------------|
| 1 | Jakarta Faces 4.0 | `jakarta.faces.validator`, `jakarta.faces.core` |
| 2 | Jakarta Validation 3.0 | `jakarta.validation.constraints` |
| 3 | CDI 4.0 + EJB | `jakarta.enterprise.context`, `jakarta.inject` |
| 4 | Jakarta Persistence 3.1 | `jakarta.persistence` |
| 5 | Jakarta Security 3.0 | `jakarta.security.enterprise`, `jakarta.annotation.security` |

---

## 12. Ejercicios Propuestos

### Ejercicio 1: Nivel 1 y 2
Crear un formulario de registro de **Docente** con los campos: nombre, apellido, email institucional (debe terminar en `@universidad.edu.ar`), legajo, fecha de ingreso. Implementar validaciones en vista y Bean Validation.

### Ejercicio 2: Nivel 3
Implementar la regla de negocio: "Un docente no puede dictar más de 4 materias en un mismo cuatrimestre, ni tener superposición de horarios".

### Ejercicio 3: Nivel 4
Diseñar el esquema DDL para la tabla `docentes` incluyendo todas las constraints necesarias (PK, FK, UNIQUE, CHECK, NOT NULL).

### Ejercicio 4: Nivel 5
Implementar un sistema donde:
- ADMIN puede crear/eliminar docentes
- DOCENTE puede ver y modificar sus propios datos
- BEDEL puede consultar datos de cualquier docente pero no modificarlos

### Ejercicio 5: Integración
Implementar el caso de uso completo "Carga de Notas de Parcial" donde:
- Solo el docente de la materia puede cargar notas (Nivel 5)
- La nota debe estar entre 0 y 10 (Nivel 1 y 2)
- El estudiante debe estar inscripto y activo en la materia (Nivel 3)
- No puede haber notas duplicadas para el mismo parcial (Nivel 3 y 4)
- La fecha de carga debe estar dentro del período de exámenes (Nivel 3)

---

## 13. Glosario

| Término | Definición |
|---------|-----------|
| **Constraint** | Restricción o regla que debe cumplirse para que un dato sea válido |
| **Bean Validation** | Especificación Jakarta para validación declarativa mediante anotaciones |
| **Validator (JSF)** | Componente que verifica datos durante el ciclo de vida de JSF |
| **Converter (JSF)** | Componente que transforma datos entre String y tipo Java |
| **CDI** | Contexts and Dependency Injection - inyección de dependencias en Jakarta EE |
| **JPA** | Jakarta Persistence API - mapeo objeto-relacional |
| **CSRF** | Cross-Site Request Forgery - ataque de falsificación de peticiones |
| **XSS** | Cross-Site Scripting - ataque de inyección de scripts |
| **DDL** | Data Definition Language - lenguaje SQL para definir estructura |
| **DML** | Data Manipulation Language - lenguaje SQL para manipular datos |
| **ViewState** | Estado de la vista JSF serializado, también funciona como token CSRF |
| **Interpolación** | Sustitución de valores en mensajes (ej: `{min}`, `{max}`) |
| **Race Condition** | Condición de carrera: dos procesos compiten por un mismo recurso |

---

## 14. Bibliografía y Referencias

- Jakarta EE 10 Specification - https://jakarta.ee/specifications/
- Jakarta Bean Validation 3.0 - https://jakarta.ee/specifications/bean-validation/3.0/
- Jakarta Faces 4.0 - https://jakarta.ee/specifications/faces/4.0/
- Jakarta Persistence 3.1 - https://jakarta.ee/specifications/persistence/3.1/
- Jakarta Security 3.0 - https://jakarta.ee/specifications/security/3.0/
- Hibernate Validator Reference Guide
- "Beginning Jakarta EE Web Development" - Lukas Jungmann et al.

---

*Documento preparado para la cátedra de Desarrollo de Aplicaciones Web*  
*Última actualización: Junio 2026*
