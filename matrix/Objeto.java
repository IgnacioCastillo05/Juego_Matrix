package matrix;

public abstract class Objeto {
    protected int posX;
    protected int posY;
    protected char simbolo;
    
    public Objeto(int posX, int posY, char simbolo) {
        this.posX = posX;
        this.posY = posY;
        this.simbolo = simbolo;
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
    
    // Setters
    public void setPosX(int posX) {
        this.posX = posX;
    }
    
    public void setPosY(int posY) {
        this.posY = posY;
    }
    
    /**
     * Método abstracto para representar el objeto en el tablero
     */
    public abstract void colocarEnTablero(char[][] tablero);
}
