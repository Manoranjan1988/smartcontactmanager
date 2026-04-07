# 🚀 Smart Contact Manager (SCM)

A full-stack **Spring Boot based Contact Management System** with unified authentication, payment integration, and modern UI.

---

## 🔥 Features

### 🔐 Authentication System

* Form Login (Email & Password)
* Google Login (OIDC)
* GitHub Login (OAuth2)
* Unified Principal System
* Provider-based access control (SELF / GOOGLE / GITHUB)

---

### 🧠 Security

* Spring Security Configuration
* Session Management (Max 1 session per user)
* CSRF Protection (Selective Disable)
* Custom Authentication Handlers
* Global Exception Handling

---

### 📇 Contact Management

* Add / Update / Delete Contacts
* Profile Image Upload (Cloudinary)
* Search & Filter Contacts

---

### 💳 Payment Integration

* Razorpay Payment Gateway
* Webhook Verification
* Idempotency Handling
* Payment Success Email (HTML)

---

### 📊 User Dashboard

* User Profile Management
* Donation System
* Order History Page
* Clean UI & Empty State UX

---

## 🛠️ Tech Stack

| Layer      | Technology               |
| ---------- | ------------------------ |
| Backend    | Spring Boot              |
| Security   | Spring Security + OAuth2 |
| Database   | MySQL                    |
| Frontend   | Thymeleaf + Bootstrap    |
| Cloud      | Cloudinary               |
| Payment    | Razorpay                 |
| Deployment | Render                   |

---

## ⚙️ Setup & Installation

### 1️⃣ Clone Repository

```bash
git clone https://github.com/Manoranjan1988/smartcontactmanager.git
cd smartcontactmanager
```

### 2️⃣ Configure Environment Variables

Create `application.properties` or set env variables:

```properties
spring.datasource.url=YOUR_DB_URL
spring.datasource.username=YOUR_DB_USER
spring.datasource.password=YOUR_DB_PASS

spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_SECRET

spring.security.oauth2.client.registration.github.client-id=YOUR_GITHUB_CLIENT_ID
spring.security.oauth2.client.registration.github.client-secret=YOUR_GITHUB_SECRET

razorpay.key_id=YOUR_KEY
razorpay.key_secret=YOUR_SECRET

cloudinary.cloud_name=YOUR_NAME
cloudinary.api_key=YOUR_KEY
cloudinary.api_secret=YOUR_SECRET
```

---

### 3️⃣ Run Application

```bash
mvn spring-boot:run
```

---

## 🌐 Live Demo

Coming Soon...

---

## 📸 Screenshots

(Add screenshots here after deployment)

---

## 🧪 Future Enhancements

* Email Verification System
* Two-Factor Authentication (2FA)
* Contact Export (PDF / Excel)
* Mobile Responsive Improvements

---

## 👨‍💻 Author

**Manoranjan Sahoo**
📧 [manoranjan1988.sahoo@gmail.com](mailto:manoranjan1988.sahoo@gmail.com)

---

## ⭐ Support

If you like this project, give it a ⭐ on GitHub!

---
