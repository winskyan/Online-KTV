package io.agora.ktv.manager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

//public class PcmToWave {
//
//    /**
//     *
//     * @param src
//     * src[0]指定pcm文件位置，src[1]指定输出的wav文件存放位置
//     * @throws Exception
//     */
//    public static void convertAudioFiles(String[] src) throws Exception {
//        FileInputStream fis = new FileInputStream(src[0]);
//
//        //获取PCM文件大小
//        File file=new File(src[0]);
//        int PCMSize =(int) file.length();
//
//        //定义wav文件头
//        //填入参数，比特率等等。这里用的是16位单声道 8000 hz
//        WaveHeader header = new WaveHeader(PCMSize);
//        //长度字段 = 内容的大小（PCMSize) + 头部字段的大小(不包括前面4字节的标识符RIFF以及fileLength本身的4字节)
//        header.fileLength = PCMSize + (44 - 8);
//        header.FmtHdrLeth = 16;
//        header.BitsPerSample = 16;
//        header.Channels = 1;
//        header.FormatTag = 0x0001;
//        header.SamplesPerSec = 44100;//8000;
//        header.BlockAlign = (short)(header.Channels * header.BitsPerSample / 8);
//        header.AvgBytesPerSec = header.BlockAlign * header.SamplesPerSec;
//        header.DataHdrLeth = PCMSize;
//
//        //获取wav文件头字节数组
//        byte[] h = header.getHeader();
//
//        assert h.length == 44; //WAV标准，头部应该是44字节
//        System.out.println((PCMSize+44));
////		   auline.write(h, 0, h.length);
//
//        byte[] b = new byte[10];
//
//        //将文件头写入文件
//        FileOutputStream fs = new FileOutputStream(src[1]);
//        fs.write(h);
//        //将pcm文件写到文件头后面
//        FileInputStream fiss = new FileInputStream(src[0]);
//        byte[] bb = new byte[10];
//        int len = -1;
//        while((len = fiss.read(bb))>0) {
//
//            fs.write(bb, 0, len);
//        }
//
//    }
//}


/**
 * WavHeader辅助类。用于生成头部信息。
 * @author Administrator
 *
 */
public class WaveHeader {

    /**wav文件头：RIFF区块
     *	名称		偏移地址	字节数	端序	内容
     * 	ID		0x00	4Byte	大端	'RIFF' (0x52494646)
     Size	0x04	4Byte	小端	fileSize - 8
     Type	0x08	4Byte	大端	'WAVE'(0x57415645)
     解析：
     以'RIFF'为标识
     Size是整个文件的长度减去ID和Size的长度
     Type是WAVE表示后面需要两个子块：Format区块和Data区块
     */
    /**
     * FORMAT区块：
     * 	名称				偏移地址	字节数	端序	内容
     ID				0x00	4Byte	大端	'fmt ' (0x666D7420)
     Size			0x04	4Byte	小端	16
     AudioFormat		0x08	2Byte	小端	音频格式
     NumChannels		0x0A	2Byte	小端	声道数
     SampleRate		0x0C	4Byte	小端	采样率
     ByteRate		0x10	4Byte	小端	每秒数据字节数
     BlockAlign		0x14	2Byte	小端	数据块对齐
     BitsPerSample	0x16	2Byte	小端	采样位数
     解析：
     以'fmt '为标识
     Size表示该区块数据的长度（不包含ID和Size的长度）
     AudioFormat表示Data区块存储的音频数据的格式，PCM音频数据的值为1
     NumChannels表示音频数据的声道数，1：单声道，2：双声道
     SampleRate表示音频数据的采样率
     ByteRate每秒数据字节数 = SampleRate * NumChannels * BitsPerSample / 8
     BlockAlign每个采样所需的字节数 = NumChannels * BitsPerSample / 8
     BitsPerSample每个采样存储的bit数，8：8bit，16：16bit，32：32bit
     */
    /**
     * DATA区块
     *
     * 名称		偏移地址	字节数	端序	内容
     ID		0x00	4Byte	大端	'data' (0x64617461)
     Size	0x04	4Byte	小端	N
     Data	0x08	NByte	小端	音频数据
     解析：
     以'data'为标识
     Size表示音频数据的长度，N = ByteRate * seconds
     Data音频数据

     */

    public final char fileID[] = {'R', 'I', 'F', 'F'};
    public int fileLength;
    public short FormatTag;
    public short Channels;
    public int SamplesPerSec;
    public int AvgBytesPerSec;
    public short BlockAlign;
    public short BitsPerSample;
    public char DataHdrID[] = {'d','a','t','a'};
    public int DataHdrLeth;
    public char wavTag[] = {'W', 'A', 'V', 'E'};;
    public char FmtHdrID[] = {'f', 'm', 't', ' '};
    public int FmtHdrLeth;

    public WaveHeader() {}//无参构造方法
    /**
     *
     * @param a
     */
    public WaveHeader(int a) {

    }

    public byte[] getHeader() throws IOException {
        //创建一个输出流，用于将各个字节数组写入缓存中，缓存区会自动增长。然后可以将整个输出流转换为完整的字节数组，关闭该流不会有任何效果。
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        WriteChar(bos, fileID);
        WriteInt(bos, fileLength);
        WriteChar(bos, wavTag);
        WriteChar(bos, FmtHdrID);
        WriteInt(bos,FmtHdrLeth);
        WriteShort(bos,FormatTag);
        WriteShort(bos,Channels);
        WriteInt(bos,SamplesPerSec);
        WriteInt(bos,AvgBytesPerSec);
        WriteShort(bos,BlockAlign);
        WriteShort(bos,BitsPerSample);
        WriteChar(bos,DataHdrID);
        WriteInt(bos,DataHdrLeth);
        bos.flush();
        byte[] r = bos.toByteArray();
        bos.close();
        return r;
    }

    private void WriteShort(ByteArrayOutputStream bos, int s) throws IOException {
        byte[] mybyte = new byte[2];
        mybyte[1] =(byte)( (s << 16) >> 24 );//存放高位
        mybyte[0] =(byte)( (s << 24) >> 24 );//存放低位
        bos.write(mybyte);
    }


    private void WriteInt(ByteArrayOutputStream bos, int n) throws IOException {
        byte[] buf = new byte[4];
        buf[3] =(byte)( n >> 24 );
        buf[2] =(byte)( (n << 8) >> 24 );
        buf[1] =(byte)( (n << 16) >> 24 );
        buf[0] =(byte)( (n << 24) >> 24 );
        bos.write(buf);
    }

    private void WriteChar(ByteArrayOutputStream bos, char[] id) {
        for (int i=0; i<id.length; i++) {
            char c = id[i];
            bos.write(c);
        }
    }
}

