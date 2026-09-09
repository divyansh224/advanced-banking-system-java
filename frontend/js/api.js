/**
 * api.js
 * Small fetch wrapper shared by every page: attaches the JWT, handles
 * JSON parsing, and centralizes 401 handling (redirect to login).
 */

const API_BASE = "http://localhost:8080/api";

const Storage = {
    getToken: () => localStorage.getItem("banking_token"),
    setToken: (t) => localStorage.setItem("banking_token", t),
    getUser: () => JSON.parse(localStorage.getItem("banking_user") || "null"),
    setUser: (u) => localStorage.setItem("banking_user", JSON.stringify(u)),
    clear: () => {
        localStorage.removeItem("banking_token");
        localStorage.removeItem("banking_user");
    }
};

async function apiRequest(path, { method = "GET", body = null, auth = true } = {}) {
    const headers = { "Content-Type": "application/json" };

    if (auth) {
        const token = Storage.getToken();
        if (token) headers["Authorization"] = `Bearer ${token}`;
    }

    const response = await fetch(`${API_BASE}${path}`, {
        method,
        headers,
        body: body ? JSON.stringify(body) : null
    });

    if (response.status === 204) return null;

    let data = null;
    try {
        data = await response.json();
    } catch (e) {
        // no body
    }

    if (!response.ok) {
        if (response.status === 401 && auth) {
            Storage.clear();
            window.location.href = "login.html";
        }
        const message = (data && (data.message || data.error)) || `Request failed (${response.status})`;
        const err = new Error(message);
        err.details = data;
        throw err;
    }

    return data;
}

function requireAuth() {
    if (!Storage.getToken()) {
        window.location.href = "login.html";
        return;
    }

    window.addEventListener("pageshow",(event)=>{
        if(event.persisted && !Storage.getToken())
        {
            window.location.href="login.html";
        }
    });

}

function showAlert(message, type = "danger") {
    const containerId = "alertContainer";
    let container = document.getElementById(containerId);
    if (!container) {
        container = document.createElement("div");
        container.id = containerId;
        document.body.appendChild(container);
    }
    const alertEl = document.createElement("div");
    alertEl.className = `alert alert-${type} alert-dismissible fade show alert-fixed shadow`;
    alertEl.role = "alert";
    alertEl.innerHTML = `${message}<button type="button" class="btn-close" data-bs-dismiss="alert"></button>`;
    container.appendChild(alertEl);
    setTimeout(() => alertEl.remove(), 5000);
}

function formatCurrency(amount) {
    const n = Number(amount || 0);
    return n.toLocaleString("en-US", { style: "currency", currency: "USD" });
}

function formatDate(dateStr) {
    if (!dateStr) return "-";
    const d = new Date(dateStr);
    return d.toLocaleString();
}

function logout() {
    Storage.clear();
    window.location.href = "login.html";
}

/** Adds a "Fraud Review" nav link for admins only. Call after setting #navUser on any authenticated page. */
function injectAdminNavLink() {
    const user = Storage.getUser();
    if (!user || user.role !== "ROLE_ADMIN") return;
    const navUserEl = document.getElementById("navUser");
    const navUserLi = navUserEl ? navUserEl.closest("li") : null;
    if (!navUserLi || !navUserLi.parentNode) return;
    const li = document.createElement("li");
    li.className = "nav-item";
    li.innerHTML = '<a class="nav-link" href="admin-fraud.html">🛡️ Fraud Review</a>';
    navUserLi.parentNode.insertBefore(li, navUserLi);
}
