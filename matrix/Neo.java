package matrix;

import java.util.*;
import java.util.concurrent.*;

public class Neo extends Persona {
    private List<Telefono> telefonos;
    private List<Agente> agentes;
    private List<Muro> muros;
    private CyclicBarrier barreraCalculo; 
    private CyclicBarrier barreraAplicacion;
    private Object lockTablero;
    private boolean juegoActivo;
    private boolean gano;
    
    private int proximaX;
    private int proximaY;
    private boolean movimientoCalculado;
    
    public Neo(int posX, int posY, List<Telefono> telefonos, 
               List<Agente> agentes, List<Muro> muros,
               CyclicBarrier barreraCalculo,
               CyclicBarrier barreraAplicacion, Object lockTablero) {
        super(posX, posY, 'N', "Neo");
        this.telefonos = telefonos;
        this.agentes = agentes;
        this.muros = muros; 
        this.barreraCalculo = barreraCalculo;
        this.barreraAplicacion = barreraAplicacion;
        this.lockTablero = lockTablero;
        this.juegoActivo = true;
        this.gano = false;
        this.movimientoCalculado = false;
    }
    
    public boolean isGano() {
        return gano;
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

                verificarVictoria();
                
                if (!juegoActivo) break;

                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            // El hilo fue interrumpido, terminar limpiamente
            Thread.currentThread().interrupt();
        } catch (BrokenBarrierException e) {
            // La barrera fue reseteada, el juego terminó
            Thread.currentThread().interrupt();
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
        Telefono telefonoObjetivo = encontrarTelefonoMasCercano();
        
        if (telefonoObjetivo == null) {
            return false;
        }
        
        int[] siguientePaso = dijkstra(telefonoObjetivo);
        
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
     * Verificación de la victoria de Neo.
     */
    private void verificarVictoria() {
        for (Telefono tel : telefonos) {
            if (!tel.isUsado() && posX == tel.getPosX() && posY == tel.getPosY()) {
                synchronized(lockTablero) {
                    gano = true;
                    juegoActivo = false;
                    tel.setUsado(true);
                    System.out.println("¡Neo llegó al teléfono en (" + posX + ", " + posY + ") y escapó de Matrix!");
                }
                break;
            }
        }
    }
    
    /**
     * Encuentra el teléfono más cercano usando distancia Manhattan
     */
    private Telefono encontrarTelefonoMasCercano() {
        Telefono masCercano = null;
        int distanciaMinima = Integer.MAX_VALUE;
        
        for (Telefono tel : telefonos) {
            if (!tel.isUsado()) {
                int distancia = tel.distanciaHasta(posX, posY);
                if (distancia < distanciaMinima) {
                    distanciaMinima = distancia;
                    masCercano = tel;
                }
            }
        }
        
        return masCercano;
    }
    
    /**
     * Implementación del algoritmo de Dijkstra
     */
    private int[] dijkstra(Telefono objetivo) {
        final int TAMANIO = 10;
        int[][] distancias = new int[TAMANIO][TAMANIO];
        boolean[][] visitado = new boolean[TAMANIO][TAMANIO];
        int[][] padre = new int[TAMANIO][TAMANIO];
        
        for (int i = 0; i < TAMANIO; i++) {
            Arrays.fill(distancias[i], Integer.MAX_VALUE);
        }
        
        PriorityQueue<Nodo> cola = new PriorityQueue<>();
        distancias[posX][posY] = 0;
        cola.offer(new Nodo(posX, posY, 0));
        padre[posX][posY] = -1;
        
        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};
        
        while (!cola.isEmpty()) {
            Nodo actual = cola.poll();
            int x = actual.x;
            int y = actual.y;
            
            if (visitado[x][y]) continue;
            visitado[x][y] = true;
            
            if (x == objetivo.getPosX() && y == objetivo.getPosY()) {
                return reconstruirPrimerPaso(padre, objetivo.getPosX(), objetivo.getPosY());
            }
            
            for (int i = 0; i < 4; i++) {
                int nx = x + dx[i];
                int ny = y + dy[i];
                
                if (!posicionValida(nx, ny, TAMANIO) || visitado[nx][ny]) {
                    continue;
                }
                
                int costo = calcularCosto(nx, ny); 
                if (costo == Integer.MAX_VALUE) continue;
                
                int nuevaDistancia = distancias[x][y] + costo;
                
                if (nuevaDistancia < distancias[nx][ny]) {
                    distancias[nx][ny] = nuevaDistancia;
                    padre[nx][ny] = x * TAMANIO + y;
                    cola.offer(new Nodo(nx, ny, nuevaDistancia));
                }
            }
        }
        
        return null;
    }
    
    /**
     * Calcula el costo de moverse a una posición
     * Considera muros (infinito) y proximidad a agentes (penalización)
     */
    private int calcularCosto(int x, int y) {
        for (Muro muro : muros) {
            if (muro.getPosX() == x && muro.getPosY() == y) {
                return Integer.MAX_VALUE;
            }
        }
        
        int costo = 1;
        

        for (Agente agente : agentes) {
            if (!agente.isVivo()) continue;
            
            int distAgente = Math.abs(x - agente.getPosX()) + 
                        Math.abs(y - agente.getPosY());
            
            if (distAgente == 0) {
                return Integer.MAX_VALUE; 
            } else if (distAgente == 1) {
                costo += 50;
            } else if (distAgente == 2) {
                costo += 10;
            }
        }
        
        return costo;
    }
    
    /**
     * Reconstruye el primer paso del camino óptimo
     */
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
    
    private class Nodo implements Comparable<Nodo> {
        int x, y, distancia;
        
        Nodo(int x, int y, int distancia) {
            this.x = x;
            this.y = y;
            this.distancia = distancia;
        }
        
        @Override
        public int compareTo(Nodo otro) {
            return Integer.compare(this.distancia, otro.distancia);
        }
    }
}
