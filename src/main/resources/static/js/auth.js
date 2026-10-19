(function () {
    "use strict";

    const tabLogin = document.getElementById("tab-login");
    const tabRegister = document.getElementById("tab-register");
    const loginForm = document.getElementById("login-form");
    const registerForm = document.getElementById("register-form");
    const errorBox = document.getElementById("auth-error");
    const infoBox = document.getElementById("auth-info");

    let csrf = null;

    function showError(message) {
        errorBox.textContent = message;
        errorBox.classList.remove("hidden");
        infoBox.classList.add("hidden");
    }

    function showInfo(message) {
        infoBox.textContent = message;
        infoBox.classList.remove("hidden");
        errorBox.classList.add("hidden");
    }

    function clearMessages() {
        errorBox.classList.add("hidden");
        infoBox.classList.add("hidden");
    }

    function readCookie(name) {
        const prefix = name + "=";
        const parts = document.cookie ? document.cookie.split("; ") : [];
        for (let index = 0; index < parts.length; index += 1) {
            if (parts[index].indexOf(prefix) === 0) {
                return decodeURIComponent(parts[index].substring(prefix.length));
            }
        }
        return null;
    }

    async function loadCsrf(force) {
        if (csrf && !force) {
            return csrf;
        }

        const response = await fetch("/api/auth/csrf", { credentials: "same-origin" });
        if (!response.ok) {
            throw new Error("Could not prepare a secure session. Please refresh the page.");
        }

        const info = await response.json();
        const token = info.token || readCookie(info.cookieName);
        if (!token) {
            throw new Error("Could not prepare a secure session. Please refresh the page.");
        }

        csrf = { headerName: info.headerName, token: token };
        return csrf;
    }

    async function postJson(url, payload) {
        const token = await loadCsrf(false);
        const response = await fetch(url, {
            method: "POST",
            credentials: "same-origin",
            headers: {
                "Content-Type": "application/json",
                [token.headerName]: token.token
            },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            return response.json().catch(function () {
                return {};
            });
        }

        let message = "Something went wrong. Please try again.";
        try {
            const problem = await response.json();
            if (problem && problem.detail) {
                message = problem.detail;
            }
            if (problem && Array.isArray(problem.errors) && problem.errors.length > 0) {
                message = problem.detail + " " + problem.errors.join(" ");
            }
        } catch (ignored) {
            // Keep the fallback message.
        }
        throw new Error(message);
    }

    function selectTab(showLogin) {
        tabLogin.classList.toggle("active", showLogin);
        tabRegister.classList.toggle("active", !showLogin);
        tabLogin.setAttribute("aria-selected", String(showLogin));
        tabRegister.setAttribute("aria-selected", String(!showLogin));
        loginForm.hidden = !showLogin;
        registerForm.hidden = showLogin;
        clearMessages();
    }

    tabLogin.addEventListener("click", function () {
        selectTab(true);
    });

    tabRegister.addEventListener("click", function () {
        selectTab(false);
    });

    loginForm.addEventListener("submit", async function (event) {
        event.preventDefault();
        clearMessages();

        const username = document.getElementById("login-username").value.trim();
        const password = document.getElementById("login-password").value;

        if (!username || !password) {
            showError("Enter your username and password.");
            return;
        }

        const submit = document.getElementById("login-submit");
        submit.disabled = true;

        try {
            await postJson("/api/auth/login", { username: username, password: password });
            window.location.href = "/";
        } catch (error) {
            showError(error.message);
        } finally {
            submit.disabled = false;
        }
    });

    registerForm.addEventListener("submit", async function (event) {
        event.preventDefault();
        clearMessages();

        const username = document.getElementById("register-username").value.trim();
        const displayName = document.getElementById("register-display-name").value.trim();
        const password = document.getElementById("register-password").value;

        if (!username || !displayName || !password) {
            showError("Fill in every field to create your account.");
            return;
        }

        const submit = document.getElementById("register-submit");
        submit.disabled = true;

        try {
            await postJson("/api/auth/register", {
                username: username,
                displayName: displayName,
                password: password
            });
            window.location.href = "/";
        } catch (error) {
            showError(error.message);
        } finally {
            submit.disabled = false;
        }
    });

    async function redirectIfSignedIn() {
        try {
            const response = await fetch("/api/auth/me", { credentials: "same-origin" });
            if (response.ok) {
                window.location.href = "/";
            }
        } catch (ignored) {
            // The sign-in form is still available if this check fails.
        }
    }

    loadCsrf(true).catch(function (error) {
        showError(error.message);
    });
    redirectIfSignedIn();
})();
