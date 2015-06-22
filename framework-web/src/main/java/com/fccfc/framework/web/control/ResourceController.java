/**
 * 
 */
package com.fccfc.framework.web.control;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.fccfc.framework.api.FrameworkException;
import com.fccfc.framework.api.ServiceException;
import com.fccfc.framework.api.bean.resource.AttachmentsPojo;
import com.fccfc.framework.core.ErrorCodeDef;
import com.fccfc.framework.core.GlobalConstants;
import com.fccfc.framework.core.config.Configuration;
import com.fccfc.framework.core.utils.Assert;
import com.fccfc.framework.core.utils.CommonUtil;
import com.fccfc.framework.core.utils.DateConstants;
import com.fccfc.framework.core.utils.DateUtil;
import com.fccfc.framework.core.utils.IOUtil;
import com.fccfc.framework.core.utils.LogUtil;
import com.fccfc.framework.core.utils.security.DataUtil;
import com.fccfc.framework.web.service.ResourceService;
//import com.qiniu.api.auth.digest.Mac;
//import com.qiniu.api.config.Config;
//import com.qiniu.api.rs.PutPolicy;

/**
 * <Description> <br>
 * 
 * @author 伟<br>
 * @version 1.0<br>
 * @CreateDate 2014-11-30 <br>
 * @see com.fccfc.framework.web.control <br>
 */
@Controller
public class ResourceController {

    private Map<String, Long> mediaSize;

    private Map<String, String> contentTypes;

    private Map<String, MediaType> meidaTypes;

    @javax.annotation.Resource
    private ResourceService resourceService;

    public ResourceController() {
        mediaSize = new HashMap<String, Long>();
        mediaSize.put("text", 1024 * 1024 * 2014l);
        mediaSize.put("image", 1024 * 1024 * 1024l);
        mediaSize.put("voice", 2 * 1024 * 1024 * 1024l);
        mediaSize.put("file", 2 * 1024 * 1024 * 1024l);

        contentTypes = new HashMap<String, String>();
        contentTypes.put("text/xml", "xml");
        contentTypes.put("application/xml", "xml");
        contentTypes.put("text/html", "html");
        contentTypes.put("text/plain", "txt");
        contentTypes.put("application/x-javascript", "js");
        contentTypes.put("text/css", "css");
        contentTypes.put("application/json", "json");

        contentTypes.put("application/x-jpg", "jpg");
        contentTypes.put("image/jpeg", "jpg");
        contentTypes.put("image/jpg", "jpg");
        contentTypes.put("application/x-png", "png");
        contentTypes.put("image/png", "png");
        contentTypes.put("image/gif", "gif");
        contentTypes.put("audio/mp3", "mp3");
        contentTypes.put("audio/amr", "amr");
        contentTypes.put("application/octet-stream", "file");

        meidaTypes = new HashMap<String, MediaType>();
        meidaTypes.put("xml", MediaType.APPLICATION_XML);
        meidaTypes.put("html", MediaType.TEXT_HTML);
        meidaTypes.put("txt", MediaType.TEXT_PLAIN);
        meidaTypes.put("js", MediaType.valueOf("application/x-javascript"));
        meidaTypes.put("css", MediaType.valueOf("text/css"));
        meidaTypes.put("json", MediaType.valueOf("application/json"));
        meidaTypes.put("jpg", MediaType.IMAGE_JPEG);
        meidaTypes.put("png", MediaType.IMAGE_PNG);
        meidaTypes.put("gif", MediaType.IMAGE_GIF);
        meidaTypes.put("mp3", MediaType.valueOf("audio/mp3"));
        meidaTypes.put("amr", MediaType.valueOf("audio/amr"));
        meidaTypes.put("file", MediaType.APPLICATION_OCTET_STREAM);
    }

//    @ResponseBody
//    public Map<String, Object> upToken() throws FrameworkException {
//        Config.ACCESS_KEY = Configuration.getString("QINIU.ACCESS_KEY");
//        Config.SECRET_KEY = Configuration.getString("QINIU.SECRET_KEY");
//        Mac mac = new Mac(Config.ACCESS_KEY, Config.SECRET_KEY);
//        // 请确保该bucket已经存在
//        String bucketName = Configuration.getString("QINIU.BUCKET_NAME");
//        PutPolicy putPolicy = new PutPolicy(bucketName);
//        Map<String, Object> result = new HashMap<String, Object>();
//        try {
//            String uptoken = putPolicy.token(mac);
//            result.put("uploadToken", uptoken);
//        }
//        catch (Exception e) {
//            throw new ServiceException(ErrorCodeDef.QI_NIU_UP_TOKEN_ERROR_20018, e);
//        }
//        result.put("uploadUrl", Configuration.getString("QINIU.UPLOAD_DOMAIN"));
//        return result;
//    }

    @ResponseBody
    public ResponseEntity<Resource> download(@RequestParam("mediaId") Integer resourceId, @RequestParam(
        value = "isThumb", required = false) String isThumb, @RequestHeader HttpHeaders reqHeader) throws Exception {
        Assert.notNull(resourceId, "资源标识不能为空");
        AttachmentsPojo attachment = resourceService.downloadResource(resourceId, "1".equals(isThumb));
        HttpHeaders httpHeader = new HttpHeaders();
        httpHeader.setExpires(System.currentTimeMillis() + 315360000);
        httpHeader.setLastModified(System.currentTimeMillis());
        httpHeader.setCacheControl("max-age=315360000");
        httpHeader.setContentType(getMediaType(attachment.getAttachmentsType()));
        httpHeader.setContentDispositionFormData("attachment", attachment.getAttachmentsName());

        if (reqHeader.getIfModifiedSince() > 0) {
            return new ResponseEntity<Resource>(httpHeader, HttpStatus.NOT_MODIFIED);
        }

        File file;
        if ("Y".equals(attachment.getIsPicture()) && "1".equals(isThumb)
            && StringUtils.isNotEmpty(attachment.getThumbPath())) {
            file = new File(Configuration.getString("RESOURCE.PATH") + attachment.getThumbPath());
        }
        else {
            file = new File(Configuration.getString("RESOURCE.PATH") + attachment.getFilePath());
        }
        return new ResponseEntity<Resource>(new FileSystemResource(file), httpHeader, HttpStatus.OK);
    }

    @ResponseBody
    public Map<String, Object> upload(HttpServletRequest request) throws FrameworkException {
        if (!(request instanceof MultipartHttpServletRequest)) {
            throw new ServiceException(ErrorCodeDef.FILE_NOT_FIND_20013, "未找到上传的文件");
        }

        boolean isBase64 = StringUtils.equals(request.getParameter("base64"), "true");

        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
        if (CommonUtil.isEmpty(fileMap)) {
            throw new ServiceException(ErrorCodeDef.FILE_NOT_FIND_20013, "未找到上传的文件");
        }

        for (Entry<String, MultipartFile> fileEntry : fileMap.entrySet()) {
            MultipartFile file = fileEntry.getValue();
            checkFile(file.getContentType(), file.getSize());
        }

        String path = GlobalConstants.PATH_SPLITOR + DateUtil.date2String(new Date(), DateConstants.DATE_FORMAT_10_2);
        File dir = new File(Configuration.getString("RESOURCE.PATH") + path);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();
        calendar.add(Calendar.MINUTE, 10);
        Date expDate = calendar.getTime();

        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();

        try {
            int i = 0;
            for (Entry<String, MultipartFile> fileEntry : fileMap.entrySet()) {
                MultipartFile file = fileEntry.getValue();
                String contentType = contentTypes.get(file.getContentType());
                String type = getType(contentType);
                String fileName = GlobalConstants.PATH_SPLITOR + DateUtil.getCurrentTimestamp() + (i++) + "."
                    + contentType;

                if (isBase64) {
                    String content = IOUtil.readString(file.getInputStream());
                    byte[] fileContent = DataUtil.base64Decode(content);
                    IOUtil.writeFile(fileContent, new File(dir.getAbsolutePath() + fileName));
                }
                else {
                    IOUtil.copyFileFromInputStream(dir.getAbsolutePath() + fileName, file.getInputStream(),
                        "text".equals(type) ? GlobalConstants.DEFAULT_CHARSET : null);
                }

                results
                    .add(saveFile(file.getName(), contentType, currentDate, expDate, path + fileName, file.getSize()));
            }

        }
        catch (IOException e) {
            throw new ServiceException(ErrorCodeDef.READ_PARAM_ERROR_10027, e);
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("medias", results);
        return result;

    }

    
    private void checkFile(String fileType, Long fileSize) throws ServiceException {
        String contentType = contentTypes.get(fileType);
        if (CommonUtil.isEmpty(contentType)) {
            throw new ServiceException(ErrorCodeDef.UNSPORT_CONTENT_TYPE_20012, "文件类型不支持");
        }

        String type = getType(contentType);

        Long size = mediaSize.get(type);
        if (size == null) {
            throw new ServiceException(ErrorCodeDef.UNSPORT_MEDIA_TYPE_20010, "不支持的媒体类型");
        }

        LogUtil.info("接收到大小为{0}的文件" + fileSize);
        if (fileSize - size > 0) {
            throw new ServiceException(ErrorCodeDef.FILE_IS_TO_LARGER_20011, "文件大小超限");
        }
    }

    private Map<String, Object> saveFile(String fileName, String contentType, Date currentDate, Date expDate,
        String path, long fileSize) throws ServiceException {
        AttachmentsPojo pojo = new AttachmentsPojo();
        pojo.setAttachmentsName(fileName);
        pojo.setAttachmentsType(contentType);
        pojo.setCreateTime(currentDate);
        pojo.setDownloadsNum(0);
        pojo.setExpTime(expDate);
        pojo.setFilePath(path);
        pojo.setFileSize(fileSize);

        String type = getType(contentType);
        pojo.setIsPicture("image".equals(type) ? "Y" : "N");
        pojo.setIsThumb("N");
        pojo.setIsRemote("N");
        resourceService.saveAttachment(pojo);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("msgtype", type);
        result.put("mediaId", pojo.getAttachmentsId());
        result.put("mediaName", fileName);
        result.put("createTime", DateUtil.date2String(currentDate, DateConstants.DATETIME_FORMAT_14));
        return result;
    }

    private String getType(String type) {
        String tempType = "," + type + ",";

        if (",xml,html,json,css,js,txt,".indexOf(tempType) != -1) {
            return "text";
        }
        else if (",jpg,png,gif,".indexOf(tempType) != -1) {
            return "image";
        }
        else if (",mp3,amr,".indexOf(tempType) != -1) {
            return "voice";
        }
        else {
            return "file";
        }
    }

    private MediaType getMediaType(String type) {
        if (CommonUtil.isNotEmpty(type)) {
            MediaType mediaType = meidaTypes.get(type);
            if (mediaType != null) {
                return mediaType;
            }
        }

        return MediaType.APPLICATION_OCTET_STREAM;
    }
}