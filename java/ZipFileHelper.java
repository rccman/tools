package com.hzfh.common.utils.helper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * com.hzfh.common.utils.helper
 * 压缩生成zip文件
 * @Project: ideawork
 * @Author: rencc
 * @Description:
 * @Date: 2018/1/17 15:15
 * @Source: Created with IntelliJ IDEA.
 */
public class ZipFileHelper {
    public static int fileCount;

    /**
     * 附件批量打包下载
     * @param request
     * @param response
     * @param list —> list 类型是List<Map<String, String>> 其中每个map需要包含 url:下载文件的url， fileName:真正的文件名不是uuid
     */
    public static void zipPackageDownload(HttpServletRequest request, HttpServletResponse response, List<Map<String, String>> list) {
        try {
            //获取项目中文件的文件夹
            String basePath = request.getSession().getServletContext().getRealPath(System.getProperty("file.separator"))+System.getProperty("file.separator");
            //定义文件list
            List<File> files = new ArrayList<File>();
            //定义zip文件名
            String fileName = "华镇金控私募系统附件包" + ".zip";
            //定义临时单个文件的list，方便后面删除
            List<String> tempUrlList = new ArrayList<String>();
            //将文件从https上下载进服务器的目录，用files装好
            for (Map<String, String> map : list) {
                String url = map.get("url");
                URL u = new URL(url);
//                String tempUrl = basePath + System.getProperty("file.separator") + url.substring(url.lastIndexOf("/")+1);
                String tempUrl = basePath + System.getProperty("file.separator") + map.get("fileName");
                File f = new File(tempUrl);
                fileCount = 1;
                f = creatFile(f,map.get("fileName"));
                InputStream ins = u.openStream();
                OutputStream os = new FileOutputStream(f);
                int bytesRead = 0;
                byte[] buffer = new byte[2048];
                while ((bytesRead = ins.read(buffer, 0, 2048)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.close();
                ins.close();
                files.add(f);
                tempUrlList.add(f.getPath());
            }
            //创建zip文件
            ZipFileHelper.createFile(basePath, fileName);
            File file = new File(basePath + fileName);
            FileOutputStream outStream = new FileOutputStream(file);
            ZipOutputStream toClient = new ZipOutputStream(outStream);
            //将files打包成zip文件
            ZipFileHelper.zipFile(files, toClient);
            toClient.close();
            outStream.close();
            //下载zip文件，并删除服务器源文件
            ZipFileHelper.downloadFile(file, response,request, true);
            //删除服务器临时的单个文件
            System.out.println("删除批量下载创建的临时文件");
            for(String t : tempUrlList){
                DeleteFileHelper.delete(t);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 递归创建文件，防止重名文件处理
    * @param f
     * @param fileName
     * @author rencc
     * @return
     */
    private static File creatFile(File f, String fileName) {
        if(!f.exists()){
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            String path = f.getPath();
            fileName = fileName.substring(0,fileName.lastIndexOf(".")) + "("+fileCount+")" + fileName.substring(fileName.lastIndexOf("."));
            path = path.substring(0,path.lastIndexOf(System.getProperty("file.separator"))+1) + fileName;
            fileCount ++ ;
            f = creatFile(new File(path),fileName);
        }
        return f;
    }

    /**
     * 压缩文件列表中的文件
     *
     * @param files
     * @param outputStream
     * @throws IOException
     */
    public static void zipFile(List<File> files, ZipOutputStream outputStream) throws IOException, ServletException {
        try {
            int size = files.size();
            // 压缩列表中的文件
            for (int i = 0; i < size; i++) {
                File file = (File) files.get(i);
                zipFile(file, outputStream);
            }
        } catch (IOException e) {
            throw e;
        }
    }
    /**
     * 将文件写入到zip文件中
     *
     * @param inputFile
     * @param outputstream
     * @throws Exception
     */
    public static void zipFile(File inputFile, ZipOutputStream outputstream)
            throws IOException, ServletException {
        try {
            if (inputFile.exists()) {
                if (inputFile.isFile()) {
                    FileInputStream inStream = new FileInputStream(inputFile);
                    BufferedInputStream bInStream = new BufferedInputStream(
                            inStream);
                    ZipEntry entry = new ZipEntry(inputFile.getName());
                    outputstream.putNextEntry(entry);
                    final int MAX_BYTE = 10 * 1024 * 1024; // 最大的流为10M
                    long streamTotal = 0; // 接受流的容量
                    int streamNum = 0; // 流需要分开的数量
                    int leaveByte = 0; // 文件剩下的字符数
                    byte[] inOutbyte; // byte数组接受文件的数据
                    streamTotal = bInStream.available(); // 通过available方法取得流的最大字符数
                    streamNum = (int) Math.floor(streamTotal / MAX_BYTE); // 取得流文件需要分开的数量
                    leaveByte = (int) streamTotal % MAX_BYTE; // 分开文件之后,剩余的数量
                    if (streamNum > 0) {
                        for (int j = 0; j < streamNum; ++j) {
                            inOutbyte = new byte[MAX_BYTE];
                            // 读入流,保存在byte数组
                            bInStream.read(inOutbyte, 0, MAX_BYTE);
                            outputstream.write(inOutbyte, 0, MAX_BYTE); // 写出流
                        }
                    }
                    // 写出剩下的流数据
                    inOutbyte = new byte[leaveByte];
                    bInStream.read(inOutbyte, 0, leaveByte);
                    outputstream.write(inOutbyte);
                    outputstream.closeEntry(); // Closes the current ZIP entry
                    // and positions the stream for
                    // writing the next entry
                    bInStream.close(); // 关闭
                    inStream.close();
                }
            } else {
                throw new ServletException("文件不存在！");
            }
        } catch (IOException e) {
            throw e;
        }
    }
    /**
     * 下载文件
     *  @param file
     * @param response
     * @param request
     */
    public static void downloadFile(File file, HttpServletResponse response,
                                    HttpServletRequest request, boolean isDelete) {
        try {
            // 以流的形式下载文件。
            BufferedInputStream fis = new BufferedInputStream(
                    new FileInputStream(file.getPath()));
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            fis.close();
            // 清空response
            response.reset();
            OutputStream toClient = new BufferedOutputStream(
                    response.getOutputStream());
            response.setContentType("application/octet-stream");
//            String fileName = formatFileName(request.getHeader("user-agent"), file.getName());
            response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + URLEncoder.encode(file.getName(),"UTF-8"));
            response.setCharacterEncoding("UTF-8");
            toClient.write(buffer);
            toClient.flush();
            toClient.close();
            if (isDelete) {
                file.delete(); // 是否将生成的服务器端文件删除
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     *  创建文件
     * @param path
     * @param fileName
     */
    public static void createFile(String path, String fileName) {
        File f = new File(path);
        File file = new File(f, fileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public static String formatFileName(String agent, String filename) throws UnsupportedEncodingException {
        //2.文件名处理
        if (agent.indexOf( "Chrome" ) >  0 || agent.indexOf( "Firefox" ) >  0){
            filename = new String(filename.getBytes("utf-8"), "iso-8859-1");
        } else {
            filename = URLEncoder.encode(filename, "utf-8");
            filename = filename.replace("+", "%20");//处理空格变“+”的问题
        }
        return filename;
    }


}
