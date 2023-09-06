package io.agora.ktv.manager;

import android.app.Activity;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import org.apache.commons.codec.binary.Base64;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import io.agora.baselibrary.util.ToastUtils;


public class IdentifyProtocolV1 {
    private String encodeBase64(byte[] bstr) {
        Base64 base64 = new Base64();
        return new String(base64.encode(bstr));
    }

    private String encryptByHMACSHA1(byte[] data, byte[] key) {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(key, "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(data);
            return encodeBase64(rawHmac);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private String getUTCTimeSeconds() {
        Calendar cal = Calendar.getInstance();
        int zoneOffset = cal.get(Calendar.ZONE_OFFSET);
        int dstOffset = cal.get(Calendar.DST_OFFSET);
        cal.add(Calendar.MILLISECOND, -(zoneOffset + dstOffset));
        return cal.getTimeInMillis() / 1000 + "";
    }

    private String postHttp(String posturl, Map<String, Object> params, int timeOut) {
        String res = "";
        String BOUNDARYSTR = "*****2015.03.30.acrcloud.rec.copyright." + System.currentTimeMillis() + "*****";
        String BOUNDARY = "--" + BOUNDARYSTR + "\r\n";
        String ENDBOUNDARY = "--" + BOUNDARYSTR + "--\r\n\r\n";

        String stringKeyHeader = BOUNDARY +
                "Content-Disposition: form-data; name=\"%s\"" +
                "\r\n\r\n%s\r\n";
        String filePartHeader = BOUNDARY +
                "Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"\r\n" +
                "Content-Type: application/octet-stream\r\n\r\n";

        URL url = null;
        HttpURLConnection conn = null;
        BufferedOutputStream out = null;
        BufferedReader reader = null;
        ByteArrayOutputStream postBufferStream = new ByteArrayOutputStream();
        try {
            for (String key : params.keySet()) {
                Object value = params.get(key);
                if (value instanceof String || value instanceof Integer) {
                    postBufferStream.write(String.format(stringKeyHeader, key, (String) value).getBytes());
                } else if (value instanceof byte[]) {
                    postBufferStream.write(String.format(filePartHeader, key, key).getBytes());
                    postBufferStream.write((byte[]) value);
                    postBufferStream.write("\r\n".getBytes());
                }
            }
            postBufferStream.write(ENDBOUNDARY.getBytes());

            url = new URL(posturl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(timeOut);
            conn.setReadTimeout(timeOut);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Accept-Charset", "utf-8");
            conn.setRequestProperty("Content-type", "multipart/form-data;boundary=" + BOUNDARYSTR);

            conn.connect();
            out = new BufferedOutputStream(conn.getOutputStream());
            out.write(postBufferStream.toByteArray());
            out.flush();
            int response = conn.getResponseCode();
            if (response == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                String tmpRes = "";
                while ((tmpRes = reader.readLine()) != null) {
                    if (tmpRes.length() > 0)
                        res = res + tmpRes;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (postBufferStream != null) {
                    postBufferStream.close();
                    postBufferStream = null;
                }
                if (out != null) {
                    out.close();
                    out = null;
                }
                if (reader != null) {
                    reader.close();
                    reader = null;
                }
                if (conn != null) {
                    conn.disconnect();
                    conn = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return res;
    }

    public String recognize(String host, String accessKey, String secretKey, byte[] queryData, String title, String queryType, int timeout) {
        String method = "POST";
        String httpURL = "/v1/identify";
        String dataType = queryType;
        String sigVersion = "1";
        String timestamp = getUTCTimeSeconds();

        String reqURL = "http://" + host + httpURL;

        String sigStr = method + "\n" + httpURL + "\n" + accessKey + "\n" + dataType + "\n" + sigVersion + "\n" + timestamp;
        String signature = encryptByHMACSHA1(sigStr.getBytes(), secretKey.getBytes());

        Map<String, Object> postParams = new HashMap<String, Object>();
        postParams.put("access_key", accessKey);
        postParams.put("sample_bytes", queryData.length + "");
        postParams.put("sample", queryData);
        postParams.put("timestamp", timestamp);
        postParams.put("signature", signature);
        postParams.put("data_type", queryType);
        postParams.put("title", title);
        postParams.put("signature_version", sigVersion);

        return postHttp(reqURL, postParams, timeout);
    }

    public void reg(Activity activity, byte[] buffers, String title) {
//        File file = new File(context.getExternalCacheDir() + "/query0017.wav");
//        byte[] buffer = new byte[1024 * 1024];
//        if (!file.exists()) {
//            return;
//        }
//        FileInputStream fin = null;
//        int bufferLen = 0;
//        try {
//            fin = new FileInputStream(file);
//            bufferLen = fin.read(buffer, 0, buffer.length);
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                if (fin != null) {
//                    fin.close();
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        Log.i("Test", "reg result:bufferLen=" + bufferLen);
//
//        if (bufferLen <= 0)
//            return;
//
//        byte[] postDatas = new byte[bufferLen];
//        System.arraycopy(buffer, 0, postDatas, 0, bufferLen);

        try {
            int pcmSize = buffers.length;
            WaveHeader mHeader = new WaveHeader(pcmSize);
            //长度字段 = 内容的大小（PCMSize) + 头部字段的大小(不包括前面4字节的标识符RIFF以及fileLength本身的4字节)
            mHeader.fileLength = pcmSize + (44 - 8);
            mHeader.FmtHdrLeth = 16;
            mHeader.BitsPerSample = 16;
            mHeader.Channels = 1;
            mHeader.FormatTag = 0x0001;
            mHeader.SamplesPerSec = 16000;
            mHeader.BlockAlign = (short) (mHeader.Channels * mHeader.BitsPerSample / 8);
            mHeader.AvgBytesPerSec = mHeader.BlockAlign * mHeader.SamplesPerSec;
            mHeader.DataHdrLeth = pcmSize;


            byte[] h = mHeader.getHeader();

            byte[] allData = new byte[h.length + buffers.length];
            System.arraycopy(h, 0, allData, 0, h.length);
            System.arraycopy(buffers, 0, allData, h.length, buffers.length);

            Log.i("Test", "reg result  title:" + title);
            String result = recognize("identify-cn-north-1.acrcloud.cn", "64f2d859960afaa368c3c749ded065b9", "TCo4VMq1Kdb74mKd9vkS2oQxm4SNS1twzQw4sfLh", allData, title, "audio", 10000);
            Log.i("Test", "reg result:" + result);

            JSONObject jsonObject = JSON.parseObject(result);
            JSONObject statusJson = jsonObject.getJSONObject("status");
            final int code = statusJson.getIntValue("code");
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (code == 0) {
                        ToastUtils.toastShort(activity, "你太棒了");
                    }
                }
            });


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
