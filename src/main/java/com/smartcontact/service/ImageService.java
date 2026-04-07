package com.smartcontact.service;

import java.io.IOException;
import java.util.Map;

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
            Map<?, ?> options = com.cloudinary.utils.ObjectUtils.asMap(
                                "folder",folderName,
                                "public_id",publicId,
                                "overwrite",true,
                                "resource_type","auto");
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
            String secureUrl = (String) uploadResult.get("secure_url");
            return secureUrl;
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
