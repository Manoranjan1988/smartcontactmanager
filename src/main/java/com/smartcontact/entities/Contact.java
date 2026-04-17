package com.smartcontact.entities;

import com.smartcontact.enums.ContactSource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "contact")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Contact {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cid;
    private String firstName;
    private String lastName;
    private String email;
    @Column(nullable = false)
    private String phone;

    @Column(length = 1000)
    private String work;

    private String image = "default.png";
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String description;

    private boolean flag = true;
    private boolean favorite =false;

    @Enumerated(EnumType.STRING)
    @Column(name = "source")
    private ContactSource source = ContactSource.MANUAL;

    @ManyToOne(optional = false)
    @JoinColumn(name="user_id",nullable = false)
    @ToString.Exclude
    private User user;

}
