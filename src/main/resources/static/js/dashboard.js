// 1. Dark Mode Logic
tailwind.config = { darkMode: 'class' }
function toggleDarkMode() {
    const isDark = document.documentElement.classList.toggle('dark');
    localStorage.setItem('theme', isDark ? 'dark' : 'light');
}

// 2. Sidebar Toggle Logic 
function toggleSidebar() {
    const sidebar = document.getElementById('sidebar');
    const overlay = document.getElementById('overlay');
    if (sidebar) {
        sidebar.classList.toggle('-translate-x-full');
        if (overlay) overlay.classList.toggle('hidden');
    }
}

// Theme sync on load
document.addEventListener('DOMContentLoaded', () => {
    if (localStorage.getItem('theme') === 'dark') {
        document.documentElement.classList.add('dark');
    }
});

tinymce.init({
    selector: '#mytextarea',
    promotion: false,
    branding: false,
    help_accessibility: false,

    plugins: 'lists link image code table wordcount',
    toolbar: 'undo redo | styleselect | bold italic | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | link image | code',

    height: 300,
    skin: 'oxide-dark',
    content_css: 'dark',
    content_style: 'body { font-family:Helvetica,Arial,sans-serif; font-size:14px }'
});

// Sweetalert2
document.addEventListener("DOMContentLoaded", () => {
    const msgInput = document.getElementById("msg-box");
    const typeInput = document.getElementById("type-box");

    const msg = msgInput ? msgInput.value.trim() : "";
    const type = typeInput ? typeInput.value.trim() : "";

    console.log("Sweet Alert started.............");
    console.log("Message received:", msg);
    console.log("Type received:", type);

    if (msg && msg !== "" && msg !== "undefined") {
        Swal.fire({
            icon: type || 'info',
            title: type === "success" ? "Great!" : "Oops...",
            html: msg,
            width: '400px',
            padding: '1.5rem',
            background: '#1f2937',
            color: '#fff',
            confirmButtonColor: '#3b82f6',
            iconColor: type === "success" ? "#10b981" : "#ef4444",

            timer: type === 'success' ? 1500 : 5000,

            timerProgressBar: type === 'success' ? false : true,

            showConfirmButton: type === 'success' ? false : true
        });
    }
});

/*for deleting contact*/

function deleteContact(cid) {
    Swal.fire({
        title: 'Are you sure?',
        text: "The Contact will be delete Permanently",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#3b82f6',
        cancelButtonColor: '#ef4444',
        confirmButtonText: 'Delete',
        cancelButtonText: 'Cancel',
        background: '#1f2937',
        color: '#fff',

        customClass: {
            confirmButton: 'rounded-xl px-6 py-2.5 font-bold',
            cancelButton: 'rounded-xl px-6 py-2.5 font-bold'
        }
    }).then((result) => {
        if (result.isConfirmed) {
            window.location = "/user/delete/" + cid;
        }
    })
}

// for contact view profile

document.addEventListener("click", function (e) {
    const btn = e.target.closest(".view-btn");

    if (btn) {
        // 1. Pehle saare elements ko variable mein dalo (Define karo)
        const modal = document.getElementById("contactModal");
        const imgTag = document.getElementById("m-img"); // <--- Yeh line honi chahiye!
        const nameTag = document.getElementById("m-name");
        const emailTag = document.getElementById("m-email");
        const phoneTag = document.getElementById("m-phone");
        const workTag = document.getElementById("m-work");

        // 2. Phir unka data set karo
        if (modal && imgTag) {
            nameTag.innerText = btn.getAttribute("data-name");
            emailTag.innerText = btn.getAttribute("data-email");
            phoneTag.innerText = btn.getAttribute("data-phone");
            workTag.innerText = btn.getAttribute("data-work");

            const image = btn.getAttribute("data-image");

            if (image && image.startsWith('http')) {
                imgTag.src = image;
            } else {
                imgTag.src = "/image/default_profile.png";
            }
            imgTag.onerror = function () {
                this.src = "/image/default_profile.png";
            };

            // Show Modal
            modal.classList.remove("hidden");
            modal.classList.add("flex");
            document.body.style.overflow = "hidden";
        } else {
            console.error("Dadu, Modal ya Image ID nahi mili! HTML check karo.");
        }
    }
});

function closeModal() {
    const modal = document.getElementById("contactModal");
    const modalContent = modal.querySelector('div');


    modalContent.classList.remove('animate__zoomIn');
    modalContent.classList.add('animate__zoomOut');
    setTimeout(() => {
        modal.classList.add("hidden");
        modal.classList.remove("flex", "modal-active");

        modalContent.classList.remove('animate__zoomOut');
        modalContent.classList.add('animate__zoomIn'); // Reset for next time
        document.body.style.overflow = 'auto';
    }, 300);
}

// User Image handling in profile_setting page
function previewProfileImage(event) {
    console.log("Image Handling started in Profile Setting")
    const input = event.target;
    const preview = document.getElementById('image-preview');
    if (input.files && input.files[0]) {
        const reader = new FileReader();
        reader.onload = function (e) {
            preview.src = e.target.result;
            preview.classList.remove('animate__animated', 'animate__pulse');
            void preview.offsetWidth;
            preview.classList.add('animate__animated', 'animate__pulse');
        }
        reader.readAsDataURL(input.files[0]);
    }
}


document.addEventListener("DOMContentLoaded", () => {
    const timerElement = document.getElementById("timer");

    // Check karo ki kya hum Reset Password waale page par hi hain?
    if (timerElement) {
        console.log("OTP Timer Started...");

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

                // Tailwind Classes Replace (Bootstrap ki jagah)
                timerElement.parentElement.classList.remove('bg-yellow-100', 'text-yellow-700');
                timerElement.parentElement.classList.add('bg-red-100', 'text-red-700', 'border-red-200');

                // Submit button gayab
                const submitBtn = document.getElementById("submit-btn");
                if (submitBtn) {
                    submitBtn.classList.add('hidden'); // Tailwind 'hidden' use karo
                }

                // Resend container dikhao
                const resendBox = document.getElementById("resend-container");
                if (resendBox) {
                    resendBox.style.display = 'block';
                    resendBox.classList.add('animate__animated', 'animate__fadeIn');
                }
            }
            time--;
        }, 1000);
    } else {
        // Agar page change ho jaye toh timer clear kar dena chahiye
        localStorage.removeItem("otp_timer");
    }
});


// for email-modal send bulk email
function updateFileName(input) {
    const display = document.getElementById('file-name-display');
    if (input.files && input.files[0]) {
        display.innerText = "Selected: " + input.files[0].name;
        display.className = "text-blue-400 text-sm font-bold uppercase tracking-widest animate__animated animate__pulse";
    }
}
//  RECENT ACTIVITY TABLE PLACEHOLDER 
function toggleActions() {
    const container = document.getElementById('actions-container');
    const btn = document.getElementById('toggle-btn');

    if (container.classList.contains('max-h-0')) {
        // Expand
        container.classList.remove('max-h-0', 'opacity-0');
        container.classList.add('max-h-[500px]', 'opacity-100'); // Height adjust kar lena content ke hisab se
        btn.innerText = 'Hide Actions';
    } else {
        // Collapse
        container.classList.add('max-h-0', 'opacity-0');
        container.classList.remove('max-h-[500px]', 'opacity-100');
        btn.innerText = 'View All';
    }
}
//Didabled button in email blast
function disableButton() {
    const btn = document.getElementById('submitBtn');
    const btnText = document.getElementById('btnText');
    const spinner = document.getElementById('spinner');

    // 1. Button  disable & Style change
    btn.disabled = true;
    btn.classList.add('cursor-not-allowed', 'opacity-90');

    // 2. Text update
    btnText.innerHTML = "Blasting Mails...";
    spinner.classList.remove('hidden');

    // 3. Form submit hone do
    return true;
}
// add-contact/ update contact button disable.
document.addEventListener('DOMContentLoaded', function() {
    // 1. Direct Button ki ID se pakdo (No confusion with other forms)
    const submitBtn = document.getElementById('submitBtn');
    const contactForm = submitBtn ? submitBtn.closest('form') : null;

    if (contactForm && submitBtn) {
        contactForm.addEventListener('submit', function(e) {
            // Check if form is actually valid (HTML5 validation check)
            if (contactForm.checkValidity()) {
                // Button ko freeze aur spin karao
                submitBtn.disabled = true;
                submitBtn.classList.add('opacity-70', 'cursor-not-allowed', 'pointer-events-none');
                
                // Icon aur Text badlo
                submitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin mr-2"></i> PROCESSING...';
                
                console.log("Form locked! Uploading to Cloudinary...");
            }
        });
    }
});

document.addEventListener('DOMContentLoaded', function() {
    // 1. Direct ID se select karo (No confusion)
    const fileInput = document.getElementById('maxFile');
    const form = fileInput ? fileInput.closest('form') : null;

    if (form && fileInput) {
        console.log("File size checker initialized...");

        form.addEventListener('submit', function(e) {
            if (fileInput.files && fileInput.files.length > 0) {
                // Correct access: files[0].size
                const fileSizeInBytes = fileInput.files[0].size;
                const fileSizeInMB = fileSizeInBytes / (1024 * 1024);
                
                console.log("Actual File Size: " + fileSizeInMB.toFixed(2) + " MB");

                if (fileSizeInMB > 5) {
                    Swal.fire({
                        icon: 'error',
                        title:"Oops...",
                        text: 'File size is too big! Please upload under 5MB.',
                        width: '400px',
                        padding: '1.5rem',
                        background: '#1f2937',
                        color: '#fff',
                        timer:2000,
                        timerProgressBar: true,
                        confirmButtonColor: '#3b82f6',
                    });
                    
                    e.preventDefault(); 
                    return fileInput.value = "";
                }
            }
        });
    } else {
        console.error("Form or File Input not found! Check IDs.");
    }
});

