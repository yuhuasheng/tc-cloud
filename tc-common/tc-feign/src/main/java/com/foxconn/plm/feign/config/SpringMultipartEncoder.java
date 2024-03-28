package com.foxconn.plm.feign.config;

import feign.form.ContentType;
import feign.form.MultipartFormContentProcessor;
import feign.form.spring.SpringFormEncoder;
import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import feign.form.spring.SpringManyMultipartFilesWriter;
import feign.form.spring.SpringSingleMultipartFileWriter;
import org.springframework.web.multipart.MultipartFile;



import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;



/**
 * FileName: SpringMultipartEncoder
 * Author:   SixJR.
 * Date:     2022/3/2 19:54:12
 * Description: Feign实现多文件上传，重写SpringFormEncoder
 * History:
 * <author>          <time>          <version>          <desc>
 */
public class SpringMultipartEncoder extends SpringFormEncoder {
    public SpringMultipartEncoder(Encoder delegate) {
        super(delegate);
        MultipartFormContentProcessor processor = (MultipartFormContentProcessor) getContentProcessor(ContentType.MULTIPART);
        processor.addWriter(new SpringSingleMultipartFileWriter());
        processor.addWriter(new SpringManyMultipartFilesWriter());
    }



    @Override
    public void encode(Object object, Type bodyType, RequestTemplate template) throws EncodeException {
        if (bodyType != null && bodyType.equals(MultipartFile[].class)) {
            MultipartFile[] file = (MultipartFile[]) object;
            if(file != null) {
                Map data = Collections.singletonMap(file.length == 0 ? "" : file[0].getName(), object);
                super.encode(data, MAP_STRING_WILDCARD, template);
                return;
            }
        }
        super.encode(object, bodyType, template);
    }
}
