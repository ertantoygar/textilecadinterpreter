package tr.com.logidex.cad;
public final class PieceSequenceNumberCreator {

    private static int sequenceNumber=1;

    private PieceSequenceNumberCreator() {}


    public static int getSequenceNumber() {
        return sequenceNumber++;
    }

    public static void resetCounter() {
        sequenceNumber=1;
    }
}
