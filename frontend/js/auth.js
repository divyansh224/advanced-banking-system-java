/**
 * auth.js
 * Handles the login and registration forms (login.html / register.html).
 */

document.addEventListener("DOMContentLoaded", () => {
    const loginForm = document.getElementById("loginForm");
    const registerForm = document.getElementById("registerForm");

    if (loginForm) {
        loginForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const btn = document.getElementById("loginBtn");
            const username = document.getElementById("username").value.trim();
            const password = document.getElementById("password").value;

            btn.disabled = true;
            btn.textContent = "Logging in...";

            try {
                const data = await apiRequest("/auth/login", {
                    method: "POST",
                    auth: false,
                    body: { username, password }
                });
                Storage.setToken(data.token);
                Storage.setUser(data);
                window.location.href = "dashboard.html";
            } catch (err) {
                showAlert(err.message || "Login failed");
            } finally {
                btn.disabled = false;
                btn.textContent = "Log In";
            }
        });
    }

    if (registerForm) {
        registerForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const btn = document.getElementById("registerBtn");

            const payload = {
                fullName: document.getElementById("fullName").value.trim(),
                username: document.getElementById("username").value.trim(),
                email: document.getElementById("email").value.trim(),
                phoneNumber: document.getElementById("phoneNumber").value.trim(),
                password: document.getElementById("password").value
            };

            btn.disabled = true;
            btn.textContent = "Creating account...";

            try {
                const data = await apiRequest("/auth/register", {
                    method: "POST",
                    auth: false,
                    body: payload
                });
                Storage.setToken(data.token);
                Storage.setUser(data);
                showAlert("Account created successfully!", "success");
                setTimeout(() => window.location.href = "dashboard.html", 800);
            } catch (err) {
                showAlert(err.message || "Registration failed");
            } finally {
                btn.disabled = false;
                btn.textContent = "Create Account";
            }
        });
    }
});
