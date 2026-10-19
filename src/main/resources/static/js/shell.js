import { api, createAvatar } from "./api.js";

const NAV_ITEMS = [
    { key: "home", href: "/", label: "Home" },
    { key: "new", href: "/newdrawing", label: "New drawing" },
    { key: "mine", href: "/my-drawings", label: "My drawings" },
    { key: "profile", href: "/profile", label: "Profile" }
];

let activeKey = null;

const MARQUEE_TEXT = "★ Welcome to the Drawing Competition ★ Draw ★ Vote ★ Win ★ Best viewed at 1024×768 ★ No Flash required ★";

export async function initShell(active) {
    activeKey = active;
    const response = await api("/api/auth/me");
    if (!response.ok) {
        window.location.href = "/login";
        throw new Error("Not signed in.");
    }

    const user = await response.json();
    renderHeader(active, user);
    ensureRetroChrome();
    return user;
}

export function updateHeaderUser(user) {
    renderHeader(activeKey, user);
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
            <a class="user-chip" href="/profile" title="View your profile"></a>
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

    const chip = header.querySelector(".user-chip");
    chip.appendChild(createAvatar(user, "avatar-small"));
    const chipText = document.createElement("span");
    chipText.className = "user-name";
    chipText.append(document.createTextNode("Signed in as "));
    const name = document.createElement("strong");
    name.textContent = user.displayName;
    chipText.appendChild(name);
    chip.appendChild(chipText);

    header.querySelector("#logout-button").addEventListener("click", async function () {
        try {
            await api("/api/auth/logout", { method: "POST" });
        } catch (ignored) {
            // Even if the request fails, send the user back to the sign-in page.
        }
        window.location.href = "/login";
    });
}

function ensureRetroChrome() {
    if (!document.querySelector(".retro-marquee")) {
        const header = document.getElementById("app-header");
        if (header) {
            const marquee = document.createElement("div");
            marquee.className = "retro-marquee";
            marquee.setAttribute("aria-hidden", "true");

            const track = document.createElement("div");
            track.className = "retro-marquee-track";
            for (let index = 0; index < 2; index += 1) {
                const chunk = document.createElement("span");
                chunk.className = "retro-marquee-chunk";
                chunk.textContent = MARQUEE_TEXT;
                track.appendChild(chunk);
            }
            marquee.appendChild(track);

            header.insertAdjacentElement("afterend", marquee);
        }
    }

    if (!document.querySelector(".site-footer")) {
        const footer = document.createElement("footer");
        footer.className = "site-footer";
        footer.innerHTML = `
            <p class="footer-badges">
                <span class="web-badge">★ Est. 2010 ★</span>
                <span class="web-badge">Made with MS Paint</span>
                <span class="web-badge">CSS3 Powered</span>
                <span class="web-badge">Best viewed at 1024×768</span>
            </p>
        `;
        document.body.appendChild(footer);
    }
}
