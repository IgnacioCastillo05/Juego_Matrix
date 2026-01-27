package matrix;
import java.util.*;
import java.util.concurrent.*;

public class Agente extends Persona {
    private Neo neo;
    private List<Agente> otrosAgentes;
    private List<Muro> muros;
    private CyclicBarrier barreraCalculo;
    private CyclicBarrier barreraAplicacion;
    private Object lockTablero;
    private boolean juegoActivo;
    private static int contadorAgentes = 0;
    private int id;
    
    // Variables para el movimiento en dos fases
    private int proximaX;
    private int proximaY;
    private boolean movimientoCalculado;
    
    // Mapa compartido de posiciones reservadas
    private Map<String, Integer> posicionesReservadas;  // ← NUEVO
    
    public Agente(int posX, int posY, Neo neo, List<Agente> otrosAgentes,
                  List<Muro> muros, CyclicBarrier barreraCalculo,
                  CyclicBarrier barreraAplicacion, Object lockTablero,
                  Map<String, Integer> posicionesReservadas) {  // ← NUEVO PARÁMETRO
        super(posX, posY, 'A', "Agente-" + (++contadorAgentes));
        this.id = contadorAgentes;
        this.neo = neo;
        this.otrosAgentes = otrosAgentes;
        this.muros = muros;
        this.barreraCalculo = barreraCalculo;
        this.barreraAplicacion = barreraAplicacion;
        this.lockTablero = lockTablero;
        this.juegoActivo = true;
        this.movimientoCalculado = false;
        this.posicionesReservadas = posicionesReservadas;  // ← NUEVO
    }
    
    public int getId() {
        return id;
    }
    
    public void setJuegoActivo(boolean juegoActivo) {
        this.juegoActivo = juegoActivo;
    }
    
    @Override
    public void run() {
        try {
            while (juegoActivo && vivo) {
                // ===== FASE 1: CÁLCULO =====
                movimientoCalculado = calcularProximoMovimiento();
                
                // Espera a que todos terminen de calcular
                barreraCalculo.await();
                
                if (!juegoActivo || !vivo) break;
                
                // ===== FASE 2: APLICACIÓN =====
                synchronized(lockTablero) {
                    if (movimientoCalculado) {
                        // Liberar la posición anterior
                        String claveAnterior = posX + "," + posY;
                        synchronized(posicionesReservadas) {
                            posicionesReservadas.remove(claveAnterior);
                        }
                        
                        posX = proximaX;
                        posY = proximaY;
                        System.out.println(">>> " + nombre + " se movió a (" + 
                                         posX + ", " + posY + ")");
                    }
                }
                
                // Espera a que todos terminen de moverse
                barreraAplicacion.await();
                
                // ===== FASE 3: VERIFICACIÓN =====
                verificarCaptura();
                
                if (!juegoActivo) break;
                
                Thread.sleep(500);
            }
        } catch (InterruptedException | BrokenBarrierException e) {
            Thread.currentThread().interrupt();
            System.out.println(nombre + " interrumpido");
        }
    }
    
    @Override
    public boolean movimiento(char[][] tablero) {
        return false;
    }
    
    /**
     * FASE 1: Calcula el próximo movimiento
     */
    private boolean calcularProximoMovimiento() {
        if (!neo.isVivo()) {
            return false;
        }
        
        int[] siguientePaso = bfsConCoordinacion();
        
        if (siguientePaso != null) {
            // Intentar reservar la posición
            String clave = siguientePaso[0] + "," + siguientePaso[1];
            
            synchronized(posicionesReservadas) {
                // Si la posición ya está reservada por otro agente
                if (posicionesReservadas.containsKey(clave)) {
                    // Buscar una posición alternativa
                    int[] alternativa = buscarPosicionAlternativa();
                    if (alternativa != null) {
                        siguientePaso = alternativa;
                        clave = alternativa[0] + "," + alternativa[1];
                    } else {
                        // No hay alternativa, quedarse en el mismo lugar
                        proximaX = posX;
                        proximaY = posY;
                        return false;
                    }
                }
                
                // Reservar la posición
                posicionesReservadas.put(clave, id);
                proximaX = siguientePaso[0];
                proximaY = siguientePaso[1];
            }
            
            return true;
        }
        
        proximaX = posX;
        proximaY = posY;
        return false;
    }
    
    /**
     * Busca una posición alternativa si la preferida está ocupada
     */
    private int[] buscarPosicionAlternativa() {
        final int TAMANIO = 10;
        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};
        
        // Probar las 4 direcciones alrededor de la posición actual
        for (int i = 0; i < 4; i++) {
            int nx = posX + dx[i];
            int ny = posY + dy[i];
            
            if (!posicionValida(nx, ny, TAMANIO)) continue;
            if (hayMuro(nx, ny)) continue;
            
            String clave = nx + "," + ny;
            synchronized(posicionesReservadas) {
                if (!posicionesReservadas.containsKey(clave)) {
                    // Posición válida y disponible
                    return new int[]{nx, ny};
                }
            }
        }
        
        return null; // No hay alternativa
    }
    
    /**
     * FASE 3: Verifica si capturó a Neo
     */
    private void verificarCaptura() {
        if (posX == neo.getPosX() && posY == neo.getPosY() && neo.isVivo()) {
            synchronized(lockTablero) {
                if (neo.isVivo()) {
                    System.out.println("\n╔════════════════════════════════════╗");
                    System.out.println("║  ¡" + nombre + " CAPTURÓ A NEO!       ║");
                    System.out.println("║      ¡JUEGO PERDIDO!                ║");
                    System.out.println("╚════════════════════════════════════╝\n");
                    neo.setVivo(false);
                    neo.setJuegoActivo(false);
                    juegoActivo = false;
                }
            }
        }
    }
    
    /**
     * BFS con coordinación entre agentes
     */
    private int[] bfsConCoordinacion() {
        final int TAMANIO = 10;
        int neoX = neo.getPosX();
        int neoY = neo.getPosY();
        
        int direccionPreferida = calcularDireccionPreferida(neoX, neoY);
        
        Queue<int[]> cola = new LinkedList<>();
        boolean[][] visitado = new boolean[TAMANIO][TAMANIO];
        int[][] padre = new int[TAMANIO][TAMANIO];
        
        cola.offer(new int[]{posX, posY});
        visitado[posX][posY] = true;
        padre[posX][posY] = -1;
        
        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};
        
        int[][] direccionesOrdenadas = ordenarDirecciones(dx, dy, direccionPreferida);
        
        while (!cola.isEmpty()) {
            int[] actual = cola.poll();
            int x = actual[0];
            int y = actual[1];
            
            if (x == neoX && y == neoY) {
                return reconstruirPrimerPaso(padre, neoX, neoY);
            }
            
            for (int[] dir : direccionesOrdenadas) {
                int nx = x + dir[0];
                int ny = y + dir[1];
                
                if (!posicionValida(nx, ny, TAMANIO) || visitado[nx][ny]) {
                    continue;
                }
                
                if (hayMuro(nx, ny)) {
                    continue;
                }
                
                // Permite moverse a la posición de Neo, pero no a la de otros agentes
                if (hayOtroAgente(nx, ny) && !(nx == neoX && ny == neoY)) {
                    continue;
                }
                
                visitado[nx][ny] = true;
                padre[nx][ny] = x * TAMANIO + y;
                cola.offer(new int[]{nx, ny});
            }
        }
        
        return null;
    }
    
    private int calcularDireccionPreferida(int neoX, int neoY) {
        int miPosicionRelativa = 0;
        
        if (posX < neoX && posY < neoY) miPosicionRelativa = 0;
        else if (posX < neoX && posY >= neoY) miPosicionRelativa = 1;
        else if (posX >= neoX && posY < neoY) miPosicionRelativa = 2;
        else miPosicionRelativa = 3;
        
        return (miPosicionRelativa + id) % 4;
    }
    
    private int[][] ordenarDirecciones(int[] dx, int[] dy, int preferencia) {
        int[][] direcciones = new int[4][2];
        
        for (int i = 0; i < 4; i++) {
            direcciones[i][0] = dx[i];
            direcciones[i][1] = dy[i];
        }
        
        if (preferencia > 0 && preferencia < 4) {
            int[] temp = direcciones[preferencia];
            direcciones[preferencia] = direcciones[0];
            direcciones[0] = temp;
        }
        
        return direcciones;
    }
    
    private boolean hayMuro(int x, int y) {
        for (Muro muro : muros) {
            if (muro.getPosX() == x && muro.getPosY() == y) {
                return true;
            }
        }
        return false;
    }
    
    private boolean hayOtroAgente(int x, int y) {
        for (Agente agente : otrosAgentes) {
            if (agente != this && agente.isVivo() && 
                agente.getPosX() == x && agente.getPosY() == y) {
                return true;
            }
        }
        return false;
    }
    
    private int[] reconstruirPrimerPaso(int[][] padre, int destinoX, int destinoY) {
        final int TAMANIO = 10;
        int x = destinoX;
        int y = destinoY;
        int anteriorX = x;
        int anteriorY = y;
        
        while (padre[x][y] != -1) {
            anteriorX = x;
            anteriorY = y;
            int codPadre = padre[x][y];
            x = codPadre / TAMANIO;
            y = codPadre % TAMANIO;
            
            if (x == posX && y == posY) {
                return new int[]{anteriorX, anteriorY};
            }
        }
        
        return new int[]{posX, posY};
    }
}