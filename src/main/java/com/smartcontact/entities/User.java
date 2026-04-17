package com.smartcontact.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "user")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank (message = "Name is required !!")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters !!")
    private String name;

    @Column(unique = true,nullable = false)
    @Email(regexp = "^[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+$", message = "Inavalid Mail")
    @NotBlank (message = "Email is required !!")
    private String email;

    private String verificationToken;
    @Column(name = "reg_date")
    private LocalDateTime registrationDate = LocalDateTime.now();

    @ToString.Exclude
    @Pattern(regexp = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,500}$", message = "Password must have: Min 8 chars, 1 Uppercase, 1 Lowercase, 1 Digit & 1 Special character.")
    @NotBlank (message = "Password not Blank !!")
    private String password;

    @Column(length = 1000)
    private String image = "default.png";

    @Column(length = 1000)
    @NotBlank
    @Size(min = 5, max = 1000, message = "About Must Be above 5 characters !!")
    private String about;

    private String status = "inactive";
    private boolean flag = false;

    @Transient
    @AssertTrue(message = "Must be Checked the Box !!")
    private boolean terms;

    private String role;
    private LocalDateTime lastLogin;
    private LocalDateTime currentLogin;

    // For Oauth2 Login
    @Column(length = 20)
    private String provided = "SELF";
    @Column(length = 100)
    private String providerUserId;

    private int resentAttempts = 0;
    private LocalDateTime resendBlockedUntil;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @ToString.Exclude
    List<Contact> contacts = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @ToString.Exclude
    List<MyOrder> myorder = new ArrayList<>();

}
