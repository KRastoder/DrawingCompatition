import { api, createAvatar } from "./api.js";

const NAV_ITEMS = [
    { key: "home", href: "/", label: "Home" },
    { key: "new", href: "/newdrawing", label: "Submit Drawing" },
    { key: "mine", href: "/my-drawings", label: "My Drawings" },
    { key: "profile", href: "/profile", label: "My Profile" }
];

const MARQUEE_TEXT = "★ WELCOME TO THE DRAWING COMPETITION ★ DRAW ★ VOTE ★ WIN ★ BEST VIEWED AT 1024×768 ★ NO POP-UPS (PROBABLY) ★";

let activeKey = null;

export async function initShell(active) {
    activeKey = active;
    const response = await api("/api/auth/me");
    if (!response.ok) {
        window.location.href = "/login";
        throw new Error("Not signed in.");
    }

    const user = await response.json();
    renderChrome(active, user);
    return user;
}

export function updateHeaderUser(user) {
    renderHeader(activeKey, user);
}

export function addSidebarWidget(title, content, className) {
    let extra = document.getElementById("sidebar-extra");
    if (!extra) {
        const sidebar = document.getElementById("site-sidebar");
        if (!sidebar) {
            return null;
        }
        extra = document.createElement("div");
        extra.id = "sidebar-extra";
        extra.className = "sidebar-extra";
        sidebar.appendChild(extra);
    }

    const widget = createWidget(title, content, className);
    extra.appendChild(widget);
    return widget;
}

function renderChrome(active, user) {
    renderHeader(active, user);
    renderSidebar(active);
    renderFooter();
    ensureMarquee();
}

function renderHeader(active, user) {
    const header = document.getElementById("app-header");
    if (!header) {
        return;
    }

    header.innerHTML = `
        <div class="banner">
            <div class="banner-stars" aria-hidden="true">✦ ✧ ★ ✦ ✧ ★ ✦</div>
            <a class="banner-logo" href="/">
                <span class="banner-kicker">★ a cozy corner of the web for doodles ★</span>
                <span class="banner-title">The Drawing Competition</span>
                <span class="banner-sub">Draw • Vote • Win • Repeat</span>
            </a>
            <div class="banner-meta">
                <span class="banner-badge">Est. 2010</span>
                <span class="banner-badge">Best viewed at 1024×768</span>
            </div>
        </div>
        <div class="utility-bar">
            <a class="user-chip" href="/profile" title="View your profile"></a>
            <span class="utility-welcome">Welcome back, <a class="utility-user" href="/profile"></a>!</span>
            <span class="utility-links">
                <a href="/">Home</a>
                <span class="utility-sep" aria-hidden="true">|</span>
                <a href="/my-drawings">My Drawings</a>
                <span class="utility-sep" aria-hidden="true">|</span>
                <a href="/profile">Profile</a>
                <span class="utility-sep" aria-hidden="true">|</span>
                <button type="button" class="link-button" id="logout-button">Log out</button>
            </span>
        </div>
        <nav class="site-nav" aria-label="Main navigation"></nav>
    `;

    const nav = header.querySelector(".site-nav");
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

    const userLink = header.querySelector(".utility-user");
    userLink.textContent = user.displayName;

    header.querySelector("#logout-button").addEventListener("click", async function () {
        try {
            await api("/api/auth/logout", { method: "POST" });
        } catch (ignored) {
            // Even if the request fails, send the user back to the sign-in page.
        }
        window.location.href = "/login";
    });
}

function renderSidebar(active) {
    const sidebar = document.getElementById("site-sidebar");
    if (!sidebar) {
        return;
    }

    sidebar.replaceChildren();
    sidebar.appendChild(createWidget("★ Navigation ★", navigationWidget(active), "widget-nav"));
    sidebar.appendChild(createWidget("★ About This Site ★", aboutWidget(), "widget-about"));
    sidebar.appendChild(createWidget("★ Now Playing ★", nowPlayingWidget(), "widget-now"));

    const extra = document.createElement("div");
    extra.id = "sidebar-extra";
    extra.className = "sidebar-extra";
    sidebar.appendChild(extra);

    sidebar.appendChild(createWidget("★ Cool Links ★", coolLinksWidget(), "widget-links"));
    sidebar.appendChild(createWidget("★ Webring ★", webringWidget(), "widget-webring"));
    sidebar.appendChild(createWidget("★ Site Info ★", siteInfoWidget(), "widget-info"));
    sidebar.appendChild(createWidget("★ Under Construction ★", constructionWidget(), "widget-construction"));
}

function renderFooter() {
    const footer = document.getElementById("site-footer");
    if (!footer) {
        return;
    }

    footer.innerHTML = `
        <div class="footer-columns">
            <div>
                <h4>About</h4>
                <p>A hand-coded corner of the web where artists post their doodles and strangers vote on them. Every drawing belongs to its creator.</p>
            </div>
            <div>
                <h4>Site Map</h4>
                <ul>
                    <li><a href="/">» Latest Drawings</a></li>
                    <li><a href="/newdrawing">» Submit a Drawing</a></li>
                    <li><a href="/my-drawings">» My Drawings</a></li>
                    <li><a href="/profile">» My Profile</a></li>
                </ul>
            </div>
            <div>
                <h4>Credits</h4>
                <p>Made with HTML, CSS, and too much free time. No cookies were harmed in the making of this website.</p>
            </div>
        </div>
        <div class="footer-badges">
            <span class="web-badge badge-blue">NEOCITIES</span>
            <span class="web-badge badge-pink">GIFCITIES</span>
            <span class="web-badge badge-gold">HTML 4.01</span>
            <span class="web-badge badge-green">CSS 3</span>
            <span class="web-badge badge-purple">1024×768</span>
            <span class="web-badge badge-grey">MS PAINT</span>
        </div>
        <p class="footer-copy">© 2010 The Drawing Competition. All drawings belong to their artists.</p>
        <a class="footer-top" href="#top">↑ Back to top</a>
    `;
}

function createWidget(title, content, className) {
    const widget = document.createElement("section");
    widget.className = "widget" + (className ? " " + className : "");

    const heading = document.createElement("h3");
    heading.className = "widget-title";
    heading.textContent = title;

    const body = document.createElement("div");
    body.className = "widget-body";
    if (content instanceof Node) {
        body.appendChild(content);
    } else {
        body.innerHTML = content;
    }

    widget.append(heading, body);
    return widget;
}

function navigationWidget(active) {
    const list = document.createElement("ul");
    list.className = "widget-list nav-list";
    NAV_ITEMS.forEach(function (item) {
        const entry = document.createElement("li");
        const link = document.createElement("a");
        link.href = item.href;
        link.textContent = "» " + item.label;
        if (item.key === active) {
            link.classList.add("sidebar-active");
        }
        entry.appendChild(link);
        list.appendChild(entry);
    });
    return list;
}

function aboutWidget() {
    return `
        <p>A cozy little corner of the web where artists post their doodles and strangers vote on them.</p>
        <p class="tiny">Est. 2010 · made with HTML, CSS, and too much free time.</p>
    `;
}

function nowPlayingWidget() {
    return `
        <div class="now-playing">
            <span class="cassette" aria-hidden="true">▣</span>
            <div>
                <strong>♫ dial-up modem handshake</strong><br>
                <span class="tiny">Mood: ✎ creative</span>
            </div>
        </div>
    `;
}

function coolLinksWidget() {
    return `
        <ul class="widget-list">
            <li><a href="https://neocities.org" target="_blank" rel="noopener">» Neocities</a></li>
            <li><a href="https://gifcities.org" target="_blank" rel="noopener">» GifCities</a></li>
            <li><a href="https://archive.org" target="_blank" rel="noopener">» Internet Archive</a></li>
            <li><a href="https://www.newgrounds.com" target="_blank" rel="noopener">» Newgrounds</a></li>
            <li><a href="https://www.deviantart.com" target="_blank" rel="noopener">» DeviantArt</a></li>
        </ul>
    `;
}

function webringWidget() {
    return `
        <div class="badge-row">
            <a class="web-badge badge-blue" href="https://neocities.org" target="_blank" rel="noopener">NEOCITIES</a>
            <a class="web-badge badge-pink" href="https://gifcities.org" target="_blank" rel="noopener">GIFCITIES</a>
            <span class="web-badge badge-gold">HTML 4.01</span>
            <span class="web-badge badge-green">CSS 3</span>
            <span class="web-badge badge-purple">1024×768</span>
            <span class="web-badge badge-grey">MS PAINT</span>
        </div>
    `;
}

function siteInfoWidget() {
    return `
        <ul class="widget-list plain">
            <li>Online since 2010</li>
            <li>Best viewed in Firefox 3.6</li>
            <li>No pop-ups! (probably)</li>
        </ul>
    `;
}

function constructionWidget() {
    return `
        <div class="construction">
            <p>★ More doodles coming soon! ★</p>
        </div>
    `;
}

function ensureMarquee() {
    if (document.querySelector(".retro-marquee")) {
        return;
    }

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

    const siteBody = document.querySelector(".site-body");
    if (siteBody && siteBody.parentNode) {
        siteBody.parentNode.insertBefore(marquee, siteBody);
    }
}
