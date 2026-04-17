package com.smartcontact.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ContactFormDTO {

    @Pattern(regexp = "^[A-Za-z]+$",message = "Only Alphabets Allowed !")
    @NotBlank(message = "First name required")
    private String firstName;

    @Pattern(regexp = "^[A-Za-z]+$",message = "Only Alphabets Allowed !")
    @NotBlank(message = "Last name required")
    private String lastName;
    

    @Email(message = "Inavalid Mail")
    private String email;

    @Pattern(regexp = "^[6-9]\\d{9}$",message = "Please enter a valid 10-digit mobile number")
    @NotBlank(message = "phone is reuired")
    private String phone;

    private String work;
    private String description;
    private boolean favorite = false;
}
