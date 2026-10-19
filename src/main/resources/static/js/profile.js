import { api, createAvatar, readProblem, setStatus } from "./api.js";
import { initShell, updateHeaderUser } from "./shell.js";

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

const MAX_AVATAR_BYTES = 2 * 1024 * 1024;

let currentUser = null;
let profile = null;
let isOwnProfile = false;

function renderAvatar() {
    avatarBox.replaceChildren(createAvatar(profile, "avatar-large"));
    removeButton.classList.toggle("hidden", !profile.avatarUrl);
}

function renderProfile() {
    displayName.textContent = profile.displayName;
    username.textContent = "@" + profile.username;
    heading.textContent = isOwnProfile ? "Your profile" : profile.displayName + "'s profile";
    subtitle.textContent = isOwnProfile
        ? "Upload a picture and tell people about your art."
        : "Artist profile";

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

async function init() {
    currentUser = await initShell("profile");

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
}

init().catch(function (error) {
    heading.textContent = "Profile";
    subtitle.textContent = "We could not load this profile.";
    setStatus(statusBox, error.message, true);
});
