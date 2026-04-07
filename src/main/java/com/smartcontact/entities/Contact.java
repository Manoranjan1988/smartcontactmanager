package com.smartcontact.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

    @Pattern(regexp = "^[A-Za-z]+$",message = "Only Alphabets Allowed !")
    private String firstName;
    @Pattern(regexp = "^[A-Za-z]+$",message = "Only Alphabets Allowed !")
    private String lastName;
    

    @Email(regexp = "^[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+$", message = "Inavalid Mail")
    @NotBlank (message = "Email is required !!")
    private String email;

    @Pattern(regexp = "^[6-9]\\d{9}$",message = "Please enter a valid 10-digit mobile number")
    private String phone;

    @Column(length = 1000)
    private String work;

    private String image = "default.png";
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String description;

    private boolean flag = true;
    private boolean favorite =false;

    @ManyToOne
    @JoinColumn(name="user_id")
    @ToString.Exclude
    private User user;

}
