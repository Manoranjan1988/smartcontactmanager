console.log("JS started")

window.history.scrollRestoration = "manual";

AOS.init({
        duration: 800,
        once: true,
        easing: 'ease-in-out',
        mirror: false
    });


$(document).ready(function () {
    console.log("Jquery Started..........");
    setTimeout(function () {
        $(".alert").fadeTo(500, 0).slideUp(500, function () {
            $(this).remove();
        });
    }, 3000);
});

document.addEventListener("DOMContentLoaded", () => {
    console.log("expire time start.............")
    let timerElement = document.getElementById("timer");
    if (timerElement) {

        let savedTime = localStorage.getItem("otp_timer");
        let time = savedTime ? parseInt(savedTime) : 40;

        let countdown = setInterval(() => {

            let m = Math.floor(time / 60).toString().padStart(2, "0");
            let s = (time % 60).toString().padStart(2, "0");

            timerElement.innerHTML = `${m}:${s}`;
            localStorage.setItem("otp_timer", time);

            if (time <= 0) {
                clearInterval(countdown);
                localStorage.removeItem("otp_timer");
                timerElement.innerHTML = "Expired";
                timerElement.classList.replace('bg-warning', 'bg-danger');

                let submitBtn = document.getElementById("submit-btn")
                if (submitBtn) {
                    submitBtn.style.display = 'none';
                }
                const resendBox = document.getElementById("resend-container");
                console.count(typeof resendBox)
                if (resendBox) {
                    resendBox.style.display = 'block';
                }

            }
            time--
        }, 1000);
    }

})

document.addEventListener("DOMContentLoaded", () => {
    const toggleBtn = document.getElementById("togglePassword"); // Ye SPAN hai
    const passwordInput = document.getElementById("password");   // Ye INPUT hai
    const eyeIcon = document.getElementById("eyeIcon");

    if (toggleBtn && passwordInput) {
        toggleBtn.addEventListener('click', () => {
            // Toggle logic
            const type = passwordInput.getAttribute("type") === "password" ? "text" : "password";
            passwordInput.setAttribute("type", type);
            
            // Icon toggle
            if (eyeIcon) {
                eyeIcon.classList.toggle("bi-eye-fill");
                eyeIcon.classList.toggle("bi-eye-slash-fill");
            }
        });
    }
});

// Sweetalert2
document.addEventListener("DOMContentLoaded", () => {
    const msgInput = document.getElementById("msg-box");
    const typeInput = document.getElementById("type-box");
    const titleInput = document.getElementById("title-box");

    const msg = msgInput ? msgInput.value.trim() : "";
    const type = typeInput ? typeInput.value.trim() : "info";
    const title = titleInput ? titleInput.value.trim() : (type === "success" ? "Great!" : "Oops...");
    console.log("DEBUG MSG:", msg);
    // Logic: Agar message hai aur "undefined" nahi hai

    if (msg && msg !== "" && msg !== "undefined") {
        Swal.fire({
            icon: type || 'info',
            title: title,
            html: msg,
            width: '400px',
            padding: '1.5rem',
            background: '#f8fafc',
            color: '#1e293b',
            confirmButtonColor: '#00a3ff',
            iconColor: type === "success" ? "#00a3ff" : "#ef4444",

            timer: type === 'success' ? 2000 : 5000,
            timerProgressBar: true,

            showConfirmButton: type === 'success' ? false : true,
            confirmButtonText:'okay',

            // Premium Feel: Thoda animation dalo
            showClass: {
                popup: 'animate__animated animate__fadeInDown animate__faster'
            },
            hideClass: {
                popup: 'animate__animated animate__fadeOutUp animate__faster'
            }

        });
    }
});


// disabled button during register form submission
document.querySelector('form').addEventListener('submit', function(e) {
    console.log("Registration Started......")
    const submitBtn = document.getElementById('submitBtn'); 
    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Processing...';
    }
});


// disabled button during contactus mail send
document.addEventListener('DOMContentLoaded', function() {
    const sendBtn = document.getElementById('sendEmailBtn');
    const contactForm = sendBtn ? sendBtn.closest('form') : null;

    if (contactForm && sendBtn) {
        contactForm.addEventListener('submit', function(e) {
            // HTML5 Validation check (required fields khali toh nahi?)
            if (contactForm.checkValidity()) {
                // 1. Button freeze karo
                sendBtn.disabled = true;
                sendBtn.classList.add('opacity-70', 'cursor-not-allowed');
                
                // 2. Spinner aur Text badlo
                sendBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin mr-2"></i> Sending Message...';
                
                console.log("Contact Us: Email firing via Brevo...");
            }
        });
    }
});

//RESEND EMAIL FOR USER ACTIVATION
document.addEventListener("DOMContentLoaded", function() {
    
    const resendForm = document.getElementById("resendForm");
    const resendBtn = document.getElementById("resendBtn");

    if (resendForm && resendBtn) {
        resendForm.addEventListener("submit", function (event) {
            resendBtn.disabled = true;
            resendBtn.innerText = "Sending...";

            setTimeout(() => {
                if (resendBtn) {
                    resendBtn.innerText = "Resend Verification Email";
                    resendBtn.disabled = false;
                }
            }, 5000);
        });
    }
});


