package com.smartcontact.service;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Service
public class ImageService {
 private static final Logger log = LoggerFactory.getLogger(ImageService.class);
    @Autowired
    private Cloudinary cloudinary;

    public String uploadImage(MultipartFile file,String folderName,String publicId){
        try {
            if(file.isEmpty()) return null;
            BufferedImage originalImage = ImageIO.read(file.getInputStream());

            //read image
            int width = originalImage.getWidth();
            int height = originalImage.getHeight();

            //resize image
            int newWidth = width;
            int newHeight = height;

            if(width > 500){
             newWidth = 500;
             newHeight = (height * newWidth) / width;
            }

            Image tmp = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
            BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);

            Graphics2D g2d = resized.createGraphics();
            g2d.drawImage(tmp, 0, 0,null);
            g2d.dispose();

            // Compress (JPG quality)
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            ImageWriter writer = writers.next();
            
            ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
            writer.setOutput(ios);

            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.7f);

            writer.write(null, new IIOImage(resized, null, null),param);

            writer.dispose();
            ios.close();

            byte[] compressedImage  = baos.toByteArray();
            
            //Upload to Cloudinary
            Map<?, ?> options = com.cloudinary.utils.ObjectUtils.asMap(
                                "folder",folderName,
                                "public_id",publicId,
                                "overwrite",true,
                                "resource_type","image");

            Map<?, ?> uploadResult = cloudinary.uploader().upload(compressedImage, options);
            return (String) uploadResult.get("secure_url");

        } catch (IOException e) {
           log.error("Cloudinary Upload Error: ",e.getMessage());
           throw new RuntimeException("Image upload failed! DB save cancelled.");
        }
    }


    public void deleteImage(String imageUrl) {
    try {
    
        String publicIdWithExtension = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
        String publicId = publicIdWithExtension.substring(0, publicIdWithExtension.lastIndexOf("."));
        
        
        String[] parts = imageUrl.split("/");
        String folderName = parts[parts.length - 2]; 

        //Final Path: "SCM_Contacts/abc123"
        String fullPath = folderName + "/" + publicId;
        
        cloudinary.uploader().destroy(fullPath, ObjectUtils.emptyMap());
        
    } catch (Exception e) {
        log.error("Error deleting from Cloudinary: " , e.getMessage());
    }
}

}
