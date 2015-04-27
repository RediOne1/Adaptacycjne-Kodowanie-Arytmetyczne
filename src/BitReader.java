import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * authot:  Adrian Kuta
 * index:   204423
 * date:    28.03.15
 */
public class BitReader {
    private int offset = 0, byteNumber = 0;

    private RandomAccessFile aFile;
    private FileChannel inChannel;
    private ByteBuffer buffer;
    byte symbol;

    public BitReader(String filePath){
        try{
            aFile = new RandomAccessFile(filePath, "r");
            inChannel = aFile.getChannel();
            buffer = ByteBuffer.allocate(1024);

            inChannel.read(buffer);
            buffer.flip();
            symbol = buffer.get();
        }catch(Exception e) { e.printStackTrace(); }
    }

    public int ReadBit(){
        int result = 0;
        if(byteNumber == buffer.limit() && byteNumber < 1024){
            //System.out.println(byteNumber);
            //System.out.println(buffer.limit());
            return -1;
        }
        if (offset == 8) {
            byteNumber++;
            offset = 0;
            if (byteNumber == buffer.limit()) {
                if(byteNumber == 1024){
                    try{
                        buffer.clear();
                        if(inChannel.read(buffer) > 0){
                            buffer.flip();
                            symbol = buffer.get();
                            byteNumber = 0;
                            offset = 0;
                        } else return -1;
                    }catch(Exception e){ e.printStackTrace();}
                } else {
                    return -1;
                }
            } else {
                symbol = buffer.get();
                offset = 0;
            }
        }
        result = (symbol & (1 << (7 - offset))) >> (7 - offset);
        offset++;
        return result;
    }

    public int ReadByte(){
        int result = 0, resultOffset = 0;

        for(int i = 0; i < 8; i++){
            int bit = this.ReadBit();
            if(bit == 1){
                result = result | (1 << (7 - resultOffset));
                resultOffset++;
            } else if(bit == 0){
                resultOffset++;
            } else {
                return result;
            }
        }
        return result;
    }
}
