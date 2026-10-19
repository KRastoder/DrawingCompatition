import { api, formatDate, readProblem, setStatus } from "./api.js";
import { addSidebarWidget, initShell } from "./shell.js";

const gallery = document.getElementById("mine-gallery");
const emptyState = document.getElementById("mine-empty");
const statusBox = document.getElementById("mine-status");

const MONTHS = ["JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"];

let currentUser = null;
let drawings = [];
let statsBody = null;

function drawingTitle(drawing) {
    const description = (drawing.description || "").trim();
    if (!description) {
        return "Untitled Drawing No. " + drawing.id;
    }
    const firstLine = description.split(/\r?\n/)[0].trim();
    return firstLine.length > 72 ? firstLine.slice(0, 69).trimEnd() + "..." : firstLine;
}

function createButton(label, className, handler) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "btn btn-small" + (className ? " " + className : "");
    button.textContent = label;
    button.addEventListener("click", handler);
    return button;
}

function dateBadge(drawing) {
    const date = new Date(drawing.createdAt);
    const badge = document.createElement("div");
    badge.className = "post-date";
    badge.setAttribute("aria-label", formatDate(drawing.createdAt));

    const month = document.createElement("span");
    month.className = "post-month";
    month.textContent = MONTHS[date.getMonth()];

    const day = document.createElement("span");
    day.className = "post-day";
    day.textContent = String(date.getDate()).padStart(2, "0");

    const year = document.createElement("span");
    year.className = "post-year";
    year.textContent = String(date.getFullYear());

    badge.append(month, day, year);
    return badge;
}

function createCard(drawing) {
    const card = document.createElement("article");
    card.className = "post manage-post";
    card.id = "drawing-" + drawing.id;

    const head = document.createElement("div");
    head.className = "post-head";

    const heading = document.createElement("div");
    heading.className = "post-heading";

    const title = document.createElement("h2");
    title.className = "post-title";
    const link = document.createElement("a");
    link.href = "/?drawing=" + drawing.id;
    link.textContent = drawingTitle(drawing);
    title.appendChild(link);

    const meta = document.createElement("p");
    meta.className = "post-meta";
    meta.textContent = formatDate(drawing.createdAt) + " \u00b7 " +
        drawing.voteCount + (drawing.voteCount === 1 ? " vote" : " votes") +
        " \u00b7 Drawing No. " + drawing.id;

    heading.append(title, meta);
    head.append(dateBadge(drawing), heading);

    const frame = document.createElement("div");
    frame.className = "post-image-frame";
    const image = document.createElement("img");
    image.className = "post-image manage-image";
    image.src = drawing.imageUrl;
    image.alt = "Drawing by " + drawing.owner.displayName;
    image.loading = "lazy";
    frame.appendChild(image);

    const description = document.createElement("p");
    const hasDescription = drawing.description && drawing.description.length > 0;
    description.className = "card-description post-description" + (hasDescription ? "" : " empty");
    description.textContent = hasDescription ? drawing.description : "No description yet - add one below!";

    const actions = document.createElement("div");
    actions.className = "card-actions manage-actions";

    const view = document.createElement("a");
    view.className = "btn btn-small btn-ghost";
    view.href = "/?drawing=" + drawing.id;
    view.textContent = "View on home";

    actions.append(
        createButton("Edit description", "", function () {
            showEditor(card, drawing);
        }),
        createButton("Replace image", "", function () {
            fileInput.click();
        }),
        createButton("Delete", "btn-danger", function () {
            deleteDrawing(drawing);
        }),
        view
    );

    const fileInput = document.createElement("input");
    fileInput.type = "file";
    fileInput.accept = "image/png,image/jpeg,image/webp";
    fileInput.hidden = true;
    fileInput.addEventListener("change", function () {
        replaceImage(card, drawing, fileInput);
    });

    card.append(head, frame, description, actions, fileInput);
    return card;
}

function showEditor(card, drawing) {
    if (card.querySelector(".inline-edit")) {
        return;
    }

    const description = card.querySelector(".card-description");
    description.classList.add("hidden");

    const form = document.createElement("form");
    form.className = "inline-edit";

    const textarea = document.createElement("textarea");
    textarea.maxLength = 2000;
    textarea.value = drawing.description || "";
    textarea.setAttribute("aria-label", "Drawing description");

    const save = document.createElement("button");
    save.type = "submit";
    save.className = "btn btn-primary btn-small";
    save.textContent = "Save";

    const cancel = createButton("Cancel", "", function () {
        form.remove();
        description.classList.remove("hidden");
    });

    const buttons = document.createElement("div");
    buttons.className = "inline-edit-actions";
    buttons.append(save, cancel);

    form.append(textarea, buttons);
    form.addEventListener("submit", async function (event) {
        event.preventDefault();
        save.disabled = true;
        try {
            const response = await api("/api/drawings/" + drawing.id, {
                method: "PATCH",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ description: textarea.value })
            });
            if (!response.ok) {
                throw new Error(await readProblem(response));
            }
            const updated = await response.json();
            drawing.description = updated.description;
            const hasDescription = updated.description && updated.description.length > 0;
            description.textContent = hasDescription ? updated.description : "No description yet - add one below!";
            description.classList.toggle("empty", !hasDescription);
            form.remove();
            description.classList.remove("hidden");
            setStatus(statusBox, "Description updated.", false);
        } catch (error) {
            setStatus(statusBox, error.message, true);
            save.disabled = false;
        }
    });

    description.after(form);
    textarea.focus();
}

async function replaceImage(card, drawing, input) {
    const file = input.files && input.files[0];
    if (!file) {
        return;
    }

    const formData = new FormData();
    formData.append("image", file, file.name);

    try {
        const response = await api("/api/drawings/" + drawing.id + "/image", {
            method: "PUT",
            body: formData
        });
        if (!response.ok) {
            throw new Error(await readProblem(response));
        }
        const updated = await response.json();
        drawing.imageUrl = updated.imageUrl + "?v=" + Date.now();
        const image = card.querySelector(".manage-image");
        if (image) {
            image.src = drawing.imageUrl;
        }
        setStatus(statusBox, "Image replaced.", false);
    } catch (error) {
        setStatus(statusBox, error.message, true);
    } finally {
        input.value = "";
    }
}

async function deleteDrawing(drawing) {
    if (!window.confirm("Delete this drawing? This also removes its votes and comments.")) {
        return;
    }

    try {
        const response = await api("/api/drawings/" + drawing.id, { method: "DELETE" });
        if (!response.ok) {
            throw new Error(await readProblem(response));
        }
        drawings = drawings.filter(function (item) {
            return item.id !== drawing.id;
        });
        render();
        setStatus(statusBox, "Drawing deleted.", false);
    } catch (error) {
        setStatus(statusBox, error.message, true);
    }
}

function highlightHash() {
    if (!window.location.hash.startsWith("#drawing-")) {
        return;
    }
    const target = document.getElementById(window.location.hash.slice(1));
    if (!target) {
        return;
    }
    target.classList.add("highlight");
    target.scrollIntoView({ behavior: "smooth", block: "center" });
}

function renderStats() {
    if (!statsBody) {
        return;
    }

    const totalVotes = drawings.reduce(function (sum, drawing) {
        return sum + drawing.voteCount;
    }, 0);

    const newest = drawings.length > 0
        ? formatDate(drawings.reduce(function (latest, drawing) {
            return new Date(drawing.createdAt) > new Date(latest.createdAt) ? drawing : latest;
        }, drawings[0]).createdAt)
        : "No posts yet";

    statsBody.replaceChildren();

    const list = document.createElement("ul");
    list.className = "widget-list plain stats-list";

    [
        ["Drawings posted", drawings.length],
        ["Votes received", totalVotes],
        ["Latest post", newest]
    ].forEach(function (entry) {
        const item = document.createElement("li");
        const label = document.createElement("span");
        label.textContent = entry[0] + ": ";
        const value = document.createElement("strong");
        value.textContent = String(entry[1]);
        item.append(label, value);
        list.appendChild(item);
    });

    statsBody.appendChild(list);
}

function render() {
    gallery.replaceChildren();

    if (drawings.length === 0) {
        emptyState.classList.remove("hidden");
    } else {
        emptyState.classList.add("hidden");
        drawings.forEach(function (drawing) {
            gallery.appendChild(createCard(drawing));
        });
        highlightHash();
    }

    renderStats();
}

async function loadMine() {
    try {
        const response = await api("/api/drawings/mine");
        if (!response.ok && response.status !== 404) {
            throw new Error(await readProblem(response));
        }
        drawings = response.status === 404 ? [] : await response.json();
        render();
    } catch (error) {
        setStatus(statusBox, error.message, true);
    }
}

function setupSidebar() {
    const stats = addSidebarWidget("\u2605 Your Stats \u2605", "<p class=\"tiny\">Counting...</p>", "widget-stats");
    if (stats) {
        statsBody = stats.querySelector(".widget-body");
    }

    addSidebarWidget("\u2605 House Rules \u2605", `
        <ul class="widget-list">
            <li>You can edit or delete only your own drawings.</li>
            <li>Replacing an image keeps the votes and comments.</li>
            <li>Deleting a drawing removes its guestbook too.</li>
        </ul>
    `, "widget-rules");
}

async function init() {
    currentUser = await initShell("mine");
    setupSidebar();

    const justCreated = new URLSearchParams(window.location.search).get("created") === "1";
    if (justCreated) {
        window.history.replaceState({}, "", "/my-drawings");
    }

    await loadMine();

    if (justCreated) {
        setStatus(statusBox, "Drawing published! It is now in your collection.", false);
    }
}

init().catch(function (error) {
    setStatus(statusBox, error.message, true);
});
