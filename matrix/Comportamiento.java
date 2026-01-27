package matrix;
public interface Comportamiento {
    /**
     * Método abstracto que define el movimiento de un personaje
     * @param tablero El tablero actual del juego
     * @return true si se pudo mover, false si no
     */
    boolean movimiento(char[][] tablero);
}