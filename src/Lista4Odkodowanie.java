import java.io.FileOutputStream;

public class Lista4Odkodowanie {

    public static long[] symbols = new long[256];
    public static long[] CumCount = new long[256];
    public static long totalCount = 0x00000000FFFFFFFFL;
    public static long lower, upper, tag;
    public static long totalNumberOfCharacters = 256;

    public static void main(String args[]) {
        if (args.length != 2)
            System.out.println("Podaj nazwe pliku input i output.");
        else {
            decodeFile(args[0], args[1]);
        }
    }

    public static void decodeFile(String inputFile, String outputFile){
        byte[] bufferForFileWrite = new byte[1024];
        int byteNumber = 0;
        long previousLow, Length, range;
        byte symbol;
        try{
            FileOutputStream outFile = new FileOutputStream(outputFile);
            setUpInitialCount();
            lower = 0;
            upper = totalCount;
            BitReader br = new BitReader(inputFile);
            tag = read32Bits(br);
            symbol = findSymbol(tag, lower, upper);
            while(symbol != 255){
                if(tag == 0) System.exit(0);

                if(byteNumber == bufferForFileWrite.length) {
                    outFile.write(bufferForFileWrite);
                    bufferForFileWrite = new byte[1024];
                    byteNumber = 0;
                }

                bufferForFileWrite[byteNumber] = symbol;
                byteNumber++;

                Length = totalNumberOfCharacters;

                previousLow = lower;
                range = upper - lower + 1;
                lower = previousLow + (range * CumCount((symbol & 0xFF) - 1)/Length);
                upper = previousLow + ((range * CumCount(symbol & 0xFF))/Length) - 1;


                while(  (MZB(lower) == MZB(upper) ||
                        (secondMZB(lower) == 1 && secondMZB(upper) == 0))){
                    if(MZB(lower) == MZB(upper)){
                        lower = (lower << 1) & totalCount;
                        upper = ((upper << 1) & totalCount) + 1;

                        tag = readNextBit(tag, br);
                        if(tag == 0) {
                            outFile.write(bufferForFileWrite, 0, byteNumber-1);
                            System.exit(0);
                        }

                    } else if(secondMZB(lower) == 1 && secondMZB(upper) == 0){
                        lower = (lower << 1) & totalCount;
                        upper = ((upper << 1) & totalCount) + 1;
                        tag = readNextBit(tag, br);
                        if(tag == 0) {
                            outFile.write(bufferForFileWrite, 0, byteNumber - 1);
                            System.exit(0);
                        }

                        lower = negateMZB(lower);
                        upper = negateMZB(upper);
                        tag = negateMZB(tag);
                    }
                }

                symbols[symbol & 0xFF] += 1;
                totalNumberOfCharacters += 1;
                calculateCumCount();

                symbol = findSymbol(tag, lower, upper);
            }
            outFile.write(bufferForFileWrite, 0, byteNumber);

        } catch(Exception e){ e.printStackTrace(); }
    }

    public static void setUpInitialCount(){
        initializeSymbolTable();
        calculateCumCount();
    }

    public static void calculateCumCount(){
        CumCount[0] = symbols[0];
        for(int i = 1; i < CumCount.length; i++){
            CumCount[i] = CumCount[i-1]+ symbols[i];
        }
    }

    public static byte findSymbol(long tag, long Low, long High){
        long range = High - Low + 1;
        int k = 0;
        while(((tag - Low + 1) * totalNumberOfCharacters - 1)/range >= CumCount(k)){
            k++;
        }
        return (byte)(k & 0xFF);
    }

    public static long read32Bits(BitReader br){
        int bit = br.ReadBit();
        long tag = bit;

        for(int i = 1; i < 32; i++){
            bit = br.ReadBit();
            tag = (tag << 1) + bit;
        }
        return tag;
    }

    public static long readNextBit(long tag, BitReader br){
        long bit = br.ReadBit();
        if(bit == -1)   {
            return (tag << 1) & totalCount;
        }
        else            return ((tag << 1) & totalCount) + bit;
    }

    public static long CumCount(int x){
        if(x < 0) {
            return 0;
        } else {
            return CumCount[x & 0xFF];
        }
    }

    public static int MZB(long value){
        return Math.abs((int)value >> 31);
    }

    public static int secondMZB(long value){
        return ((int)value & 0x7FFFFFFF) >> 30;
    }

    public static void initializeSymbolTable(){
        for(int i = 0; i < symbols.length; i++){
            symbols[i] = 1;
        }
    }

    public static long negateMZB(long value){
        if((value & 0x0000000080000000L) > 0){
            return value & 0x000000007FFFFFFFL;
        } else {
            return value | 0x0000000080000000L;
        }
    }


}
