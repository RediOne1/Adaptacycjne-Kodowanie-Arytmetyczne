import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class Lista4 {

    public static final long MASK_32BIT = 0x00000000ffffffffL;
    public static final int MASK_8BIT = 0xff;
    public static long numOfCodes, totalLengthOfCodes;
    public static long totalCount = 0x00000000ffffffffL;
    public static long totalNumberOfCharacters = 256;

    public static long[] symbols = new long[256];
    public static long[] CumCount = new long[256];
    public static long lower, upper, previousLow, range, Length, scale3;

    public static void main(String args[]) {
        encodeFile("test.txt", "test");
    }

    public static void encodeFile(String inputFileName, String outputFileName) {
        try {
            RandomAccessFile aFile = new RandomAccessFile(inputFileName, "r");
            FileChannel inChannel = aFile.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            BitWriter bw = new BitWriter(outputFileName);

            setUpInitialCount();
            lower = 0;
            upper = totalCount;

            while (inChannel.read(buffer) > 0) {
                buffer.flip();
                for (int i = 0; i < buffer.limit(); i++) {
                    byte symbol = buffer.get();
                    numOfCodes++;

                    encodeSymbol(symbol, bw);
                }
                buffer.clear();
            }
            encodeSymbol((byte) 255, bw);
            bw.finish();

            File inFile = new File(inputFileName);
            File outFile = new File(outputFileName);
            System.out.println();
            System.out.println("Stopien kompresji: " + (1 - ((double) outFile.length() / (double) inFile.length())) * 100 + "%");
            System.out.println("Średnia długość słowa: " + (double) totalLengthOfCodes / (double) numOfCodes);
            System.out.println("Entropy: \t\t\t" + calculateEntropy());
            inChannel.close();
            aFile.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setUpInitialCount() {
        initializeSymbolTable();
        calculateCumCount();
    }

    public static void encodeSymbol(byte symbol, BitWriter bw) {
        Length = totalNumberOfCharacters;

        previousLow = lower;
        range = upper - lower + 1;
        lower = previousLow + (range * CumCount((symbol & 0xFF) - 1) / Length);
        upper = previousLow + range * CumCount(symbol & 0xFF) / Length - 1;

        while (MSB(lower) == MSB(upper) ||
                (secondMSB(lower) == 1 && secondMSB(upper) == 0)) {
            if (MSB(lower) == MSB(upper)) {
                int bit = MSB(upper);
                bw.writeBit(bit);
                totalLengthOfCodes++;
                lower = (lower << 1) & MASK_32BIT;
                upper = ((upper << 1) & MASK_32BIT) + 1;

                while (scale3 > 0) {
                    bw.writeBit(1 - bit);
                    totalLengthOfCodes++;
                    scale3 -= 1;
                }
            } else if (secondMSB(lower) == 1 && secondMSB(upper) == 0) {
                lower = (lower << 1) & MASK_32BIT;
                upper = ((upper << 1) & MASK_32BIT) + 1;

                lower = negateMSB(lower);
                upper = negateMSB(upper);

                scale3 += 1;
            }
        }
        symbols[symbol & MASK_8BIT] += 1;
        totalNumberOfCharacters += 1;
        calculateCumCount();
    }

    public static void calculateCumCount() {
        CumCount[0] = symbols[0];
        for (int i = 1; i < CumCount.length; i++) {
            CumCount[i] = CumCount[i - 1] + symbols[i];
        }
    }

    public static long CumCount(int x) {
        if (x < 0) {
            return 0;
        } else {
            return CumCount[x & 0xFF];
        }
    }

    public static double calculateEntropy() {
        double sum = 0;
        double mariaInformacji;
        double probability;
        for (int i = 0; i < 256; i++) {
            if (symbols[i] > 1) {
                probability = (double) symbols[i] / (double) totalNumberOfCharacters;
                mariaInformacji = Math.log(1 / probability) / Math.log(2);
                sum += probability * mariaInformacji;
            }
        }
        return sum;
    }

    public static int MSB(long value) {
        return Math.abs((int) value >> 31);
    }

    public static int secondMSB(long value) {
        return ((int) value & 0x7FFFFFFF) >> 30;
    }

    public static void initializeSymbolTable() {
        for (int i = 0; i < symbols.length; i++) {
            symbols[i] = 1;
        }
    }

    public static long negateMSB(long value) {
        if ((value & 0x0000000080000000L) > 0) {
            return value & 0x000000007FFFFFFFL;
        } else {
            return value | 0x0000000080000000L;
        }
    }
}
