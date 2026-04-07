package com.smartcontact.service;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.smartcontact.entities.Contact;
import com.smartcontact.entities.User;
import com.smartcontact.exception.DuplicateContactException;
import com.smartcontact.exception.ExportException;
import com.smartcontact.repository.ContactRepository;
import com.smartcontact.repository.UserRepository;

import jakarta.servlet.http.HttpServletResponse;

@Service
public class ContactService {
    private static final Logger log = LoggerFactory.getLogger(ContactService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ImageService imageService;

    private final AtomicBoolean isBlastRunning = new AtomicBoolean(false);

    public long getContactCount(Principal principal) {
        User user = userRepository.getUserByUserName(principal.getName());
        return contactRepository.countActiveContacts(user.getId(), true);
    }

    @Transactional
    public Contact saveContact(Contact contact, MultipartFile file, Principal principal) {

        try {

            User user = userRepository.getUserByUserName(principal.getName());
            Optional<Contact> oldContactOpt = contactRepository.findByPhoneAndUserId(contact.getPhone(), user.getId());
            if (oldContactOpt.isPresent()) {
                Contact oldContact = oldContactOpt.get();
                if (oldContact.isFlag()) {

                    throw new DuplicateContactException("Number already saved in the name of: "
                            + oldContact.getFirstName() + " " + oldContact.getLastName());
                }

                oldContact.setFirstName(contact.getFirstName());
                oldContact.setLastName(contact.getLastName());
                oldContact.setEmail(contact.getEmail());
                oldContact.setWork(contact.getWork());
                oldContact.setDescription(contact.getDescription());
                oldContact.setFavorite(contact.isFavorite());
                oldContact.setFlag(true);

                if (file != null && !file.isEmpty()) {
                    String fixedId = user.getEmail().split("@")[0] + "_" + contact.getPhone();
                    String cloudUrl = imageService.uploadImage(file, "SCM_Contacts", fixedId);
                    oldContact.setImage(cloudUrl);
                } else {
                    oldContact.setImage("default.png");
                }
                return contactRepository.save(oldContact);
            }
            contact.setUser(user);
            if (file != null && !file.isEmpty()) {
                String fixedId = user.getEmail().split("@")[0] + "_" + contact.getPhone();
                String cloudUrl = imageService.uploadImage(file, "SCM_Contacts", fixedId);
                contact.setImage(cloudUrl);
            } else {
                contact.setImage("default.png");
            }
            Contact saveContact = contactRepository.save(contact);
            log.info("Actual Contact Saved ID: {} ",saveContact.getCid());
            return contact;
        } catch (DataIntegrityViolationException dv) {
           log.info("DB Level Duplicate Blocked");
            throw dv;
        } catch (DuplicateContactException dc) {
            log.error("MSG: " ,dc.getMessage());
            throw dc;
        } catch (Exception e) {

            throw e;
        }

    }

    public Page<Contact> getAllContacts(Principal principal, int currentPage, String key) {
        User user = userRepository.getUserByUserName(principal.getName());
        Long uid = user.getId();
        Pageable pageable = PageRequest.of(currentPage, 4);
        if (key != null && !key.trim().isEmpty()) {
            return contactRepository.getAllContactsWithSearch(uid, true, key, pageable);
        }
        return contactRepository.findByUserIdAndFlag(uid, true, pageable);

    }

    public Contact toggleFavorite(Long cid, Principal principal) {
        Optional<Contact> optional = contactRepository.findById(cid);
        Contact contact = null;
        if (optional.isPresent()) {
            contact = optional.get();
        } else {
            throw new RuntimeException("Contact Not Found: " + cid);
        }

        User user = userRepository.getUserByUserName(principal.getName());
        if (contact.getUser().getId() == user.getId()) {
            contact.setFavorite(!contact.isFavorite());
            return contactRepository.save(contact);
        } else {
            throw new RuntimeException("You are not authorized");
        }
    }

    public long favoriteCount(Principal principal) {
        User user = userRepository.getUserByUserName(principal.getName());
        return contactRepository.countByUserIdAndFlagAndFavorite(user.getId(), true, true);
    }

    public int getProfileStrength(User user) {
        int strength = 0;

        if (user.getName() != null && !user.getName().isEmpty()) {
            strength += 25;
        }
        if (user.getAbout() != null && user.getAbout().length() > 5) {
            strength += 25;
        }
        if (user.getImage() != null && !user.getImage().equals("default.png")) {
            strength += 25;
        }
        if (user.getStatus().equals("active")) {
            strength += 25;
        }

        return strength;
    }

    public Contact getContactById(Long cid) {
        return contactRepository.findById(cid)
                .orElseThrow(() -> new RuntimeException("Contact Not Found with ID:" + cid));
    }

    public void deleteContact(Contact contact) {
        try {
            String imageName = contact.getImage();
            if (imageName != null && !imageName.equals("default_profile.png")) {
                imageService.deleteImage(imageName);
            }
            contact.setFlag(false);
            contactRepository.save(contact);
        } catch (Exception e) {
            throw new RuntimeException("Delete failed: " + e.getMessage());
        }
    }

    @Transactional
    public synchronized Contact updateContact(Contact contact, MultipartFile file, Principal principal) {

        try {
            User user = this.userRepository.getUserByUserName(principal.getName());

            Optional<Contact> conflictOpt = contactRepository.findByPhoneAndUserId(contact.getPhone(), user.getId());

            if (conflictOpt.isPresent()) {
                Contact confliContact = conflictOpt.get();
                if (confliContact.getCid() != contact.getCid()) {
                    String status = confliContact.isFlag() ? "Active" : "Deleted";
                    throw new DuplicateContactException(
                            "Number already exist in your " + status + " contacts as " + confliContact.getFirstName()
                                    + " " + confliContact.getLastName() + ". Please use a different number.");
                }
            }

            Contact oldContact = this.contactRepository.getContactById(contact.getCid());

            if (!file.isEmpty()) {

                if (oldContact.getImage() != null && oldContact.getImage().contains("cloudinary.com")) {
                    imageService.deleteImage(oldContact.getImage());
                    log.info("DEBUG: Old Cloudinary Contact Image Deleted.");
                }
                String fixedId = user.getEmail().split("@")[0] + "_" + contact.getPhone();
                String cloudUrl = imageService.uploadImage(file, "SCM_Contacts", fixedId);
                if (cloudUrl != null) {
                    contact.setImage(cloudUrl);
                }
            } else {
                contact.setImage(oldContact.getImage());
            }
            contact.setUser(user);
            user.setTerms(true);
            this.contactRepository.save(contact);
        } catch (Exception e) {
            throw e;
        }
        return contact;
    }

    @Transactional
    public void updateProfile(User user, String newName, MultipartFile file, String about) {
        try {
            user.setName(newName);
            user.setAbout(about);

            if (!file.isEmpty()) {

                if (user.getImage() != null && user.getImage().contains("cloudinary.com")) {
                    imageService.deleteImage(user.getImage());
                    log.info("DEBUG: Old Cloudinary Image Deleted.");
                }
                String fixedId = user.getEmail().replaceAll("[^a-zA-Z0-9]", "_");
                String cloudImageUrl = imageService.uploadImage(file, "SCM_Profiles", fixedId);
                if (cloudImageUrl != null) {
                    user.setImage(cloudImageUrl);
                    log.info("Profile Photo Updated: {}", cloudImageUrl);
                }
            }
            user.setTerms(true);
            userRepository.save(user);
        } catch (Exception e) {
            log.error("The Error is: " ,e.getMessage());
            throw new RuntimeException("Profile Update Failed!", e);
        }
    }

    public List<Contact> findByUser(String email) {
        return contactRepository.getAllContacts(email, true);

    }

    public void exportToExcel(List<Contact> contacts, HttpServletResponse response) {
        try {
            // Create a new Workbook
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("contacts");

                // Create a new Row
                Row header = sheet.createRow(0);

                // Creating a Header of Excel
                String[] columns = { "Name", "Email", "Phone", "Work" };
                for (int i = 0; i < columns.length; i++) {
                    Cell cell = header.createCell(i);
                    cell.setCellValue(columns[i]);
                    sheet.autoSizeColumn(i);
                }

                int rowIdx = 1;
                // Creating a data of Excel
                for (Contact c : contacts) {

                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(c.getFirstName() + " " + c.getLastName());
                    row.createCell(1).setCellValue(c.getEmail());
                    row.createCell(2).setCellValue(c.getPhone());
                    row.createCell(3).setCellValue(c.getWork());
                }
                for (int i = 0; i < 3; i++) {
                    sheet.autoSizeColumn(i);
                }
                workbook.write(response.getOutputStream());
            }
        } catch (Exception e) {
            throw new ExportException("Something Error in Creating Excel!");
        }

    }

    public void exportToPdf(List<Contact> contacts, HttpServletResponse response, User user) {
        try {
            PdfWriter writer = new PdfWriter(response.getOutputStream());
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            Paragraph heading = new Paragraph("SCM - Your Contact List");
            heading.setBold();
            heading.setFontSize(18);
            heading.setTextAlignment(TextAlignment.CENTER);
            heading.setMarginBottom(5);

            Paragraph userHead = new Paragraph("USER: " + user.getName());
            userHead.setBold();
            userHead.setFontSize(10);
            userHead.setTextAlignment(TextAlignment.LEFT);
            userHead.setMarginBottom(5);
            userHead.setFontColor(ColorConstants.DARK_GRAY);

            DateTimeFormatter myFormat = DateTimeFormatter.ofPattern("dd-MMM-yy,HH:mm");
            Paragraph downDate = new Paragraph("Generated on: " + LocalDateTime.now().format(myFormat));
            downDate.setBold();
            downDate.setFontSize(10);
            downDate.setTextAlignment(TextAlignment.LEFT);
            downDate.setMarginBottom(20);
            downDate.setFontColor(ColorConstants.DARK_GRAY);

            document.add(heading);
            document.add(userHead);
            document.add(downDate);
            Table table = new Table(UnitValue.createPercentArray(new float[] { 10, 25, 35, 20, 20 }));
            table.setWidth(UnitValue.createPercentValue(100));
            table.setHorizontalAlignment(HorizontalAlignment.CENTER);

            table.addHeaderCell(new com.itextpdf.layout.element.Cell()
                    .add(new Paragraph("SlNo").setBold()
                            .setTextAlignment(TextAlignment.CENTER).setBackgroundColor(ColorConstants.LIGHT_GRAY)));

            table.addHeaderCell(new com.itextpdf.layout.element.Cell()
                    .add(new Paragraph("Name").setBold()
                            .setTextAlignment(TextAlignment.CENTER).setBackgroundColor(ColorConstants.LIGHT_GRAY)));
            table.addHeaderCell(new com.itextpdf.layout.element.Cell()
                    .add(new Paragraph("Email").setBold()
                            .setTextAlignment(TextAlignment.CENTER).setBackgroundColor(ColorConstants.LIGHT_GRAY)));
            table.addHeaderCell(new com.itextpdf.layout.element.Cell()
                    .add(new Paragraph("Phone").setBold()
                            .setTextAlignment(TextAlignment.CENTER).setBackgroundColor(ColorConstants.LIGHT_GRAY)));
            table.addHeaderCell(new com.itextpdf.layout.element.Cell()
                    .add(new Paragraph("Work").setBold()
                            .setTextAlignment(TextAlignment.CENTER).setBackgroundColor(ColorConstants.LIGHT_GRAY)));

            int slno = 1;
            for (Contact c : contacts) {
                table.addCell(new com.itextpdf.layout.element.Cell()
                        .add(new Paragraph(String.valueOf(slno++))).setTextAlignment(TextAlignment.CENTER));

                table.addCell(c.getFirstName() + " " + c.getLastName()).setTextAlignment(TextAlignment.CENTER);
                table.addCell(c.getEmail()).setTextAlignment(TextAlignment.CENTER);
                table.addCell(c.getPhone()).setTextAlignment(TextAlignment.CENTER);
                table.addCell(c.getWork()).setTextAlignment(TextAlignment.CENTER);
            }
            document.add(table);
            document.close();

        } catch (IOException e) {

            throw new ExportException("Something Error in Creating Pdf!");
        }
    }

    public Long noOfAllContacts(String email) {
        return contactRepository.noOfContacts(email, true);
    }

    public void processEmailBlast(String userEmail, String subject, String message, MultipartFile file)
            throws Exception {
        if (!isBlastRunning.compareAndSet(false, true)) {
            throw new Exception("BLAST ALREADY IS PROGRESS");
        }
        byte[] bytes = null;
        String fileName = null;
        try {
            List<Contact> allContacts = contactRepository.getAllContacts(userEmail, true);
            if (file != null && !file.isEmpty()) {
                bytes = file.getBytes();
                fileName = file.getOriginalFilename();
            }
            for (Contact contact : allContacts) {
                emailService.sendEmailWithAttachment(contact.getEmail(), subject, message, bytes, fileName);
                log.info("Queued for: {}", contact.getEmail());
            }

        } finally {
            isBlastRunning.set(false);
        }
    }

}
