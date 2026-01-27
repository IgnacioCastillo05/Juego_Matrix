package matrix;

public abstract class Persona implements Comportamiento, Runnable {
    protected int posX;
    protected int posY;
    protected char simbolo;
    protected boolean vivo; // Esto es para saber si el personaje sigue activo
    protected String nombre;
    
    public Persona(int posX, int posY, char simbolo, String nombre) {
        this.posX = posX;
        this.posY = posY;
        this.simbolo = simbolo;
        this.vivo = true;
        this.nombre = nombre;
    }
    
    // Getters
    public int getPosX() {
        return posX;
    }
    
    public int getPosY() {
        return posY;
    }
    
    public char getSimbolo() {
        return simbolo;
    }
    
    public boolean isVivo() {
        return vivo;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    // Setters
    public void setPosX(int posX) {
        this.posX = posX;
    }
    
    public void setPosY(int posY) {
        this.posY = posY;
    }
    
    public void setVivo(boolean vivo) {
        this.vivo = vivo;
    }
    
    /**
     * Método para colocar al personaje en el tablero
     */
    public void colocarEnTablero(char[][] tablero) {
        if (vivo) {
            tablero[posX][posY] = this.simbolo;
        }
    }
    
    /**
     * Verifica si una posición está dentro de los límites del tablero
     */
    protected boolean posicionValida(int x, int y, int tamaño) {
        return x >= 0 && x < tamaño && y >= 0 && y < tamaño;
    }
    
    /**
     * Método abstracto que cada clase hija implementará
     * Este método contendrá la lógica del hilo
     */
    @Override
    public abstract void run();
    
    /**
     * Método de la interfaz Comportamiento que cada clase implementará
     */
    @Override
    public abstract boolean movimiento(char[][] tablero);
}
