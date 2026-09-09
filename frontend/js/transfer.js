/**
 * transfer.js
 * Populates the "from account" dropdown, shows a confirmation modal,
 * then submits the transfer to POST /api/transfers.
 */

requireAuth();

let myAccounts = [];
let pendingTransfer = null;

document.addEventListener("DOMContentLoaded", async () => {
    const user = Storage.getUser();
    document.getElementById("navUser").textContent = user ? `👤 ${user.fullName}` : "";
    injectAdminNavLink();

    await loadMyAccounts();

    const preselect = new URLSearchParams(window.location.search).get("from");
    if (preselect) {
        document.getElementById("fromAccount").value = preselect;
        updateBalanceHint();
    }

    document.getElementById("fromAccount").addEventListener("change", updateBalanceHint);

    document.getElementById("transferForm").addEventListener("submit", (e) => {
        e.preventDefault();
        openConfirmation();
    });

    document.getElementById("confirmBtn").addEventListener("click", submitTransfer);
});

async function loadMyAccounts() {
    try {
        myAccounts = await apiRequest("/accounts");
        const select = document.getElementById("fromAccount");
        select.innerHTML = myAccounts.map(a =>
            `<option value="${a.accountNumber}">${a.accountType} •••• ${a.accountNumber.slice(-4)} (${formatCurrency(a.balance)})</option>`
        ).join("");
        updateBalanceHint();
    } catch (err) {
        showAlert(err.message || "Could not load accounts");
    }
}

function updateBalanceHint() {
    const selected = document.getElementById("fromAccount").value;
    const acc = myAccounts.find(a => a.accountNumber === selected);
    document.getElementById("fromBalanceHint").textContent =
        acc ? `Available balance: ${formatCurrency(acc.balance)}` : "";
}

function openConfirmation() {
    const fromAccountNumber = document.getElementById("fromAccount").value;
    const toAccountNumber = document.getElementById("toAccountNumber").value.trim();
    const amount = parseFloat(document.getElementById("amount").value);
    const description = document.getElementById("description").value.trim();

    if (!fromAccountNumber || !toAccountNumber || !amount || amount <= 0) {
        showAlert("Please fill in all required fields correctly");
        return;
    }
    if (fromAccountNumber === toAccountNumber) {
        showAlert("Sender and receiver accounts must be different");
        return;
    }

    pendingTransfer = { fromAccountNumber, toAccountNumber, amount, description };

    document.getElementById("confirmBody").innerHTML = `
        <p>You're about to transfer <strong>${formatCurrency(amount)}</strong></p>
        <p>From: <strong>${fromAccountNumber}</strong><br>To: <strong>${toAccountNumber}</strong></p>
        ${description ? `<p class="text-muted">Note: ${description}</p>` : ""}
    `;

    new bootstrap.Modal(document.getElementById("confirmModal")).show();
}

async function submitTransfer() {
    if (!pendingTransfer) return;
    const btn = document.getElementById("confirmBtn");
    btn.disabled = true;
    btn.textContent = "Sending...";

    try {
        const result = await apiRequest("/transfers", { method: "POST", body: pendingTransfer });
        bootstrap.Modal.getInstance(document.getElementById("confirmModal")).hide();

        if (result.status === "PENDING") {
            showAlert("This transfer was flagged for review due to unusual activity and is awaiting admin approval. No funds have moved yet.", "warning");
        } else {
            showAlert("Transfer completed successfully!", "success");
        }
        document.getElementById("transferForm").reset();
        await loadMyAccounts();
    } catch (err) {
        showAlert(err.message || "Transfer failed");
    } finally {
        btn.disabled = false;
        btn.textContent = "Confirm & Send";
        pendingTransfer = null;
    }
}
