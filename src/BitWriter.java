import java.io.FileOutputStream;

/**
 * authot:  Adrian Kuta
 * index:   204423
 * date:    30.03.15
 */
public class BitWriter {
    private static int offset = 0, byteNumber = 0;
    FileOutputStream outFile;
    byte[] bufferForFileWrite = new byte[1024];

    public BitWriter(String filePath){
        try{
            outFile = new FileOutputStream(filePath);
        }catch(Exception e) { e.printStackTrace(); }
    }

    public void writeBit(int bit){
        try{
            //System.out.print(bit);
            //for (int j = 0; j < value.length(); j++) {
                if (offset == 8) {
                    byteNumber++;
                    offset = 0;
                }
                if (byteNumber == bufferForFileWrite.length) {
                    outFile.write(bufferForFileWrite);
                    bufferForFileWrite = new byte[1024];
                    byteNumber = 0;
                }
                if (bit == 1) {
                    bufferForFileWrite[byteNumber] = (byte)(bufferForFileWrite[byteNumber] |
                            (byte) (1 << (7 - offset)));
                    offset++;
                } else if (bit == 0){
                    offset++;
                }
           // }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void finish(){
        try{
            if(offset > 0){
                outFile.write(bufferForFileWrite, 0, byteNumber+1);
            } else {
                outFile.write(bufferForFileWrite, 0, byteNumber);
            }

        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
