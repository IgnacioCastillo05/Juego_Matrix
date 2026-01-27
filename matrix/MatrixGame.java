package matrix;

import java.util.*;
import java.util.concurrent.*;

public class MatrixGame {
    private static final int TAMANIO = 10;
    private static final int MAX_MUROS = 10;
    private static final int MIN_AGENTES = 2;
    private static final int MAX_AGENTES = 4;
    
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
        
        // 1. Configuración inicial del tablero
        configurarJuego();
        
        // 2. Crear las barreras cíclicas
        // Número de participantes = 1 Neo + cantidad de agentes
        int numParticipantes = 1 + agentes.size();
        
        // La barrera de cálculo espera a que todos calculen su movimiento
        barreraCalculo = new CyclicBarrier(numParticipantes, () -> {
            // Esta acción se ejecuta cuando todos llegan a la barrera
            System.out.println("\n--- Todos calcularon su movimiento ---");
        });
        
        // La barrera de aplicación espera a que todos apliquen su movimiento
        barreraAplicacion = new CyclicBarrier(numParticipantes, () -> {
            // Esta acción se ejecuta cuando todos aplicaron su movimiento
            turnoActual++;
            System.out.println("--- Todos aplicaron su movimiento ---");
            System.out.println("\n========== TURNO " + turnoActual + " ==========");
            imprimirTablero();
        });
        
        // 3. Asignar barreras a Neo
        neo = new Neo(neo.getPosX(), neo.getPosY(), telefonos, agentes,
                     barreraCalculo, barreraAplicacion, lockTablero);
        
        // 4. Asignar barreras a todos los agentes
        for (int i = 0; i < agentes.size(); i++) {
            Agente agenteViejo = agentes.get(i);
            Agente agenteNuevo = new Agente(
                agenteViejo.getPosX(), 
                agenteViejo.getPosY(),
                neo, 
                agentes, 
                muros,
                barreraCalculo,
                barreraAplicacion,
                lockTablero
            );
            agentes.set(i, agenteNuevo);
        }
        
        // 5. Mostrar tablero inicial
        System.out.println("\n========== TABLERO INICIAL ==========");
        imprimirTablero();
        
        System.out.println("\nPresiona ENTER para iniciar la simulación...");
        scanner.nextLine();
        
        // 6. Iniciar los hilos
        Thread hiloNeo = new Thread(neo);
        List<Thread> hilosAgentes = new ArrayList<>();
        
        hiloNeo.start();
        
        for (Agente agente : agentes) {
            Thread hiloAgente = new Thread(agente);
            hilosAgentes.add(hiloAgente);
            hiloAgente.start();
        }
        
        // 7. Esperar a que el juego termine
        try {
            hiloNeo.join();
            for (Thread hilo : hilosAgentes) {
                hilo.join();
            }
        } catch (InterruptedException e) {
            System.out.println("El juego fue interrumpido");
        }
        
        // 8. Mostrar resultado final
        mostrarResultadoFinal();
        scanner.close();
    }
    
    /**
     * Configura el juego pidiendo posiciones al usuario y generando obstáculos
     */
    private void configurarJuego() {
        // Pedir posición de Neo
        System.out.println("\n=== CONFIGURACIÓN DE NEO ===");
        int[] posNeo = pedirPosicion("Neo");
        neo = new Neo(posNeo[0], posNeo[1], telefonos, agentes, 
                     barreraCalculo, barreraAplicacion, lockTablero);
        
        // Pedir posiciones de los 2 teléfonos
        System.out.println("\n=== CONFIGURACIÓN DE TELÉFONOS ===");
        for (int i = 1; i <= 2; i++) {
            int[] posTel = pedirPosicionUnica("Teléfono " + i, 
                          Arrays.asList(posNeo));
            telefonos.add(new Telefono(posTel[0], posTel[1]));
        }
        
        // Generar muros aleatorios
        generarMurosAleatorios();
        
        // Generar agentes aleatorios
        generarAgentesAleatorios();
        
        System.out.println("\n✓ Configuración completada");
        System.out.println("  - Neo: (" + neo.getPosX() + ", " + neo.getPosY() + ")");
        System.out.println("  - Teléfonos: " + telefonos.size());
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
                    System.out.println("❌ Posición fuera de rango. Intenta de nuevo.");
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Entrada inválida. Ingresa números entre 0 y 9.");
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
                System.out.println("❌ Esa posición ya está ocupada. Intenta otra.");
            }
        }
    }
    
    /**
     * Genera muros en posiciones aleatorias
     */
    private void generarMurosAleatorios() {
        Random rand = new Random();
        int cantidadMuros = rand.nextInt(MAX_MUROS) + 1; // Entre 1 y 10 muros
        
        System.out.println("\n=== GENERANDO MUROS ALEATORIOS ===");
        System.out.println("Cantidad de muros a generar: " + cantidadMuros);
        
        int murosGenerados = 0;
        int intentos = 0;
        int maxIntentos = 100; // Para evitar bucle infinito
        
        while (murosGenerados < cantidadMuros && intentos < maxIntentos) {
            int x = rand.nextInt(TAMANIO);
            int y = rand.nextInt(TAMANIO);
            
            // Verifica que la posición no esté ocupada
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
        int cantidadAgentes = rand.nextInt(MAX_AGENTES - MIN_AGENTES + 1) + MIN_AGENTES;
        
        System.out.println("\n=== GENERANDO AGENTES ALEATORIOS ===");
        System.out.println("Cantidad de agentes a generar: " + cantidadAgentes);
        
        int agentesGenerados = 0;
        int intentos = 0;
        int maxIntentos = 100;
        
        while (agentesGenerados < cantidadAgentes && intentos < maxIntentos) {
            int x = rand.nextInt(TAMANIO);
            int y = rand.nextInt(TAMANIO);
            
            if (!posicionOcupada(x, y)) {
                // Creamos un agente temporal (luego se recreará con las barreras)
                Agente agente = new Agente(x, y, neo, agentes, muros,
                                          null, null, lockTablero);
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
        // Verifica Neo
        if (neo != null && neo.getPosX() == x && neo.getPosY() == y) {
            return true;
        }
        
        // Verifica teléfonos
        for (Telefono tel : telefonos) {
            if (tel.getPosX() == x && tel.getPosY() == y) {
                return true;
            }
        }
        
        // Verifica muros
        for (Muro muro : muros) {
            if (muro.getPosX() == x && muro.getPosY() == y) {
                return true;
            }
        }
        
        // Verifica agentes
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
        
        // Colocar teléfonos
        for (Telefono tel : telefonos) {
            if (!tel.isUsado()) {
                tel.colocarEnTablero(tablero);
            }
        }
        
        // Colocar muros
        for (Muro muro : muros) {
            muro.colocarEnTablero(tablero);
        }
        
        // Colocar agentes
        for (Agente agente : agentes) {
            if (agente.isVivo()) {
                agente.colocarEnTablero(tablero);
            }
        }
        
        // Colocar Neo (al final para que tenga prioridad visual)
        if (neo.isVivo()) {
            neo.colocarEnTablero(tablero);
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
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║                                        ║");
        System.out.println("║         BIENVENIDO A MATRIX            ║");
        System.out.println("║                                        ║");
        System.out.println("║  Neo debe llegar al teléfono más       ║");
        System.out.println("║  cercano evitando a los Agentes        ║");
        System.out.println("║                                        ║");
        System.out.println("╚════════════════════════════════════════╝\n");
    }
    
    /**
     * Muestra el resultado final del juego
     */
    private void mostrarResultadoFinal() {
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║          RESULTADO FINAL               ║");
        System.out.println("╠════════════════════════════════════════╣");
        
        if (neo.isGano()) {
            System.out.println("║                                        ║");
            System.out.println("║      🎉 ¡NEO ESCAPÓ DE MATRIX! 🎉     ║");
            System.out.println("║                                        ║");
        } else {
            System.out.println("║                                        ║");
            System.out.println("║      💀 NEO FUE CAPTURADO 💀          ║");
            System.out.println("║                                        ║");
        }
        
        System.out.println("╠════════════════════════════════════════╣");
        System.out.println("║  Estadísticas:                         ║");
        System.out.println("║  - Turnos jugados: " + String.format("%-19d", turnoActual) + "║");
        System.out.println("║  - Agentes activos: " + String.format("%-18d", contarAgentesVivos()) + "║");
        System.out.println("╚════════════════════════════════════════╝\n");
        
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
