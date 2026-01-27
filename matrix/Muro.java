package matrix;

public class Muro extends Objeto {
    
    public Muro(int posX, int posY) {
        super(posX, posY, 'M');
    }
    
    @Override
    public void colocarEnTablero(char[][] tablero) {
        tablero[posX][posY] = this.simbolo;
    }
    
    @Override
    public String toString() {
        return "Muro en posición (" + posX + ", " + posY + ")";
    }
}
