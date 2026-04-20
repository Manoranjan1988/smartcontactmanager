package com.smartcontact.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smartcontact.entities.Contact;
import com.smartcontact.enums.ContactSource;

public interface ContactRepository extends JpaRepository<Contact, Long> {

        @Query("SELECT c FROM Contact c WHERE c.user.id = :userId AND c.flag = :flag " +
                        "AND (LOWER(c.firstName) LIKE LOWER(CONCAT('%', :key, '%')) " +
                        "OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :key, '%')) " +
                        "OR LOWER(c.email) LIKE LOWER(CONCAT('%', :key, '%')) " +
                        "OR c.phone LIKE %:key%) "+
                        "ORDER BY c.cid DESC")

        Page<Contact> getAllContactsWithSearch(
                        @Param("userId") Long userId,
                        @Param("flag") boolean flag,
                        @Param("key") String key,
                        Pageable pageable);

        @Query("SELECT c FROM Contact c WHERE c.user.id = :userId and c.flag = :flag")
        Page<Contact> findByUserIdAndFlag(
                        @Param("userId") Long userId,
                        @Param("flag") boolean flag,
                        Pageable pageable);

        @Query("SELECT COUNT(c) FROM Contact c WHERE c.user.id = :userId and c.flag =:flag")
        long countActiveContacts(@Param("userId") long userId, boolean flag);

        @Query("SELECT COUNT(c) FROM Contact c WHERE c.user.id = :userId and c.flag =:flag and c.favorite =:favorite")
        long countByUserIdAndFlagAndFavorite(@Param("userId") long userId, boolean flag, boolean favorite);

        @Query("SELECT c FROM Contact c WHERE c.cid = :cid")
        Contact getContactById(@Param("cid") long cid);

        @Query("SELECT c FROM Contact c where c.user.email = :email and c.flag =:flag")
        List<Contact> getAllContacts(@Param("email") String userId, boolean flag);

        @Query("SELECT COUNT(c) from Contact c where c.user.email =:email AND c.flag =:flag")
        Long noOfContacts(@Param("email") String email, boolean flag);

        @Query("SELECT c FROM Contact c WHERE c.phone = :phone AND c.user.id = :userId AND c.cid != :currentId")
        Contact findByPhoneAndUserIdAndCidNot(@Param("phone") String phone, @Param("userId") Long userId,@Param("currentId") Long currentId);

        Optional<Contact> findByPhoneAndUserId(String phone, Long userId);

        @Query("SELECT c.phone FROM Contact c WHERE c.user.id = :userId")
        List<String> findPhonesByUserId(Long userId);
         
        @Modifying
        @Query("UPDATE Contact c SET c.flag = false WHERE c.user.id = :userId AND c.source = :source AND c.flag = true")
        int softDeleteAllGoogleContacts(@Param("userId") Long userId, @Param("source") ContactSource source);
        
        List<Contact> findByUserId(Long userId);
        List<Contact> findByUserIdAndFlagTrue(Long userId);

}
