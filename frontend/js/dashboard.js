/**
 * dashboard.js
 * Loads and renders the current user's accounts, and handles new-account creation.
 */

requireAuth();

document.addEventListener("DOMContentLoaded", () => {
    const user = Storage.getUser();
    document.getElementById("navUser").textContent = user ? `👤 ${user.fullName}` : "";
    injectAdminNavLink();
    document.getElementById("welcomeName").textContent = user ? user.fullName.split(" ")[0] : "there";

    loadAccounts();

    document.getElementById("newAccountForm").addEventListener("submit", async (e) => {
        e.preventDefault();
        const btn = document.getElementById("createAccountBtn");
        btn.disabled = true;
        btn.textContent = "Creating...";

        try {
            await apiRequest("/accounts", {
                method: "POST",
                body: {
                    accountType: document.getElementById("accountType").value,
                    initialDeposit: parseFloat(document.getElementById("initialDeposit").value || "0")
                }
            });
            bootstrap.Modal.getInstance(document.getElementById("newAccountModal")).hide();
            showAlert("New account created!", "success");
            loadAccounts();
        } catch (err) {
            showAlert(err.message || "Could not create account");
        } finally {
            btn.disabled = false;
            btn.textContent = "Create Account";
        }
    });
});

async function loadAccounts() {
    const row = document.getElementById("accountsRow");
    const noAccounts = document.getElementById("noAccounts");
    row.innerHTML = "";

    try {
        const accounts = await apiRequest("/accounts");

        if (!accounts || accounts.length === 0) {
            noAccounts.classList.remove("d-none");
            return;
        }
        noAccounts.classList.add("d-none");

        accounts.forEach(acc => {
            const badgeClass = `badge-${acc.status.toLowerCase()}`;
            const col = document.createElement("div");
            col.className = "col-md-6 col-lg-4";
            col.innerHTML = `
                <div class="card balance-card h-100">
                    <div class="card-body">
                        <div class="d-flex justify-content-between align-items-start mb-3">
                            <span class="badge ${badgeClass}">${acc.status}</span>
                            <span class="text-white-50">${acc.accountType}</span>
                        </div>
                        <div class="balance-amount">${formatCurrency(acc.balance)}</div>
                        <div class="account-number mt-2">•••• ${acc.accountNumber.slice(-4)}</div>
                        <div class="text-white-50 small mt-1">Full A/C: ${acc.accountNumber}</div>
                        <div class="d-flex gap-2 mt-3">
                            <a href="account-details.html?id=${acc.id}" class="btn btn-light btn-sm flex-fill">Details</a>
                            <a href="transfer.html?from=${acc.accountNumber}" class="btn btn-outline-light btn-sm flex-fill">Transfer</a>
                        </div>
                    </div>
                </div>`;
            row.appendChild(col);
        });
    } catch (err) {
        showAlert(err.message || "Could not load accounts");
    }
}
