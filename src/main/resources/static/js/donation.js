function setAmount(val) {

    const amt = document.getElementById('donation-amount').value = val;

    console.log("Amount to be paid: " + amt);

}

async function startDonation() {
    const amount = document.getElementById('donation-amount').value;
    const payBtn = document.getElementById('pay-btn');
    const btnText = payBtn.querySelector('span');
    const btnIcon = payBtn.querySelector('i');

    // Frontend validation
    if (!amount || amount < 1) {
        Swal.fire({
            title: "Oops",
            text: "Minimum Rupees 1/- allowed",
            icon: "warning",
            background: '#1f2937',
            color: '#fff',
        });
        return;
    }
    console.log("PAY BUTTON CLICKED");
    payBtn.disabled = true;
    payBtn.classList.add('opacity-70', 'cursor-not-allowed');
    btnText.innerText = "Processing...";
    btnIcon.className = "fa-solid fa-circle-notch fa-spin ml-2";


    try {
        console.log("Calling create_order...");
        const response = await fetch('/user/create_order', {
            method: 'POST',
            credentials: 'same-origin',
            headers: { 'Content-Type': "application/json" },
            body: JSON.stringify({ amount: amount })
        });

        const orderData = await response.json();
        console.log("Response received");
        // Check if Backend sent an error (Like amount < 1)
        if (!response.ok || orderData.status === "error") {
            Swal.fire({
                icon: 'error',
                title: 'Error!',
                text: orderData.message || "Something went wrong",
                background: '#1f2937',
                color: '#fff'
            });
            return;
        }

        const options = {
            "key": orderData.keyId,
            "amount": orderData.amount,
            "currency": "INR",
            "name": "SCM Project",
            "order_id": orderData.id,
            "handler": function (response) {
                console.log("Payment Success:", response);

                Swal.fire({
                    icon: 'success',
                    title: 'Payment Successful',
                    text: 'Processing your payment confirmation...',
                    color: '#fff',
                    timer: 2000,
                    timerProgressBar: true,
                    showConfirmButton: false
                }).then(() => {
                    window.location.href = "/user/dashboard_home";
                });

            },

            "modal": {
                "ondismiss": function () {
                    console.log("Payment popup closed ❌");

                    // 🔥 Re-enable button
                    payBtn.disabled = false;
                    payBtn.classList.remove('opacity-70', 'cursor-not-allowed');
                    btnText.innerText = "Proceed to Pay";
                    btnIcon.className = "fa-solid fa-arrow-right group-hover:translate-x-1 transition";

                    Swal.fire({
                        icon: 'info',
                        title: 'Payment Cancelled',
                        text: 'You closed the payment window.',
                        background: '#1f2937',
                        color: '#fff',
                        timer: 1000,
                        timerProgressBar: true,
                        showConfirmButton: false
                    });
                }
            },
            "prefill": {
                "name": orderData.userName,
                "email": orderData.userEmail
            }
        };

        const rzp = new Razorpay(options);
        rzp.open();

    } catch (error) {
        Swal.fire({
            icon: 'error',
            title: 'Network Error!',
            text: "Connecting to Server Failed",
            background: '#1f2937',
            color: '#fff'
        });
        payBtn.disabled = false;
        payBtn.classList.remove('opacity-70', 'cursor-not-allowed');
        btnText.innerText = "Proceed to Pay";
        btnIcon.className = "fa-solid fa-arrow-right group-hover:translate-x-1 transition";
    }
}