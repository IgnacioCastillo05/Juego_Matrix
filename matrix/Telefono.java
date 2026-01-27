package matrix;

public class Telefono extends Objeto {
    private boolean usado; // Esto es para saber si Neo ya llegó a este teléfono
    
    public Telefono(int posX, int posY) {
        super(posX, posY, 'T');
        this.usado = false;
    }
    
    @Override
    public void colocarEnTablero(char[][] tablero) {
        if (!usado) {
            tablero[posX][posY] = this.simbolo;
        }
    }
    
    public boolean isUsado() {
        return usado;
    }
    
    public void setUsado(boolean usado) {
        this.usado = usado;
    }
    
    /**
     * Calcula la distancia Manhattan hasta una posición dada
     * Útil para que Neo encuentre el teléfono más cercano
     */
    public int distanciaHasta(int x, int y) {
        return Math.abs(this.posX - x) + Math.abs(this.posY - y);
    }
    
    @Override
    public String toString() {
        return "Teléfono en posición (" + posX + ", " + posY + ")" + 
               (usado ? " [USADO]" : "");
    }
}
