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
    
    private int proximaX;
    private int proximaY;
    private boolean movimientoCalculado;
    
    public Agente(int posX, int posY, Neo neo, List<Agente> otrosAgentes,
                  List<Muro> muros, CyclicBarrier barreraCalculo,
                  CyclicBarrier barreraAplicacion, Object lockTablero) {
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
                movimientoCalculado = calcularProximoMovimiento();
                
                barreraCalculo.await();
                
                if (!juegoActivo || !vivo) break;

                synchronized(lockTablero) {
                    if (movimientoCalculado) {
                        posX = proximaX;
                        posY = proximaY;
                        System.out.println(">>> " + nombre + " se movió a (" + 
                                         posX + ", " + posY + ")");
                    }
                }
                
                barreraAplicacion.await();
                
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
     * Cálculo del próximo movimiento sin modificar la posición actual
     */
    private boolean calcularProximoMovimiento() {
        if (!neo.isVivo()) {
            return false;
        }
        
        int[] siguientePaso = bfsConCoordinacion();
        
        if (siguientePaso != null) {
            proximaX = siguientePaso[0];
            proximaY = siguientePaso[1];
            return true;
        }
        
        proximaX = posX;
        proximaY = posY;
        return false;
    }
    
    /**
     * Verificación de la captura de Neo.
     */
    private void verificarCaptura() {
        if (posX == neo.getPosX() && posY == neo.getPosY() && neo.isVivo()) {
            synchronized(lockTablero) {
                if (neo.isVivo()) {
                    System.out.println("\n╔════════════════════════════════╗");
                    System.out.println("║  ¡" + nombre + " CAPTURÓ A NEO!  ║");
                    System.out.println("║      ¡JUEGO PERDIDO! 💀        ║");
                    System.out.println("╚════════════════════════════════╝\n");
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
    
    /**
     * Calcula dirección preferida para rodear a Neo
     */
    private int calcularDireccionPreferida(int neoX, int neoY) {
        int miPosicionRelativa = 0;
        
        if (posX < neoX && posY < neoY) miPosicionRelativa = 0;
        else if (posX < neoX && posY >= neoY) miPosicionRelativa = 1;
        else if (posX >= neoX && posY < neoY) miPosicionRelativa = 2;
        else miPosicionRelativa = 3;
        
        return (miPosicionRelativa + id) % 4;
    }
    
    /**
     * Reordena direcciones según preferencia
     */
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
