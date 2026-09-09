/**
 * admin-fraud.js
 * Lists transfers currently PENDING_REVIEW (HIGH/CRITICAL risk) and lets an
 * admin approve (releases the funds) or reject (finalizes as FAILED, no
 * funds ever moved) each one.
 */

requireAuth();

document.addEventListener("DOMContentLoaded", () => {
    const user = Storage.getUser();
    document.getElementById("navUser").textContent = user ? `👤 ${user.fullName}` : "";

    if (!user || user.role !== "ROLE_ADMIN") {
        document.getElementById("notAdminNotice").classList.remove("d-none");
        document.querySelector(".card-elevated").classList.add("d-none");
        return;
    }

    loadPendingReviews();
});

async function loadPendingReviews() {
    const tbody = document.getElementById("reviewBody");
    const emptyState = document.getElementById("emptyState");
    tbody.innerHTML = "";

    try {
        const items = await apiRequest("/admin/fraud/pending");

        if (!items || items.length === 0) {
            emptyState.classList.remove("d-none");
            return;
        }
        emptyState.classList.add("d-none");

        items.forEach(item => {
            const row = document.createElement("tr");
            row.innerHTML = `
                <td class="text-muted small">${item.transactionRef.slice(0, 8)}...</td>
                <td>${item.fromAccountNumber}</td>
                <td>${item.toAccountNumber}</td>
                <td class="fw-semibold">${formatCurrency(item.amount)}</td>
                <td>${item.riskScore}</td>
                <td><span class="badge bg-${riskBadgeColor(item.riskLevel)}">${item.riskLevel}</span></td>
                <td class="small">${item.fraudReason}</td>
                <td>${formatDate(item.timestamp)}</td>
                <td>
                    <div class="btn-group btn-group-sm">
                        <button class="btn btn-accent" onclick="reviewTransaction(${item.transactionId}, 'approve')">Approve</button>
                        <button class="btn btn-outline-danger" onclick="reviewTransaction(${item.transactionId}, 'reject')">Reject</button>
                    </div>
                </td>
            `;
            tbody.appendChild(row);
        });
    } catch (err) {
        showAlert(err.message || "Could not load the fraud review queue");
    }
}

function riskBadgeColor(level) {
    return level === "CRITICAL" ? "danger" : "warning";
}

async function reviewTransaction(transactionId, action) {
    try {
        await apiRequest(`/admin/fraud/${transactionId}/${action}`, { method: "POST" });
        showAlert(action === "approve" ? "Transfer approved and funds released." : "Transfer rejected.", "success");
        await loadPendingReviews();
    } catch (err) {
        showAlert(err.message || `Could not ${action} this transaction`);
    }
}
