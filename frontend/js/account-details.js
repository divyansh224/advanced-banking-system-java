/**
 * account-details.js
 * Shows a single account's details/balance and lets the owner update its status.
 */

requireAuth();

const params = new URLSearchParams(window.location.search);
const accountId = params.get("id");

document.addEventListener("DOMContentLoaded", () => {
    const user = Storage.getUser();
    document.getElementById("navUser").textContent = user ? `👤 ${user.fullName}` : "";
    injectAdminNavLink();

    if (!accountId) {
        showAlert("No account selected");
        setTimeout(() => window.location.href = "dashboard.html", 1000);
        return;
    }

    loadAccount();

    document.getElementById("updateAccountForm").addEventListener("submit", async (e) => {
        e.preventDefault();
        const btn = document.getElementById("updateBtn");
        btn.disabled = true;
        btn.textContent = "Saving...";
        try {
            await apiRequest(`/accounts/${accountId}`, {
                method: "PUT",
                body: { status: document.getElementById("statusSelect").value }
            });
            showAlert("Account updated", "success");
            loadAccount();
        } catch (err) {
            showAlert(err.message || "Update failed");
        } finally {
            btn.disabled = false;
            btn.textContent = "Save Changes";
        }
    });
});

async function loadAccount() {
    try {
        const acc = await apiRequest(`/accounts/${accountId}`);
        document.getElementById("statusBadge").textContent = acc.status;
        document.getElementById("statusBadge").className = `badge badge-${acc.status.toLowerCase()}`;
        document.getElementById("accountTypeLabel").textContent = acc.accountType;
        document.getElementById("balanceAmount").textContent = formatCurrency(acc.balance);
        document.getElementById("fullAccountNumber").textContent = `A/C: ${acc.accountNumber}`;
        document.getElementById("createdAt").textContent = formatDate(acc.createdAt);
        document.getElementById("ownerName").value = acc.ownerName || "-";
        document.getElementById("statusSelect").value = acc.status;

        document.getElementById("transferFromBtn").href = `transfer.html?from=${acc.accountNumber}`;
        document.getElementById("viewHistoryBtn").href = `transactions.html?id=${acc.id}`;
    } catch (err) {
        showAlert(err.message || "Could not load account");
    }
}
