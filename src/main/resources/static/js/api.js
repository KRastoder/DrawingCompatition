let csrf = null;

export function todayISO() {
    const now = new Date();
    const offset = now.getTimezoneOffset();
    return new Date(now.getTime() - offset * 60 * 1000).toISOString().slice(0, 10);
}

export function formatDate(iso) {
    return new Date(iso).toLocaleString(undefined, {
        dateStyle: "medium",
        timeStyle: "short"
    });
}

export function initials(name) {
    const parts = String(name || "").trim().split(/\s+/).filter(Boolean);
    if (parts.length === 0) {
        return "?";
    }
    if (parts.length === 1) {
        return parts[0].slice(0, 2).toUpperCase();
    }
    return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
}

export function createAvatar(user, extraClass) {
    const avatar = document.createElement("span");
    avatar.className = "avatar" + (extraClass ? " " + extraClass : "");

    const label = user ? user.displayName : "";
    if (user && user.avatarUrl) {
        const image = document.createElement("img");
        image.src = user.avatarUrl;
        image.alt = "";
        image.loading = "lazy";
        image.addEventListener("error", function () {
            avatar.replaceChildren(document.createTextNode(initials(label)));
        });
        avatar.appendChild(image);
    } else {
        avatar.textContent = initials(label);
    }

    return avatar;
}

export function readCookie(name) {
    const prefix = name + "=";
    const parts = document.cookie ? document.cookie.split("; ") : [];
    for (let index = 0; index < parts.length; index += 1) {
        if (parts[index].indexOf(prefix) === 0) {
            return decodeURIComponent(parts[index].substring(prefix.length));
        }
    }
    return null;
}

async function ensureCsrf(force) {
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

export async function api(path, options) {
    const settings = Object.assign({ credentials: "same-origin" }, options || {});
    const method = (settings.method || "GET").toUpperCase();
    const headers = new Headers(settings.headers || {});

    if (method !== "GET" && method !== "HEAD") {
        const token = await ensureCsrf(false);
        headers.set(token.headerName, token.token);
    }
    settings.headers = headers;

    const response = await fetch(path, settings);
    if (response.status === 401) {
        window.location.href = "/login";
        throw new Error("Your session expired. Please sign in again.");
    }
    return response;
}

export async function readProblem(response) {
    let message = "Something went wrong. Please try again.";
    try {
        const problem = await response.json();
        if (problem && problem.detail) {
            message = problem.detail;
        }
        if (problem && Array.isArray(problem.errors) && problem.errors.length > 0) {
            message = message + " " + problem.errors.join(" ");
        }
    } catch (ignored) {
        // Keep the fallback message.
    }
    return message;
}

export function setStatus(element, message, isError) {
    if (!element) {
        return;
    }
    if (!message) {
        element.classList.add("hidden");
        element.textContent = "";
        return;
    }
    element.textContent = message;
    element.classList.remove("hidden", "error", "success");
    element.classList.add(isError ? "error" : "success");
}
