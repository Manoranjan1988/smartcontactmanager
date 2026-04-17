package com.smartcontact.googleservice;

import java.util.ArrayList;
import java.util.HashMap;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.smartcontact.dto.GooglePerson;
import com.smartcontact.dto.ImportResult;
import com.smartcontact.entities.Contact;
import com.smartcontact.entities.User;
import com.smartcontact.enums.ContactSource;
import com.smartcontact.repository.ContactRepository;

@Service
public class GoogleContactService {

    @Autowired
    private ContactRepository contactRepository;

    public ImportResult importContacts(List<GooglePerson> persons, User user) {
        int saved = 0;
        int restored = 0;
        int skipped = 0;

        List<Contact> allExisting = contactRepository.findByUserId(user.getId());
        Map<String, Contact> contactMap = new HashMap<>();
        for (Contact c : allExisting) {
            if (c.getPhone() != null) {
                contactMap.put(c.getPhone(), c);
            }
        }

        List<Contact> contactToSave = new ArrayList<>();

        for (GooglePerson person : persons) {
            String name = extractName(person);
            String email = extractEmail(person);
            String phone = extractPhone(person);

            // Validation logic
            if (phone == null && email == null) {
                skipped++;
                continue;
            }
            if (phone != null) {
                phone = phone.replaceAll("\\D", "");
                if (phone.length() > 10)
                    phone = phone.substring(phone.length() - 10);
            }
            if (phone == null || phone.length() < 6) {
                skipped++;
                continue;
            }

            // 2. check by map (No DB call inside loop!)
            if (contactMap.containsKey(phone)) {
                Contact existing = contactMap.get(phone);

                // Case: Soft-deleted Google Contact for restoration
                if (existing.getSource() == ContactSource.GOOGLE && !existing.isFlag()) {
                    updateDetails(existing, name, email);
                    existing.setFlag(true);
                    contactRepository.save(existing); 
                    restored++;
                } else {
                    // Case: Already active or Manual contact
                    skipped++;
                }
                continue;
            }

            // 3. New Contact creation
            Contact contact = new Contact();
            setBasicFields(contact, name, email, phone, user);
            contactToSave.add(contact);

            contactMap.put(phone, contact);
            saved++;
        }

        if (!contactToSave.isEmpty()) {
            contactRepository.saveAll(contactToSave);
        }

        return new ImportResult(saved, restored, skipped);
    }

    private void updateDetails(Contact c, String name, String email) {
        if (name != null && !name.isBlank()) {
            String[] parts = name.trim().split("\\s+", 2);
            c.setFirstName(parts[0]);
            c.setLastName(parts.length > 1 ? parts[1] : "");
        }
        if (email != null)
            c.setEmail(email);
    }

    private void setBasicFields(Contact c, String name, String email, String phone, User user) {
        if (name != null && !name.isBlank()) {
            String[] parts = name.trim().split("\\s+", 2);
            c.setFirstName(parts[0]);
            c.setLastName(parts.length > 1 ? parts[1] : "");
        } else {
            c.setFirstName("unknown");
            c.setLastName("");
        }
        c.setEmail(email != null ? email : "");
        c.setPhone(phone);
        c.setImage("default.png");
        c.setSource(ContactSource.GOOGLE);
        c.setUser(user);
        c.setFlag(true);
    }

    private String extractName(GooglePerson p) {
        return (p.getNames() != null && !p.getNames().isEmpty()) ? p.getNames().get(0).getDisplayName() : null;
    }

    private String extractEmail(GooglePerson p) {
        return (p.getEmailAddresses() != null && !p.getEmailAddresses().isEmpty())
                ? p.getEmailAddresses().get(0).getValue(): null;
    }

    private String extractPhone(GooglePerson p) {
        return (p.getPhoneNumbers() != null && !p.getPhoneNumbers().isEmpty()) ? p.getPhoneNumbers().get(0).getValue(): null;
    }
}
