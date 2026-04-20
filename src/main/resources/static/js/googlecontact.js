// 🔥 Button Click → Start OAuth Flow
function startGoogleImport() {

    const btn = document.getElementById("importBtn");
    const label = document.getElementById("importBtnText");

    if (btn) {
        btn.disabled = true;
        if (label) label.innerText = "Redirecting...";
        btn.classList.add("opacity-50", "cursor-not-allowed");
    }

    window.location.href = "/oauth2/authorization/google-contacts?prompt=consent";
}


// 🔥 Page Load → Resume Import Flow
window.onload = function () {

    const params = new URLSearchParams(window.location.search);
    const importStatus = params.get("import");

    if (importStatus !== "success") {
        
        sessionStorage.removeItem("importStarted");

        const btn = document.getElementById("importBtn");
        const label = document.getElementById("importBtnText");

        if (btn) {
            btn.disabled = false;
            if (label) label.innerText = "Import Google Contacts";
            btn.classList.remove("opacity-50", "cursor-not-allowed");
        }

        return; // 🔥 STOP here
    }
    
        //SUCCESS -> START IMPORT
        sessionStorage.setItem("importStarted", "true");

        document.getElementById("importOverlay").classList.remove("hidden");

        fetch("/user/test-api");

        startProgressPolling();

        //  Clean URL
        window.history.replaceState({}, document.title, "/user/dashboard");
    

};


// 🔥 Animation helper
function animate(el) {
    if (!el) return;
    el.classList.add("animate-pulse");
    setTimeout(() => el.classList.remove("animate-pulse"), 300);
}


// 🔥 Progress Polling
function startProgressPolling() {

    const interval = setInterval(async () => {

        try {
            const res = await fetch("/user/test-progress");
            const data = await res.json();

            const processedEl = document.getElementById("countProcessed");
            const savedEl = document.getElementById("countSaved");
            const skippedEl = document.getElementById("countSkipped");
            const percentEl = document.getElementById("progressPercent");

            // 🔹 Progress %
            let percent = data.total > 0
                ? Math.floor((data.processed / data.total) * 100)
                : 0;

            // 🔹 Update UI
            if (processedEl) {
                processedEl.innerText = data.processed;
                animate(processedEl);
            }

            if (savedEl) {
                savedEl.innerText = data.saved;
                animate(savedEl);
            }

            if (skippedEl) {
                skippedEl.innerText = data.skipped;
                animate(skippedEl);
            }

            if (percentEl) {
                percentEl.innerText = percent + "%";
            }

            document.getElementById("progressBar").style.width = percent + "%";

            // 🔹 Completion
            if (data.completed) {

                clearInterval(interval);

                setTimeout(() => {

                    document.getElementById("importOverlay").classList.add("hidden");

                    sessionStorage.removeItem("importStarted");

                    const btn = document.getElementById("importBtn");
                    const label = document.getElementById("importBtnText");
                    if (btn) {
                        btn.disabled = false;
                        if (label) label.innerText = "Import Google Contacts";
                        btn.classList.remove("opacity-50", "cursor-not-allowed");
                    }

                    Swal.fire({
                        icon: "success",
                        title: "Import Completed 🎉",
                        text: `Saved: ${data.saved}, Skipped: ${data.skipped}`,
                        color: '#fff',
                        background: '#1e293b',
                        timer: 3000,
                        timerProgressBar: true,
                        showConfirmButton: true,
                        customClass: {
                            htmlContainer: 'text-center'
                        }
                    });

                }, 800);
            }

        } catch (e) {

            clearInterval(interval);

            document.getElementById("importOverlay").classList.add("hidden");

            sessionStorage.removeItem("importStarted");

            Swal.fire({
                icon: "error",
                title: "Import Failed ❌",
                text: "Please try again"
            });
        }

    }, 800);
}