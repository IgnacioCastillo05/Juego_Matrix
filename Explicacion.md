# Juego Matrix - Documentación Técnica

## 📋 Índice
1. [Introducción](#introducción)
2. [Arquitectura del Código](#arquitectura-del-código)
3. [Estrategias de Movimiento](#estrategias-de-movimiento)
4. [Mecanismos de Concurrencia](#mecanismos-de-concurrencia)

---

## Introducción

Este proyecto implementa un juego inspirado en la película **Matrix (1999)** donde Neo debe escapar de los Agentes llegando a un teléfono. El juego utiliza programación concurrente en Java para simular el movimiento simultáneo de múltiples entidades.

### Objetivo del Juego
- **Neo**: Llegar a un teléfono antes de ser capturado
- **Agentes**: Capturar a Neo antes de que escape

---

## Arquitectura del Código

### Diagrama de Clases

```
Comportamiento (Interface)
    ↑
    |
Persona (Abstract) ← implements Comportamiento, Runnable
    ↑                      ↑
    |                      |
    |------ Neo            |------ Agente
    

Objeto (Abstract)
    ↑
    |
    |------ Muro
    |------ Telefono


MatrixGame (Controlador Principal)
```

### Clases Abstractas Base

#### 1. **Comportamiento** (Interface)
```java
public interface Comportamiento {
    boolean movimiento(char[][] tablero);
}
```
- Define el contrato para el comportamiento de movimiento
- Todas las entidades móviles deben implementar este método

#### 2. **Objeto** (Clase Abstracta)
Representa elementos estáticos del tablero:
- **Atributos**: `posX`, `posY`, `simbolo`
- **Métodos**: getters, setters, `colocarEnTablero()`
- **Clases hijas**: `Muro`, `Telefono`

**Responsabilidades**:
- Mantener la posición en el tablero
- Proveer símbolo visual para representación
- Método abstracto para colocarse en el tablero

#### 3. **Persona** (Clase Abstracta)
Representa entidades con movimiento propio:
- **Implementa**: `Comportamiento`, `Runnable`
- **Atributos**: `posX`, `posY`, `simbolo`, `vivo`, `nombre`
- **Clases hijas**: `Neo`, `Agente`

**Responsabilidades**:
- Gestionar estado vital del personaje
- Validar posiciones dentro del tablero
- Ejecutarse como hilo independiente

### Clases Concretas

#### **Muro**
```java
public class Muro extends Objeto
```
- Obstáculo impenetrable
- Símbolo: `'M'`
- Posición fija durante el juego

#### **Telefono**
```java
public class Telefono extends Objeto
```
- Objetivo de Neo
- Símbolo: `'T'`
- **Estado**: `usado` (marcado cuando Neo lo alcanza)
- **Método clave**: `distanciaHasta(x, y)` - calcula con distancia Manhattan

**DISTANCIA MANHATTAN**: La distancia Manhattan es una métrica utilizada para determinar la distancia entre dos puntos de una trayectoria en forma de cuadrícula. A diferencia de la distancia euclidiana, que mide la línea más corta posible entre dos puntos, la distancia de Manhattan mide la suma de las diferencias absolutas entre las coordenadas de los puntos(1).

#### **Neo**
```java
public class Neo extends Persona
```
**Atributos especiales**:
- `telefonos`: Lista de teléfonos disponibles
- `agentes`: Lista de agentes perseguidores
- `muros`: Lista de obstáculos
- `gano`: Estado de victoria
- `proximaX`, `proximaY`: Coordenadas calculadas para siguiente movimiento

**Responsabilidades**:
- Encontrar el teléfono más cercano
- Calcular ruta óptima evitando agentes
- Detectar victoria al llegar al teléfono

#### **Agente**
```java
public class Agente extends Agente extends Persona
```
**Atributos especiales**:
- `neo`: Referencia al objetivo
- `otrosAgentes`: Coordinación entre agentes
- `posicionesReservadas`: Mapa compartido para evitar colisiones
- `id`: Identificador único para coordinación

**Responsabilidades**:
- Perseguir a Neo usando BFS
- Coordinarse con otros agentes
- Detectar captura de Neo

#### **MatrixGame** (Controlador)
Clase principal que orquesta todo el juego.

**Responsabilidades**:
1. **Configuración inicial**:
   - Solicitar posición de Neo al usuario
   - Generar aleatoriamente: teléfonos (1-2), muros (1-10), agentes (2-4)

2. **Gestión de concurrencia**:
   - Crear barreras cíclicas
   - Inicializar hilos de personajes
   - Monitorear estado del juego

3. **Visualización**:
   - Imprimir tablero 10x10
   - Mostrar leyenda y estadísticas
   - Declarar ganador

---

## Estrategias de Movimiento

### Estrategia de Neo: Algoritmo de Dijkstra

Neo utiliza el **algoritmo de Dijkstra** para encontrar el camino más corto al teléfono más cercano, considerando costos variables.

#### Flujo del Algoritmo

```java
private int[] dijkstra(Telefono objetivo) {
    // 1. Inicialización
    int[][] distancias = new int[10][10];  // Todas = INFINITO
    PriorityQueue<Nodo> cola = new PriorityQueue<>();
    
    // 2. Punto de partida
    distancias[posX][posY] = 0;
    cola.offer(new Nodo(posX, posY, 0));
    
    // 3. Exploración
    while (!cola.isEmpty()) {
        Nodo actual = cola.poll();
        
        // Si llegamos al objetivo, reconstruir camino
        if (esObjetivo(actual)) {
            return reconstruirPrimerPaso(...);
        }
        
        // Explorar vecinos (arriba, abajo, izq, der)
        for (vecino : vecinos) {
            int costo = calcularCosto(vecino);
            int nuevaDistancia = distancias[actual] + costo;
            
            if (nuevaDistancia < distancias[vecino]) {
                distancias[vecino] = nuevaDistancia;
                cola.offer(vecino);
            }
        }
    }
}
```

#### Sistema de Costos Inteligente

```java
private int calcularCosto(int x, int y) {
    // Muro = imposible
    if (hayMuro(x, y)) return INFINITO;
    
    int costo = 1;  // Costo base
    
    // Penalización por proximidad a agentes
    for (Agente agente : agentes) {
        int dist = distanciaManhattan(x, y, agente);
        
        if (dist == 0) return INFINITO;     // Capturado
        else if (dist == 1) costo += 50;    // Muy peligroso
        else if (dist == 2) costo += 10;    // Peligroso
    }
    
    return costo;
}
```
Con respecto a los costos, como se pueden dar cuenta en el código documentado, Neo evita pasar por encima de los muros (error que ocurrió durante el desarrollo) haciendo que el peso de los muros sea infinito, es decir, por esa ruta no pasaría (siguiendo la lógica del juego que no puede atravesar los muros).

**Ventajas de esta estrategia**:
- Encuentra el camino óptimo al teléfono
- Evita acercarse demasiado a agentes
- Considera múltiples peligros simultáneamente
- Garantiza encontrar solución si existe

### Estrategia de Agentes: BFS con Coordinación

Los agentes usan **Breadth-First Search (BFS)** con un sistema de coordinación para evitar colisiones y "rodear" a Neo.

#### Flujo del Algoritmo BFS

```java
private int[] bfsConCoordinacion() {
    Queue<int[]> cola = new LinkedList<>();
    boolean[][] visitado = new boolean[10][10];
    int[][] padre = new int[10][10];
    
    // Calcular dirección preferida según posición relativa
    int direccionPreferida = calcularDireccionPreferida(neoX, neoY);
    
    // Ordenar direcciones para explorar primero la preferida
    int[][] direccionesOrdenadas = ordenarDirecciones(dx, dy, direccionPreferida);
    
    cola.offer(new int[]{posX, posY});
    visitado[posX][posY] = true;
    
    while (!cola.isEmpty()) {
        int[] actual = cola.poll();
        
        if (esNeo(actual)) {
            return reconstruirPrimerPaso(...);
        }
        
        // Explorar en orden de preferencia
        for (int[] dir : direccionesOrdenadas) {
            int nx = actual[0] + dir[0];
            int ny = actual[1] + dir[1];
            
            if (esValido(nx, ny) && !visitado[nx][ny]) {
                visitado[nx][ny] = true;
                padre[nx][ny] = codificar(actual);
                cola.offer(new int[]{nx, ny});
            }
        }
    }
}
```

#### Sistema de Coordinación entre Agentes

**1. Dirección Preferida** - Evita que todos vayan por el mismo lado:
```java
private int calcularDireccionPreferida(int neoX, int neoY) {
    // Determinar cuadrante relativo a Neo
    int cuadrante;
    if (posX < neoX && posY < neoY) cuadrante = 0;      // Arriba-Izq
    else if (posX < neoX && posY >= neoY) cuadrante = 1; // Arriba-Der
    else if (posX >= neoX && posY < neoY) cuadrante = 2; // Abajo-Izq
    else cuadrante = 3;                                  // Abajo-Der
    
    // Cada agente tiene preferencia diferente según su ID
    return (cuadrante + id) % 4;
}
```

**2. Reserva de Posiciones** - Evita colisiones:
```java
// Mapa compartido entre todos los agentes
private Map<String, Integer> posicionesReservadas;

private boolean calcularProximoMovimiento() {
    int[] siguientePaso = bfsConCoordinacion();
    
    if (siguientePaso != null) {
        String clave = siguientePaso[0] + "," + siguientePaso[1];
        
        synchronized(posicionesReservadas) {
            // Si otro agente ya reservó esta posición
            if (posicionesReservadas.containsKey(clave)) {
                // Buscar alternativa
                siguientePaso = buscarPosicionAlternativa();
            }
            
            // Reservar la posición
            posicionesReservadas.put(clave, id);
            proximaX = siguientePaso[0];
            proximaY = siguientePaso[1];
        }
        return true;
    }
    return false;
}
```

**3. Posiciones Alternativas**:
```java
private int[] buscarPosicionAlternativa() {
    // Intentar moverse a cualquier casilla adyacente libre
    int[] dx = {-1, 1, 0, 0};
    int[] dy = {0, 0, -1, 1};
    
    for (int i = 0; i < 4; i++) {
        int nx = posX + dx[i];
        int ny = posY + dy[i];
        
        if (esValido(nx, ny) && !estaReservada(nx, ny)) {
            return new int[]{nx, ny};
        }
    }
    return null;  // Quedarse quieto
}
```

**Ventajas de esta estrategia**:
- Simple y eficiente (BFS garantiza camino más corto)
- Los agentes se distribuyen alrededor de Neo
- No colisionan entre ellos
- Pueden "rodear" y bloquear caminos

Sobre las estrategias utilizadas para ambos personajes, se utilizaron estrategias diferentes por distintas razones, entre ellas:
1. Dijsktra vs BFS: Dijkstra es un algoritmo de búsqueda el cual da el camino más corto según los pesos en su recorrido, implementado por ejemplo por los GPS, siendo uno de los algoritmos de búsqueda bastante eficiente; BFS es otro algoritmo el cual recorre o busca elementos en grafos y árboles, mediante una búsqueda por amplitud, y a pesar que es un algoritmo útil, si es un grafo/árbol demasiado grande, todo el proceso de búsqueda se haría más demorado.
2. Sobre el juego: Ya definimos que Dijkstra es más eficiente que BFS a la hora de la búsqueda de un camino (camino más corto vs búsqueda en algo en específico), entonces ahora pasamos al argumento de porqué de la elección para cada personaje.
3. Neo: Para Neo utilizamos dijkstra porque al ser el protagonista, le damos el algoritmo mejor optimizado para que pueda llegar al teléfono y escape usando la mejor estrategia posible, evitando los muros y los agentes.
4. Agentes: Para los agentes utilizamos BFS, un algoritmo un poco menos eficiente, debido a que normalmente durante el juego habrá dos o más agentes, que de paso tienen la capacidad de comunicarse entre ellos para llegar a rodear a Neo y que no pueda llegar al teléfono, entonces a pesar de tener un algoritmo menos eficiente, tiene a su favor la estrategia de la comunicación entre agentes y el poder rodear a Neo.

---

## Mecanismos de Concurrencia

### Sincronización en Dos Fases

El juego utiliza un modelo de **cálculo-aplicación en dos fases** para evitar condiciones de carrera.

```
TURNO = [FASE CÁLCULO] → [FASE APLICACIÓN]
```

#### Fase 1: Cálculo de Movimientos
```java
// Cada hilo calcula su próximo movimiento SIN modificar el tablero
movimientoCalculado = calcularProximoMovimiento();

// BARRERA: Esperar a que TODOS calculen
barreraCalculo.await();
```

#### Fase 2: Aplicación de Movimientos
```java
// Todos aplican su movimiento AL MISMO TIEMPO
synchronized(lockTablero) {
    if (movimientoCalculado) {
        posX = proximaX;
        posY = proximaY;
    }
}

// BARRERA: Esperar a que TODOS apliquen
barreraAplicacion.await();
```

### Herramientas de Concurrencia Utilizadas

#### 1. **CyclicBarrier** - Sincronización de Fases

```java
// En MatrixGame - Inicialización
int numParticipantes = 1 + agentes.size();  // Neo + todos los agentes

barreraCalculo = new CyclicBarrier(numParticipantes, () -> {
    System.out.println("--- Todos calcularon su movimiento ---");
});

barreraAplicacion = new CyclicBarrier(numParticipantes, () -> {
    turnoActual++;
    System.out.println("--- Todos aplicaron su movimiento ---");
    imprimirTablero();
});
```

**¿Por qué CyclicBarrier?**
- Sincroniza N hilos en un punto específico
- Reutilizable (se "resetea" automáticamente)
- Permite ejecutar acción al completarse (imprimir tablero)
- Previene lecturas/escrituras simultáneas del tablero

**Funcionamiento**:
```
Turno 1:
  Neo:     [calcula] → await() → [aplica] → await() 
  Agente1: [calcula] → await() → [aplica] → await()
  Agente2: [calcula] → await() → [aplica] → await()
  Agente3: [calcula] → await() → [aplica] → await()
  Agente4: [calcula] → await() → [aplica] → await()
                         ↓                    ↓
                   Todos listos         Todos listos
                   
Turno 2: [Se repite...]
```

#### 2. **synchronized** - Protección de Recursos Compartidos

**a) Lock del Tablero**:
```java
private Object lockTablero = new Object();

// Al aplicar movimiento
synchronized(lockTablero) {
    posX = proximaX;
    posY = proximaY;
}
```
- Previene que dos hilos modifiquen posiciones simultáneamente
- Garantiza atomicidad al actualizar coordenadas

**b) Mapa de Posiciones Reservadas**:
```java
synchronized(posicionesReservadas) {
    if (!posicionesReservadas.containsKey(clave)) {
        posicionesReservadas.put(clave, id);
    }
}
```
- Evita que dos agentes reserven la misma casilla
- Operación check-and-set atómica

**c) Verificación de Victoria/Derrota**:
```java
// En Neo
synchronized(lockTablero) {
    gano = true;
    juegoActivo = false;
    tel.setUsado(true);
}

// En Agente
synchronized(lockTablero) {
    if (neo.isVivo()) {
        neo.setVivo(false);
        juegoActivo = false;
    }
}
```
- Previene condiciones de carrera al terminar el juego
- Garantiza que solo un hilo declare el fin del juego

#### 3. **ConcurrentHashMap** - Mapa Thread-Safe

```java
Map<String, Integer> posicionesReservadas = new ConcurrentHashMap<>();
```

**¿Por qué ConcurrentHashMap?**
- Thread-safe sin sincronización externa
- Permite lecturas concurrentes
- Escrituras eficientes con bloqueo fino
- Ideal para múltiples agentes accediendo simultáneamente

#### 4. **Volatile implícito** - Variables de Estado

```java
private boolean juegoActivo = true;
private boolean vivo = true;
```

Aunque no están marcadas explícitamente como `volatile`, estas variables:
- Se acceden/modifican dentro de bloques `synchronized`
- Tienen garantía de visibilidad entre hilos
- Controlan la terminación de los bucles `while`

### Flujo de Concurrencia Completo

```
╔════════════════════════════════════════════════════════════╗
║                    INICIO DEL JUEGO                        ║
╚════════════════════════════════════════════════════════════╝
                            │
                            ▼
        ┌─────────────────────────────────────┐
        │   Crear Hilos (Neo + 4 Agentes)    │
        │   - Neo.start()                     │
        │   - Agente1.start()                 │
        │   - Agente2.start()                 │
        │   - ...                             │
        └─────────────────────────────────────┘
                            │
        ╔═══════════════════▼═══════════════════╗
        ║         LOOP PRINCIPAL (Turno)        ║
        ╚═══════════════════════════════════════╝
                            │
        ┌───────────────────▼───────────────────┐
        │      FASE 1: CÁLCULO                  │
        │  ┌─────────────────────────────────┐  │
        │  │ Neo:     calcularMovimiento()   │  │
        │  │ Agente1: calcularMovimiento()   │  │
        │  │ Agente2: calcularMovimiento()   │  │
        │  │ ...                             │  │
        │  └─────────────────────────────────┘  │
        └───────────────────┬───────────────────┘
                            │
                    ▼▼▼ await() ▼▼▼
        ┌───────────────────────────────────────┐
        │       BARRERA DE CÁLCULO              │
        │   (Esperar a que TODOS calculen)      │
        └───────────────────┬───────────────────┘
                            │
        ┌───────────────────▼───────────────────┐
        │      FASE 2: APLICACIÓN               │
        │  ┌─────────────────────────────────┐  │
        │  │ synchronized(lockTablero) {     │  │
        │  │   posX = proximaX               │  │
        │  │   posY = proximaY               │  │
        │  │ }                               │  │
        │  └─────────────────────────────────┘  │
        └───────────────────┬───────────────────┘
                            │
                        await()
        ┌───────────────────────────────────────┐
        │         BARRERA DE APLICACIÓN         │
        │  (Esperar a que TODOS apliquen)       │
        │  ┌─────────────────────────────────┐  │
        │  │ Acción: turno++                 │  │
        │  │         imprimirTablero()       │  │
        │  │         limpiar reservas        │  │
        │  └─────────────────────────────────┘  │
        └───────────────────┬───────────────────┘
                            │
        ┌───────────────────▼───────────────────┐
        │    VERIFICACIÓN DE CONDICIONES        │
        │  ┌─────────────────────────────────┐  │
        │  │ Neo llegó al teléfono? → GANA   │  │
        │  │ Agente capturó a Neo? → PIERDE  │  │
        │  └─────────────────────────────────┘  │
        └───────────────────┬───────────────────┘
                            │
                    ┌───────┴────────┐
                    │                │
            Continuar ◄────┐    Fin del juego
                    │      │          │
                    └──────┘          ▼
                                 FIN DEL LOOP

```

---

## Resumen de Conceptos de Concurrencia

| Concepto | Uso en el Proyecto | Propósito |
|----------|-------------------|-----------|
| **Runnable** | `Persona implements Runnable` | Permite ejecutar personajes en hilos separados |
| **Thread** | `new Thread(neo).start()` | Crea hilos para cada entidad |
| **CyclicBarrier** | 2 barreras (cálculo/aplicación) | Sincroniza fases del turno |
| **synchronized** | Bloques críticos | Protege recursos compartidos |
| **ConcurrentHashMap** | Posiciones reservadas | Almacenamiento thread-safe |
| **Object lock** | `lockTablero` | Coordina acceso al tablero |
| **Interrupt** | `thread.interrupt()` | Termina hilos limpiamente |

---

## Conclusiones

Al haber terminado este pequeño proyecto, podemos concluir que los objetivos alcanzados fueron:

1. **Sincronización de fases** con `CyclicBarrier`
2. **Exclusión mutua** con `synchronized`
3. **Comunicación entre hilos** mediante estructuras compartidas
4. **Terminación ordenada** con interrupciones y flags
5. **Algoritmos distribuidos** (agentes coordinándose)

El diseño en dos fases (cálculo/aplicación) es muy útil para este ejercicio debido a que:
- Elimina condiciones de carrera
- Permite procesamiento paralelo del cálculo
- Garantiza actualizaciones atómicas del estado
- Facilita depuración y razonamiento sobre el código

---

**Autores**: Ignacio Andrés Castillo Rendón
**Inspiración**: Matrix (1999) - Wachowski Sisters
**Referencias**:
- https://www.datacamp.com/es/tutorial/manhattan-distance
- https://keepcoding.io/blog/que-es-java-util-concurrent-cyclicbarrier/
- https://www.tutorialesprogramacionya.com/javaya/detalleconcepto.php?codigo=182
- https://www-bairesdev-com.translate.goog/blog/java-concurrency/?_x_tr_sl=en&_x_tr_tl=es&_x_tr_hl=es&_x_tr_pto=tc
- https://blog.softtek.com/es/java-concurrency
