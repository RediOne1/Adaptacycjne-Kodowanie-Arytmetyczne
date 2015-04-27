import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class Lista4 {
    public static final boolean DEBUG_SYMBOL = false;
    public static final boolean DEBUG_BEFORE = false;
    public static final boolean DEBUG_DURING = false;
    public static final boolean DEBUG_AFTER = false;
    public static long numOfCodes, totalLengthOfCodes;
    public static long TotalCount = 0x00000000ffffffffL;
    public static final long MASK_32BIT = 0x00000000ffffffffL;
    public static final int MASK_8BIT = 0xff;
    public static long totalNumberOfCharacters = 256;

    public static long[] symbols = new long[256];
    public static long[] CumCount = new long[256];
    public static long Low, High, previousLow, range, Length, scale3;

    public static void main(String args[]) {
        if (args.length != 2)
            System.out.println("Podaj nazwe pliku input i output.");
        else {
            encodeFile(args[0], args[1]);
        }
    }

    public static void encodeFile(String inputFileName, String outputFileName){
        try{
            RandomAccessFile aFile = new RandomAccessFile(inputFileName, "r");
            FileChannel inChannel = aFile.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            BitWriter bw = new BitWriter(outputFileName);

            setUpInitialCount();
            Low = 0;
            High = TotalCount;

            while(inChannel.read(buffer) > 0){
                buffer.flip();
                for (int i = 0; i < buffer.limit(); i++){
                    byte symbol = buffer.get();
                    numOfCodes++;

                    encodeSymbol(symbol, bw);
                }
                buffer.clear();
            }
            encodeSymbol((byte)255, bw);
            bw.finish();

            File inFile = new File(inputFileName);
            File outFile = new File(outputFileName);
            System.out.println();
            System.out.println("Stopien kompresji: "+ (1 - ((double)outFile.length() / (double)inFile.length()))*100+"%");
            System.out.println("Average Length of Codes: "+(double)totalLengthOfCodes/(double)numOfCodes);
            System.out.println("Entropy: \t\t\t" + calculateEntropy());
            inChannel.close();
            aFile.close();

        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void setUpInitialCount(){
        initializeSymbolTable();
        calculateCumCount();
    }

    public static void encodeSymbol(byte symbol, BitWriter bw){
        Length = totalNumberOfCharacters;
        if(DEBUG_SYMBOL) System.out.println("Symbol: "+(char) symbol);

        previousLow = Low;
        range = High - Low + 1;
        Low = previousLow + (range * CumCount((symbol & 0xFF) - 1)/Length);
        High = previousLow + range * CumCount(symbol & 0xFF)/Length - 1;

        if(DEBUG_BEFORE){
            System.out.print("Przed: ");
            System.out.println("low: "+Low+" high: "+High);
        }

        while(  MSB(Low) == MSB(High) ||
                (secondMSB(Low) == 1 && secondMSB(High) == 0)){
            if(MSB(Low) == MSB(High)){
                int bit = MSB(High);
                bw.writeBit(bit);
                totalLengthOfCodes++;
                Low = (Low << 1) & MASK_32BIT;
                High = ((High << 1) & MASK_32BIT) + 1;

                if(DEBUG_DURING){
                    System.out.print("W trakcie E1/2: ");
                    System.out.println("low: "+Low+" high: "+High);
                }

                while(scale3 > 0){
                    bw.writeBit(1 - bit);
                    totalLengthOfCodes++;
                    scale3 -= 1;
                }
            } else if(secondMSB(Low) == 1 && secondMSB(High) == 0){
                Low = (Low << 1) & MASK_32BIT;
                High = ((High << 1) & MASK_32BIT) + 1;

                if(DEBUG_DURING){
                    System.out.print("W trakcie E3: ");
                    System.out.println("low: "+Low+" high: "+High);
                }

                Low = negateMSB(Low);
                High = negateMSB(High);

                scale3 += 1;
            }
        }
        symbols[symbol & MASK_8BIT] += 1;
        totalNumberOfCharacters += 1;
        calculateCumCount();
        if(DEBUG_AFTER) {
            System.out.print("Po: ");
            System.out.println("low: " + Low + " high: " + High);
            System.out.println("symbol: " + symbol + " totalNumOfChars: " + totalNumberOfCharacters);
        }
    }
    public static void calculateCumCount(){
        CumCount[0] = symbols[0];
        for(int i = 1; i < CumCount.length; i++){
            CumCount[i] = CumCount[i-1]+ symbols[i];
        }
    }

    public static long CumCount(int x){
        if(x < 0) {
            return 0;
        } else {
            return CumCount[x & 0xFF];
        }
    }

    public static double calculateEntropy(){
        double sum = 0;
        double mariaInformacji;
        double probability;
        for (int i = 0; i < 256; i++){
            if(symbols[i] > 1){
                probability = (double) symbols[i] / (double) totalNumberOfCharacters;
                mariaInformacji = Math.log(1/probability)/Math.log(2);
                sum += probability*mariaInformacji;
            }
        }
        return sum;
    }

    public static int MSB(long value){
        return Math.abs((int)value >> 31);
    }

    public static int secondMSB(long value){
        return ((int)value & 0x7FFFFFFF) >> 30;
    }

    public static void initializeSymbolTable(){
        for(int i = 0; i < symbols.length; i++){
            symbols[i] = 1;
        }
    }

    public static long negateMSB(long value){
        if((value & 0x0000000080000000L) > 0){
            return value & 0x000000007FFFFFFFL;
        } else {
            return value | 0x0000000080000000L;
        }
    }
}
