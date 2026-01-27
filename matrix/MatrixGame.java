package matrix;

import java.util.*;
import java.util.concurrent.*;

public class MatrixGame {
    private static final int TAMANIO = 10;
    private static final int MAX_MUROS = 10;
    
    private char[][] tablero;
    private Neo neo;
    private List<Agente> agentes;
    private List<Telefono> telefonos;
    private List<Muro> muros;
    
    private CyclicBarrier barreraCalculo;
    private CyclicBarrier barreraAplicacion;
    private Object lockTablero;
    
    private int turnoActual;
    private Scanner scanner;
    
    public MatrixGame() {
        this.tablero = new char[TAMANIO][TAMANIO];
        this.agentes = new ArrayList<>();
        this.telefonos = new ArrayList<>();
        this.muros = new ArrayList<>();
        this.lockTablero = new Object();
        this.turnoActual = 0;
        this.scanner = new Scanner(System.in);
    }
    
    public static void main(String[] args) {
        MatrixGame juego = new MatrixGame();
        juego.iniciar();
    }
    
    /**
     * Método principal que inicia y controla el juego
     */
    public void iniciar() {
        mostrarBienvenida();
 
        configurarJuego();

        inicializarSistemaConcurrencia();

        mostrarEstadoInicial();

        ejecutarSimulacion();
  
        mostrarResultadoFinal();
        scanner.close();
    }

    /**
     * Inicializa las barreras cíclicas y recrea los personajes con las referencias correctas
     */
    private void inicializarSistemaConcurrencia() {
        Map<String, Integer> posicionesReservadas = new ConcurrentHashMap<>();

        int numParticipantes = 1 + agentes.size();
        
        barreraCalculo = new CyclicBarrier(numParticipantes, () -> {
            System.out.println("\n--- Todos calcularon su movimiento ---");
        });
        
        barreraAplicacion = new CyclicBarrier(numParticipantes, () -> {
            posicionesReservadas.clear();
            
            turnoActual++;
            System.out.println("--- Todos aplicaron su movimiento ---");
            System.out.println("\n========== TURNO " + turnoActual + " ==========");
            imprimirTablero();
        });

        neo = new Neo(neo.getPosX(), neo.getPosY(), telefonos, agentes, muros,
                    barreraCalculo, barreraAplicacion, lockTablero);

        for (int i = 0; i < agentes.size(); i++) {
            Agente agenteViejo = agentes.get(i);
            Agente agenteNuevo = new Agente(
                agenteViejo.getPosX(), 
                agenteViejo.getPosY(),
                neo, 
                agentes, 
                muros,
                telefonos,
                barreraCalculo,
                barreraAplicacion,
                lockTablero,
                posicionesReservadas
            );
            agentes.set(i, agenteNuevo);
        }
    }

    /**
     * Muestra el tablero inicial y espera confirmación del usuario
     */
    private void mostrarEstadoInicial() {
        System.out.println("\n========== TABLERO INICIAL ==========");
        imprimirTablero();
        
        System.out.println("\nPresiona ENTER para iniciar la simulación...");
        scanner.nextLine();
    }

    /**
     * Ejecuta la simulación del juego iniciando y coordinando todos los hilos
     */
    private void ejecutarSimulacion() {
        // Iniciar los hilos
        Thread hiloNeo = new Thread(neo);
        List<Thread> hilosAgentes = new ArrayList<>();
        
        hiloNeo.start();
        
        for (Agente agente : agentes) {
            Thread hiloAgente = new Thread(agente);
            hilosAgentes.add(hiloAgente);
            hiloAgente.start();
        }
        
        try {
            monitorerarJuego(hiloNeo, hilosAgentes);
        } catch (InterruptedException e) {
            System.out.println("El juego fue interrumpido");
        } catch (Exception e) {
            System.out.println("Error en la ejecución del juego: " + e.getMessage());
        }
    }

    /**
     * Monitorea el estado del juego y maneja la finalización de los hilos
     */
    private void monitorerarJuego(Thread hiloNeo, List<Thread> hilosAgentes) 
            throws InterruptedException {
        while (neo.isVivo() && !neo.isGano()) {
            Thread.sleep(100);
        }
        
        Thread.sleep(1000);

        barreraCalculo.reset();
        barreraAplicacion.reset();

        hiloNeo.interrupt();
        for (Thread hilo : hilosAgentes) {
            hilo.interrupt();
        }

        hiloNeo.join(1000);
        for (Thread hilo : hilosAgentes) {
            hilo.join(500);
        }
    }
    
    /**
     * Configura el juego pidiendo posiciones al usuario y generando obstáculos
     */
    private void configurarJuego() {
        System.out.println("\n=== CONFIGURACIÓN DE NEO ===");
        int[] posNeo = pedirPosicion("Neo");
        neo = new Neo(posNeo[0], posNeo[1], telefonos, agentes, muros,
                    barreraCalculo, barreraAplicacion, lockTablero);
        
        List<int[]> posicionesOcupadas = new ArrayList<>();
        posicionesOcupadas.add(posNeo);
        
        generarTelefonosAleatorios(posicionesOcupadas);

        generarMurosAleatorios();

        generarAgentesAleatorios();
        
        System.out.println("\nConfiguración completada");
        System.out.println("  - Neo: (" + neo.getPosX() + ", " + neo.getPosY() + ")");
        System.out.println("  - Teléfono(s): " + telefonos.size());
        System.out.println("  - Muros: " + muros.size());
        System.out.println("  - Agentes: " + agentes.size());
    }
    
    /**
     * Pide al usuario una posición válida
     */
    private int[] pedirPosicion(String elemento) {
        int x, y;
        while (true) {
            try {
                System.out.print("Ingresa la posición X de " + elemento + " (0-9): ");
                x = Integer.parseInt(scanner.nextLine());
                System.out.print("Ingresa la posición Y de " + elemento + " (0-9): ");
                y = Integer.parseInt(scanner.nextLine());
                
                if (x >= 0 && x < TAMANIO && y >= 0 && y < TAMANIO) {
                    return new int[]{x, y};
                } else {
                    System.out.println("Posición fuera de rango. Intenta de nuevo.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Entrada inválida. Ingresa números entre 0 y 9.");
            }
        }
    }
    
    /**
     * Pide una posición que no esté ocupada
     */
    private int[] pedirPosicionUnica(String elemento, List<int[]> posicionesOcupadas) {
        int x, y;
        while (true) {
            int[] pos = pedirPosicion(elemento);
            x = pos[0];
            y = pos[1];
            
            boolean ocupada = false;
            for (int[] ocupada_pos : posicionesOcupadas) {
                if (ocupada_pos[0] == x && ocupada_pos[1] == y) {
                    ocupada = true;
                    break;
                }
            }
            
            if (!ocupada) {
                posicionesOcupadas.add(pos);
                return pos;
            } else {
                System.out.println("Esa posición ya está ocupada. Intenta otra.");
            }
        }
    }

    /**
     * Genera teléfonos en posiciones aleatorias
     */
    private void generarTelefonosAleatorios(List<int[]> posicionesOcupadas) {
        Random rand = new Random();
        int cantidadTelefonos = rand.nextInt(2) + 1;
        
        System.out.println("Cantidad de teléfonos a generar: " + cantidadTelefonos);
        
        int telefonosGenerados = 0;
        int intentos = 0;
        int maxIntentos = 100;
        
        while (telefonosGenerados < cantidadTelefonos && intentos < maxIntentos) {
            int x = rand.nextInt(TAMANIO);
            int y = rand.nextInt(TAMANIO);
            
            // Verifica que la posición no esté ocupada
            boolean ocupada = false;
            for (int[] pos : posicionesOcupadas) {
                if (pos[0] == x && pos[1] == y) {
                    ocupada = true;
                    break;
                }
            }
            if (!ocupada) {
                telefonos.add(new Telefono(x, y));
                posicionesOcupadas.add(new int[]{x, y});
                System.out.println("  Teléfono " + (telefonosGenerados + 1) + " en (" + x + ", " + y + ")");
                telefonosGenerados++;
            }
            
            intentos++;
        }
        if (telefonosGenerados < cantidadTelefonos) {
            System.out.println("Advertencia: Solo se pudieron generar " + telefonosGenerados + " teléfonos");
        }
    }
    
    /**
     * Genera muros en posiciones aleatorias
     */
    private void generarMurosAleatorios() {
        Random rand = new Random();
        int cantidadMuros = rand.nextInt(MAX_MUROS) + 1; 
        
        System.out.println("Cantidad de muros a generar: " + cantidadMuros);
        
        int murosGenerados = 0;
        int intentos = 0;
        int maxIntentos = 100;
        
        while (murosGenerados < cantidadMuros && intentos < maxIntentos) {
            int x = rand.nextInt(TAMANIO);
            int y = rand.nextInt(TAMANIO);
            
            if (!posicionOcupada(x, y)) {
                muros.add(new Muro(x, y));
                System.out.println("  Muro " + (murosGenerados + 1) + " en (" + x + ", " + y + ")");
                murosGenerados++;
            }
            intentos++;
        }
    }
    
    /**
     * Genera agentes en posiciones aleatorias
     */
    private void generarAgentesAleatorios() {
        Random rand = new Random();
        int cantidadAgentes = 4;
        
        System.out.println("Cantidad de agentes a generar: " + cantidadAgentes);
        
        int agentesGenerados = 0;
        int intentos = 0;
        int maxIntentos = 100;
        
        while (agentesGenerados < cantidadAgentes && intentos < maxIntentos) {
            int x = rand.nextInt(TAMANIO);
            int y = rand.nextInt(TAMANIO);
            
            if (!posicionOcupada(x, y)) {
                Agente agente = new Agente(x, y, neo, agentes, muros, telefonos,
                                      null, null, lockTablero, null);
            agentes.add(agente);
                System.out.println("  Agente-" + (agentesGenerados + 1) + " en (" + x + ", " + y + ")");
                agentesGenerados++;
            }
            intentos++;
        }
    }
    
    /**
     * Verifica si una posición está ocupada
     */
    private boolean posicionOcupada(int x, int y) {
        if (neo != null && neo.getPosX() == x && neo.getPosY() == y) {
            return true;
        }
      
        for (Telefono tel : telefonos) {
            if (tel.getPosX() == x && tel.getPosY() == y) {
                return true;
            }
        }

        for (Muro muro : muros) {
            if (muro.getPosX() == x && muro.getPosY() == y) {
                return true;
            }
        }

        for (Agente agente : agentes) {
            if (agente.getPosX() == x && agente.getPosY() == y) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Imprime el tablero actual en consola
     */
    private void imprimirTablero() {
        // Limpiar tablero
        for (int i = 0; i < TAMANIO; i++) {
            Arrays.fill(tablero[i], '.');
        }
        
        // Colocar teléfonos - CAMBIO: [y][x] en lugar de [x][y]
        for (Telefono tel : telefonos) {
            if (!tel.isUsado()) {
                tablero[tel.getPosY()][tel.getPosX()] = tel.getSimbolo();
            }
        }
        
        // Colocar muros - CAMBIO: [y][x]
        for (Muro muro : muros) {
            tablero[muro.getPosY()][muro.getPosX()] = muro.getSimbolo();
        }
        
        // Colocar Neo PRIMERO (si está vivo) - CAMBIO: [y][x]
        if (neo.isVivo()) {
            tablero[neo.getPosY()][neo.getPosX()] = neo.getSimbolo();
        }
        
        // Colocar agentes AL FINAL - CAMBIO: [y][x]
        for (Agente agente : agentes) {
            if (agente.isVivo()) {
                tablero[agente.getPosY()][agente.getPosX()] = agente.getSimbolo();
            }
        }
        
        // Imprimir tablero con formato
        System.out.println("\n    0 1 2 3 4 5 6 7 8 9");
        System.out.println("  ┌─────────────────────┐");
        
        for (int i = 0; i < TAMANIO; i++) {
            System.out.print(i + " │ ");
            for (int j = 0; j < TAMANIO; j++) {
                System.out.print(tablero[i][j] + " ");
            }
            System.out.println("│");
        }
        
        System.out.println("  └─────────────────────┘");
        
        // Leyenda
        System.out.println("\nLeyenda: N=Neo | A=Agente | T=Teléfono | M=Muro | .=Vacío");
    }
    
    /**
     * Muestra mensaje de bienvenida
     */
    private void mostrarBienvenida() {
        System.out.println("BIENVENIDO A MATRIX");
        System.out.println("El objetivo es que Neo llegue al teléfono más cercano evitando a los Agentes.");
        System.out.println("¡Buena suerte!\n");
    }
    
    /**
     * Muestra el resultado final del juego
     */
    private void mostrarResultadoFinal() {
        System.out.println("RESULTADO FINAL DEL JUEGO");
        
        if (neo.isGano()) {
            System.out.println("¡ NEO HA LOGRADO ESCAPAR!");
        } else {
            System.out.println("NEO HA SIDO CAPTURADO POR LOS AGENTES.");
        }
        
        System.out.println("Estadísticas:");
        System.out.println("Turnos jugados: " + String.format("%-19d", turnoActual));
        System.out.println("Agentes activos: " + String.format("%-18d", contarAgentesVivos()));
        
        System.out.println("Gracias por jugar. ¡Hasta la próxima!");
    }
    
    /**
     * Cuenta cuántos agentes siguen vivos
     */
    private int contarAgentesVivos() {
        int vivos = 0;
        for (Agente agente : agentes) {
            if (agente.isVivo()) {
                vivos++;
            }
        }
        return vivos;
    }
}
