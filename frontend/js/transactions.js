/**
 * transactions.js
 * Lets the user pick one of their accounts and view its transaction history
 * (GET /api/transactions/{accountId}), including sender, receiver, type and status.
 */

requireAuth();

let accountsById = {};

document.addEventListener("DOMContentLoaded", async () => {
    const user = Storage.getUser();
    document.getElementById("navUser").textContent = user ? `👤 ${user.fullName}` : "";
    injectAdminNavLink();

    const select = document.getElementById("accountSelect");

    try {
        const accounts = await apiRequest("/accounts");
        accountsById = Object.fromEntries(accounts.map(a => [String(a.id), a]));
        if (accounts.length === 0) {
            document.getElementById("emptyState").classList.remove("d-none");
            document.getElementById("emptyState").textContent = "You don't have any accounts yet.";
            return;
        }

        select.innerHTML = accounts.map(a =>
            `<option value="${a.id}">${a.accountType} •••• ${a.accountNumber.slice(-4)}</option>`
        ).join("");

        const preselect = new URLSearchParams(window.location.search).get("id");
        if (preselect && accounts.some(a => String(a.id) === preselect)) {
            select.value = preselect;
        }

        select.addEventListener("change", () => loadTransactions(select.value));
        await loadTransactions(select.value);
    } catch (err) {
        showAlert(err.message || "Could not load accounts");
    }
});

async function loadTransactions(accountId) {
    const tbody = document.getElementById("txnBody");
    const emptyState = document.getElementById("emptyState");
    tbody.innerHTML = "";

    try {
        const transactions = await apiRequest(`/transactions/${accountId}`);

        if (!transactions || transactions.length === 0) {
            emptyState.classList.remove("d-none");
            emptyState.textContent = "No transactions yet for this account.";
            return;
        }
        emptyState.classList.add("d-none");

        const viewedAccountNumber = accountsById[String(accountId)]?.accountNumber;

        transactions.forEach(t => {
            const isCredit = t.toAccountNumber === viewedAccountNumber;
            const row = document.createElement("tr");
            row.innerHTML = `
                <td class="text-muted small">${t.transactionRef.slice(0, 8)}...</td>
                <td>${formatDate(t.timestamp)}</td>
                <td>${t.type}</td>
                <td>${t.fromAccountNumber || "-"}</td>
                <td>${t.toAccountNumber || "-"}</td>
                <td class="${isCredit ? "txn-credit" : "txn-debit"}">${isCredit ? "+" : "-"}${formatCurrency(t.amount)}</td>
                <td><span class="status-${t.status.toLowerCase()}">${t.status}</span></td>
            `;
            tbody.appendChild(row);
        });
    } catch (err) {
        showAlert(err.message || "Could not load transactions");
    }
}
