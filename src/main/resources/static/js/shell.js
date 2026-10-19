import { api } from "./api.js";

const NAV_ITEMS = [
    { key: "home", href: "/", label: "Home" },
    { key: "new", href: "/newdrawing", label: "New drawing" },
    { key: "mine", href: "/my-drawings", label: "My drawings" }
];

export async function initShell(active) {
    const response = await api("/api/auth/me");
    if (!response.ok) {
        window.location.href = "/login";
        throw new Error("Not signed in.");
    }

    const user = await response.json();
    renderHeader(active, user);
    return user;
}

function renderHeader(active, user) {
    const header = document.getElementById("app-header");
    if (!header) {
        return;
    }

    header.innerHTML = `
        <a class="brand" href="/">
            <span class="brand-mark" aria-hidden="true">DC</span>
            <span class="brand-name">Drawing Competition</span>
        </a>
        <nav class="app-nav" aria-label="Main navigation"></nav>
        <div class="header-actions">
            <span class="user-chip">Signed in as <strong></strong></span>
            <button type="button" class="btn btn-small" id="logout-button">Sign out</button>
        </div>
    `;

    const nav = header.querySelector(".app-nav");
    NAV_ITEMS.forEach(function (item) {
        const link = document.createElement("a");
        link.href = item.href;
        link.textContent = item.label;
        link.dataset.nav = item.key;
        if (item.key === active) {
            link.classList.add("active");
            link.setAttribute("aria-current", "page");
        }
        nav.appendChild(link);
    });

    header.querySelector(".user-chip strong").textContent = user.displayName;
    header.querySelector("#logout-button").addEventListener("click", async function () {
        try {
            await api("/api/auth/logout", { method: "POST" });
        } catch (ignored) {
            // Even if the request fails, send the user back to the sign-in page.
        }
        window.location.href = "/login";
    });
}
