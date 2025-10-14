package tr.com.logidex.cad.processor;
public final class PieceSequenceNumberCreator {

    private static int sequenceNumber=1;

    private PieceSequenceNumberCreator() {}


    public static int getSequenceNumber() {
        return sequenceNumber++;
    }
}
