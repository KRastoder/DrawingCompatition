import { api, formatDate, readProblem, setStatus, todayISO } from "./api.js";
import { initShell } from "./shell.js";

const gallery = document.getElementById("gallery");
const galleryEmpty = document.getElementById("gallery-empty");
const galleryStatus = document.getElementById("gallery-status");

const dialog = document.getElementById("detail-dialog");
const detailTitle = document.getElementById("detail-title");
const detailMeta = document.getElementById("detail-meta");
const detailImage = document.getElementById("detail-image");
const detailDescription = document.getElementById("detail-description");
const detailOwnerActions = document.getElementById("detail-owner-actions");
const detailManageLink = document.getElementById("detail-manage-link");
const commentList = document.getElementById("comment-list");
const commentStatus = document.getElementById("comment-status");

const filterDate = document.getElementById("filter-date");
const filterFrom = document.getElementById("filter-from");
const filterTo = document.getElementById("filter-to");
const singleDateField = document.getElementById("single-date-field");
const rangeDateField = document.getElementById("range-date-field");

let currentUser = null;
let activeDrawing = null;
let filterMode = "all";

function voteButton(drawing) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "vote-button" + (drawing.votedByMe ? " active" : "");
    button.innerHTML = '<span class="heart">' + (drawing.votedByMe ? "\u2665" : "\u2661") + "</span>" +
        "<span>" + drawing.voteCount + "</span>";
    button.setAttribute("aria-pressed", String(drawing.votedByMe));
    button.setAttribute("aria-label", drawing.votedByMe ? "Remove your vote" : "Vote for this drawing");
    button.addEventListener("click", function () {
        toggleVote(drawing, button);
    });
    return button;
}

function createCard(drawing, rank) {
    const card = document.createElement("article");
    card.className = "drawing-card";

    const badge = document.createElement("span");
    badge.className = "rank-badge" + (rank <= 3 ? " rank-" + rank : "");
    badge.textContent = String(rank);
    badge.setAttribute("aria-label", "Rank " + rank);

    const image = document.createElement("img");
    image.className = "card-image";
    image.src = drawing.imageUrl;
    image.alt = "Drawing by " + drawing.owner.displayName;
    image.loading = "lazy";
    image.addEventListener("click", function () {
        openDetail(drawing.id);
    });

    const body = document.createElement("div");
    body.className = "card-body";

    const description = document.createElement("p");
    const hasDescription = drawing.description && drawing.description.length > 0;
    description.className = "card-description" + (hasDescription ? "" : " empty");
    description.textContent = hasDescription ? drawing.description : "No description";

    const meta = document.createElement("div");
    meta.className = "card-meta";
    meta.textContent = drawing.owner.displayName + " · " + formatDate(drawing.createdAt);

    const actions = document.createElement("div");
    actions.className = "card-actions";
    actions.appendChild(voteButton(drawing));

    const open = document.createElement("button");
    open.type = "button";
    open.className = "btn btn-small btn-ghost";
    open.textContent = "Open";
    open.addEventListener("click", function () {
        openDetail(drawing.id);
    });
    actions.appendChild(open);

    body.append(description, meta, actions);
    card.append(image, body, badge);
    return card;
}

function renderGallery(drawings) {
    gallery.replaceChildren();
    drawings.forEach(function (drawing, index) {
        gallery.appendChild(createCard(drawing, index + 1));
    });
}

function showEmpty(message) {
    const paragraph = galleryEmpty.querySelector("p");
    if (paragraph) {
        paragraph.textContent = message;
    }
    galleryEmpty.classList.remove("hidden");
}

function hideEmpty() {
    galleryEmpty.classList.add("hidden");
}

async function loadGallery() {
    const params = new URLSearchParams();

    if (filterMode === "day") {
        if (!filterDate.value) {
            setStatus(galleryStatus, "Pick a date to show one day of drawings.", true);
            return;
        }
        params.set("date", filterDate.value);
    } else if (filterMode === "range") {
        if (!filterFrom.value || !filterTo.value) {
            setStatus(galleryStatus, "Pick both a start and an end date.", true);
            return;
        }
        params.set("from", filterFrom.value);
        params.set("to", filterTo.value);
    }

    setStatus(galleryStatus, "Loading drawings...", false);

    try {
        const query = params.toString();
        const response = await api("/api/drawings" + (query ? "?" + query : ""));

        if (response.status === 404) {
            const problem = await response.json().catch(function () {
                return {};
            });
            renderGallery([]);
            showEmpty(problem.detail || "No drawings were found for that filter.");
            setStatus(galleryStatus, "", false);
            return;
        }

        if (!response.ok) {
            throw new Error(await readProblem(response));
        }

        const drawings = await response.json();
        renderGallery(drawings);
        if (drawings.length === 0) {
            showEmpty("No drawings were found.");
        } else {
            hideEmpty();
        }
        setStatus(galleryStatus, "", false);
    } catch (error) {
        setStatus(galleryStatus, error.message, true);
    }
}

async function toggleVote(drawing, button) {
    button.disabled = true;

    try {
        const method = drawing.votedByMe ? "DELETE" : "POST";
        const response = await api("/api/drawings/" + drawing.id + "/votes", { method: method });
        if (!response.ok) {
            throw new Error(await readProblem(response));
        }

        await loadGallery();
        if (activeDrawing && activeDrawing.id === drawing.id) {
            await openDetail(drawing.id);
        }
    } catch (error) {
        setStatus(galleryStatus, error.message, true);
    } finally {
        button.disabled = false;
    }
}

function setFilterMode(mode) {
    filterMode = mode;
    document.querySelectorAll(".filter-modes button").forEach(function (button) {
        button.classList.toggle("active", button.dataset.mode === mode);
    });
    singleDateField.classList.toggle("hidden", mode !== "day");
    rangeDateField.classList.toggle("hidden", mode !== "range");
}

function setupFilters() {
    const today = todayISO();
    filterDate.value = today;
    filterFrom.value = today;
    filterTo.value = today;

    document.querySelectorAll(".filter-modes button").forEach(function (button) {
        button.addEventListener("click", function () {
            setFilterMode(button.dataset.mode);
            loadGallery();
        });
    });

    document.getElementById("apply-filter").addEventListener("click", loadGallery);

    document.getElementById("reset-filter").addEventListener("click", function () {
        const now = todayISO();
        filterDate.value = now;
        filterFrom.value = now;
        filterTo.value = now;
        setFilterMode("all");
        loadGallery();
    });
}

function buildMeta(drawing) {
    detailMeta.replaceChildren();

    const votes = document.createElement("span");
    votes.textContent = drawing.voteCount + (drawing.voteCount === 1 ? " vote" : " votes");

    const owner = document.createElement("span");
    owner.textContent = "by " + drawing.owner.displayName;

    const created = document.createElement("span");
    created.textContent = formatDate(drawing.createdAt);

    detailMeta.append(votes, owner, created, voteButton(drawing));
}

function renderDetail(drawing) {
    detailTitle.textContent = "Drawing by " + drawing.owner.displayName;
    buildMeta(drawing);
    detailImage.src = drawing.imageUrl;
    detailImage.alt = "Drawing by " + drawing.owner.displayName;

    const hasDescription = drawing.description && drawing.description.length > 0;
    detailDescription.className = "card-description" + (hasDescription ? "" : " empty");
    detailDescription.textContent = hasDescription ? drawing.description : "No description";

    const isOwner = currentUser && drawing.owner.id === currentUser.id;
    detailOwnerActions.classList.toggle("hidden", !isOwner);
    if (isOwner) {
        detailManageLink.href = "/my-drawings#drawing-" + drawing.id;
    }
}

async function openDetail(id) {
    try {
        const response = await api("/api/drawings/" + id);
        if (!response.ok) {
            throw new Error(await readProblem(response));
        }

        activeDrawing = await response.json();
        renderDetail(activeDrawing);
        await loadComments(id);

        if (!dialog.open) {
            dialog.showModal();
        }
    } catch (error) {
        setStatus(galleryStatus, error.message, true);
    }
}

async function loadComments(drawingId) {
    commentList.replaceChildren();
    setStatus(commentStatus, "", false);

    try {
        const response = await api("/api/drawings/" + drawingId + "/comments");
        if (!response.ok) {
            throw new Error(await readProblem(response));
        }
        const comments = await response.json();
        comments.forEach(function (comment) {
            commentList.appendChild(createComment(comment));
        });
    } catch (error) {
        setStatus(commentStatus, error.message, true);
    }
}

function createComment(comment) {
    const wrapper = document.createElement("div");
    wrapper.className = "comment";

    const head = document.createElement("div");
    head.className = "comment-head";

    const author = document.createElement("span");
    author.textContent = comment.author.displayName;

    const date = document.createElement("span");
    date.textContent = formatDate(comment.createdAt);

    head.append(author, date);

    const body = document.createElement("p");
    body.className = "comment-body";
    body.textContent = comment.body;

    wrapper.append(head, body);

    if (currentUser && comment.author.id === currentUser.id) {
        const actions = document.createElement("div");
        actions.className = "comment-actions";

        const edit = document.createElement("button");
        edit.type = "button";
        edit.className = "btn btn-small";
        edit.textContent = "Edit";
        edit.addEventListener("click", function () {
            showCommentEditor(wrapper, comment);
        });

        const remove = document.createElement("button");
        remove.type = "button";
        remove.className = "btn btn-small btn-danger";
        remove.textContent = "Delete";
        remove.addEventListener("click", function () {
            deleteComment(comment.id);
        });

        actions.append(edit, remove);
        wrapper.appendChild(actions);
    }

    return wrapper;
}

function showCommentEditor(wrapper, comment) {
    wrapper.replaceChildren();

    const form = document.createElement("form");
    const field = document.createElement("div");
    field.className = "field";
    const label = document.createElement("label");
    label.textContent = "Edit comment";
    const textarea = document.createElement("textarea");
    textarea.maxLength = 1000;
    textarea.value = comment.body;
    field.append(label, textarea);

    const save = document.createElement("button");
    save.type = "submit";
    save.className = "btn btn-primary btn-small";
    save.textContent = "Save";

    const cancel = document.createElement("button");
    cancel.type = "button";
    cancel.className = "btn btn-small";
    cancel.textContent = "Cancel";
    cancel.addEventListener("click", function () {
        loadComments(activeDrawing.id);
    });

    form.append(field, save, cancel);
    form.addEventListener("submit", async function (event) {
        event.preventDefault();
        try {
            const response = await api("/api/comments/" + comment.id, {
                method: "PATCH",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ body: textarea.value })
            });
            if (!response.ok) {
                throw new Error(await readProblem(response));
            }
            await loadComments(activeDrawing.id);
        } catch (error) {
            setStatus(commentStatus, error.message, true);
        }
    });

    wrapper.appendChild(form);
}

async function deleteComment(commentId) {
    if (!window.confirm("Delete this comment?")) {
        return;
    }

    try {
        const response = await api("/api/comments/" + commentId, { method: "DELETE" });
        if (!response.ok) {
            throw new Error(await readProblem(response));
        }
        await loadComments(activeDrawing.id);
    } catch (error) {
        setStatus(commentStatus, error.message, true);
    }
}

function setupDialog() {
    document.getElementById("detail-close").addEventListener("click", function () {
        dialog.close();
    });

    dialog.addEventListener("click", function (event) {
        if (event.target === dialog) {
            dialog.close();
        }
    });

    document.getElementById("comment-form").addEventListener("submit", async function (event) {
        event.preventDefault();
        const body = document.getElementById("comment-body").value.trim();
        if (!body) {
            setStatus(commentStatus, "Write something before posting.", true);
            return;
        }

        try {
            const response = await api("/api/drawings/" + activeDrawing.id + "/comments", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ body: body })
            });
            if (!response.ok) {
                throw new Error(await readProblem(response));
            }
            document.getElementById("comment-body").value = "";
            await loadComments(activeDrawing.id);
        } catch (error) {
            setStatus(commentStatus, error.message, true);
        }
    });
}

async function init() {
    currentUser = await initShell("home");
    setupFilters();
    setupDialog();
    await loadGallery();

    const requestedDrawing = new URLSearchParams(window.location.search).get("drawing");
    if (requestedDrawing) {
        await openDetail(Number(requestedDrawing));
    }
}

init().catch(function (error) {
    setStatus(galleryStatus, error.message, true);
});
