package src.types;

public enum DrawMode {
    Antoine256, AntoineShift256,
    Adjacency1_256, Adjacency2_256,
    GridLines256, GridLines128, GridLines64, GridLines32,
    Landscape32,
    Blockies128,
    Random128,
    Fourier128,
    Fourier256,
    FourierCartesian128,
    FourierCartesian256;


    public int length() {
        return Integer.parseInt(toString().substring(toString().length() - 3));
    }


}
