import { api, createAvatar, formatDate, readProblem, setStatus } from "./api.js";
import { addSidebarWidget, initShell, updateHeaderUser } from "./shell.js";

const avatarBox = document.getElementById("profile-avatar");
const displayName = document.getElementById("profile-display-name");
const username = document.getElementById("profile-username");
const heading = document.getElementById("profile-heading");
const subtitle = document.getElementById("profile-subtitle");
const actions = document.getElementById("profile-actions");
const uploadButton = document.getElementById("upload-avatar-button");
const removeButton = document.getElementById("remove-avatar-button");
const avatarInput = document.getElementById("avatar-input");
const editSection = document.getElementById("profile-edit");
const readonlySection = document.getElementById("profile-readonly");
const bioInput = document.getElementById("profile-bio");
const bioCount = document.getElementById("bio-count");
const bioText = document.getElementById("profile-bio-text");
const saveBioButton = document.getElementById("save-bio-button");
const statusBox = document.getElementById("profile-status");

const drawingsBox = document.getElementById("profile-drawings");
const drawingsStatus = document.getElementById("profile-drawings-status");

const MAX_AVATAR_BYTES = 2 * 1024 * 1024;

let currentUser = null;
let profile = null;
let isOwnProfile = false;
let profileDrawings = [];
let memberCardBody = null;
let statsBody = null;

function renderAvatar() {
    avatarBox.replaceChildren(createAvatar(profile, "avatar-large"));
    removeButton.classList.toggle("hidden", !profile.avatarUrl);
}

function renderProfile() {
    displayName.textContent = profile.displayName;
    username.textContent = "@" + profile.username;
    heading.textContent = isOwnProfile ? "\u2605 Your Profile \u2605" : "\u2605 " + profile.displayName + "'s Profile \u2605";
    subtitle.textContent = isOwnProfile
        ? "Upload a picture and tell people about your art."
        : "Artist profile and latest doodles.";

    document.title = "\u2605 " + profile.displayName + " | The Drawing Competition \u2605";

    renderAvatar();

    if (isOwnProfile) {
        actions.classList.remove("hidden");
        editSection.classList.remove("hidden");
        readonlySection.classList.add("hidden");
        bioInput.value = profile.bio || "";
        bioCount.textContent = String(bioInput.value.length);
    } else {
        actions.classList.add("hidden");
        editSection.classList.add("hidden");
        readonlySection.classList.remove("hidden");

        const hasBio = profile.bio && profile.bio.length > 0;
        bioText.textContent = hasBio ? profile.bio : "This artist has not added a description yet.";
        bioText.classList.toggle("empty", !hasBio);
    }
}

function cacheBustedAvatar(avatarUrl) {
    return avatarUrl ? avatarUrl + "?v=" + Date.now() : null;
}

async function uploadAvatar(file) {
    if (file.size > MAX_AVATAR_BYTES) {
        setStatus(statusBox, "The profile picture must be 2 MB or smaller.", true);
        return;
    }

    const formData = new FormData();
    formData.append("image", file, file.name);

    uploadButton.disabled = true;
    try {
        const response = await api("/api/profile/avatar", { method: "PUT", body: formData });
        if (!response.ok) {
            throw new Error(await readProblem(response));
        }

        const updated = await response.json();
        profile.avatarUrl = cacheBustedAvatar(updated.avatarUrl);
        currentUser.avatarUrl = profile.avatarUrl;
        renderAvatar();
        renderMemberCard();
        updateHeaderUser(currentUser);
        setStatus(statusBox, "Profile picture updated.", false);
    } catch (error) {
        setStatus(statusBox, error.message, true);
    } finally {
        uploadButton.disabled = false;
        avatarInput.value = "";
    }
}

async function removeAvatar() {
    if (!window.confirm("Remove your profile picture?")) {
        return;
    }

    removeButton.disabled = true;
    try {
        const response = await api("/api/profile/avatar", { method: "DELETE" });
        if (!response.ok) {
            throw new Error(await readProblem(response));
        }

        profile.avatarUrl = null;
        currentUser.avatarUrl = null;
        renderAvatar();
        renderMemberCard();
        updateHeaderUser(currentUser);
        setStatus(statusBox, "Profile picture removed.", false);
    } catch (error) {
        setStatus(statusBox, error.message, true);
    } finally {
        removeButton.disabled = false;
    }
}

async function saveBio() {
    saveBioButton.disabled = true;
    try {
        const response = await api("/api/profile", {
            method: "PATCH",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ bio: bioInput.value })
        });
        if (!response.ok) {
            throw new Error(await readProblem(response));
        }

        const updated = await response.json();
        profile.bio = updated.bio;
        currentUser.bio = updated.bio;
        bioInput.value = updated.bio || "";
        bioCount.textContent = String(bioInput.value.length);
        renderMemberCard();
        setStatus(statusBox, "Description saved.", false);
    } catch (error) {
        setStatus(statusBox, error.message, true);
    } finally {
        saveBioButton.disabled = false;
    }
}

function setupEditing() {
    uploadButton.addEventListener("click", function () {
        avatarInput.click();
    });

    avatarInput.addEventListener("change", function () {
        const file = avatarInput.files && avatarInput.files[0];
        if (file) {
            uploadAvatar(file);
        }
    });

    removeButton.addEventListener("click", removeAvatar);
    saveBioButton.addEventListener("click", saveBio);

    bioInput.addEventListener("input", function () {
        bioCount.textContent = String(bioInput.value.length);
    });
}

function showMessage(element, message) {
    if (!message) {
        element.classList.add("hidden");
        element.textContent = "";
        return;
    }
    element.textContent = message;
    element.classList.remove("hidden");
}

function renderMemberCard() {
    if (!memberCardBody || !profile) {
        return;
    }

    memberCardBody.replaceChildren();

    const card = document.createElement("div");
    card.className = "profile-widget";

    const avatar = createAvatar(profile, "avatar-large profile-widget-avatar");
    const who = document.createElement("div");
    who.className = "profile-widget-who";

    const name = document.createElement("strong");
    name.textContent = profile.displayName;

    const handle = document.createElement("span");
    handle.className = "tiny";
    handle.textContent = "@" + profile.username;

    who.append(name, document.createElement("br"), handle);

    const bio = document.createElement("p");
    bio.className = "profile-widget-bio";
    bio.textContent = profile.bio || "No description yet.";

    card.append(avatar, who, bio);
    memberCardBody.appendChild(card);
}

function renderStats() {
    if (!statsBody) {
        return;
    }

    const totalVotes = profileDrawings.reduce(function (sum, drawing) {
        return sum + drawing.voteCount;
    }, 0);

    const list = document.createElement("ul");
    list.className = "widget-list plain stats-list";

    [
        ["Drawings", profileDrawings.length],
        ["Votes earned", totalVotes],
        ["Member", "@" + profile.username]
    ].forEach(function (entry) {
        const item = document.createElement("li");
        const label = document.createElement("span");
        label.textContent = entry[0] + ": ";
        const value = document.createElement("strong");
        value.textContent = String(entry[1]);
        item.append(label, value);
        list.appendChild(item);
    });

    statsBody.replaceChildren(list);
}

function drawingTitle(drawing) {
    const description = (drawing.description || "").trim();
    if (!description) {
        return "Untitled Drawing No. " + drawing.id;
    }
    const firstLine = description.split(/\r?\n/)[0].trim();
    return firstLine.length > 60 ? firstLine.slice(0, 57).trimEnd() + "..." : firstLine;
}

function renderDrawings() {
    drawingsBox.replaceChildren();

    if (profileDrawings.length === 0) {
        const empty = document.createElement("p");
        empty.className = "tiny";
        empty.textContent = isOwnProfile
            ? "You have not posted any drawings yet. Head to the studio and make one!"
            : "This artist has not posted any drawings yet.";
        drawingsBox.appendChild(empty);
        showMessage(drawingsStatus, "");
        return;
    }

    profileDrawings.slice(0, 6).forEach(function (drawing) {
        const post = document.createElement("article");
        post.className = "profile-post";

        const thumb = document.createElement("div");
        thumb.className = "profile-post-image";
        const image = document.createElement("img");
        image.src = drawing.imageUrl;
        image.alt = "Drawing by " + profile.displayName;
        image.loading = "lazy";
        thumb.appendChild(image);

        const info = document.createElement("div");
        info.className = "profile-post-info";

        const title = document.createElement("h3");
        const link = document.createElement("a");
        link.href = "/?drawing=" + drawing.id;
        link.textContent = drawingTitle(drawing);
        title.appendChild(link);

        const meta = document.createElement("p");
        meta.textContent = formatDate(drawing.createdAt) + " \u00b7 " +
            drawing.voteCount + (drawing.voteCount === 1 ? " vote" : " votes");

        info.append(title, meta);
        post.append(thumb, info);
        drawingsBox.appendChild(post);
    });

    if (profileDrawings.length > 6) {
        const more = document.createElement("p");
        more.className = "tiny";
        more.textContent = "Showing the 6 newest of " + profileDrawings.length + " drawings.";
        drawingsBox.appendChild(more);
    }

    showMessage(drawingsStatus, "");
}

async function loadProfileDrawings() {
    showMessage(drawingsStatus, "Loading drawings...");

    try {
        const response = await api("/api/drawings");
        if (response.status === 404) {
            profileDrawings = [];
        } else if (!response.ok) {
            throw new Error(await readProblem(response));
        } else {
            const all = await response.json();
            profileDrawings = all
                .filter(function (drawing) {
                    return drawing.owner.id === profile.id;
                })
                .sort(function (left, right) {
                    return new Date(right.createdAt) - new Date(left.createdAt);
                });
        }

        renderDrawings();
        renderStats();
    } catch (error) {
        showMessage(drawingsStatus, error.message);
        const paragraph = document.createElement("p");
        paragraph.className = "tiny";
        paragraph.textContent = "The drawing list could not be loaded.";
        drawingsBox.replaceChildren(paragraph);
    }
}

function setupSidebar() {
    const memberCard = addSidebarWidget("\u2605 Member Card \u2605", "<p class=\"tiny\">Loading...</p>", "widget-member");
    if (memberCard) {
        memberCardBody = memberCard.querySelector(".widget-body");
    }

    const stats = addSidebarWidget("\u2605 Profile Stats \u2605", "<p class=\"tiny\">Loading...</p>", "widget-profile-stats");
    if (stats) {
        statsBody = stats.querySelector(".widget-body");
    }

    const links = document.createElement("ul");
    links.className = "widget-list";
    links.innerHTML = `
        <li><a href="/">\u00bb Latest drawings</a></li>
        <li><a href="/newdrawing">\u00bb Submit a drawing</a></li>
        <li><a href="/my-drawings">\u00bb Manage my drawings</a></li>
        <li><a href="#top">\u00bb Back to top</a></li>
    `;
    addSidebarWidget("\u2605 Quick Links \u2605", links, "widget-quick-links");
}

async function init() {
    currentUser = await initShell("profile");
    setupSidebar();

    const match = window.location.pathname.match(/^\/profile\/(\d+)$/);
    const viewedId = match ? Number(match[1]) : currentUser.id;
    isOwnProfile = viewedId === currentUser.id;

    const response = await api(isOwnProfile ? "/api/profile" : "/api/users/" + viewedId);
    if (!response.ok) {
        throw new Error(await readProblem(response));
    }

    profile = await response.json();
    setupEditing();
    renderProfile();
    renderMemberCard();
    await loadProfileDrawings();
}

init().catch(function (error) {
    heading.textContent = "Profile";
    subtitle.textContent = "We could not load this profile.";
    setStatus(statusBox, error.message, true);
});
