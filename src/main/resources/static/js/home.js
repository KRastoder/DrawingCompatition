import { api, createAvatar, formatDate, readProblem, setStatus, todayISO } from "./api.js";
import { addSidebarWidget, initShell } from "./shell.js";

const gallery = document.getElementById("gallery");
const galleryEmpty = document.getElementById("gallery-empty");
const galleryStatus = document.getElementById("gallery-status");
const pagination = document.getElementById("pagination");

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

const MONTHS = ["JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"];

let currentUser = null;
let activeDrawing = null;
let filterMode = "all";
let topTenBody = null;
let currentPage = 0;
let totalPages = 1;
const PAGE_SIZE = 20;

function ownerLink(user, extraClass) {
    const link = document.createElement("a");
    link.className = "owner-link" + (extraClass ? " " + extraClass : "");
    link.href = "/profile/" + user.id;
    link.appendChild(createAvatar(user, "avatar-tiny"));

    const name = document.createElement("span");
    name.textContent = user.displayName;
    link.appendChild(name);

    return link;
}

function drawingTitle(drawing) {
    const description = (drawing.description || "").trim();
    if (!description) {
        return "Untitled Drawing No. " + drawing.id;
    }
    const firstLine = description.split(/\r?\n/)[0].trim();
    return firstLine.length > 72 ? firstLine.slice(0, 69).trimEnd() + "..." : firstLine;
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

function voteButton(drawing) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "vote-button" + (drawing.votedByMe ? " active" : "");
    button.innerHTML = '<span class="heart">' + (drawing.votedByMe ? "\u2665" : "\u2661") + "</span>" +
        "<span>" + drawing.voteCount + (drawing.voteCount === 1 ? " vote" : " votes") + "</span>";
    button.setAttribute("aria-pressed", String(drawing.votedByMe));
    button.setAttribute("aria-label", drawing.votedByMe ? "Remove your vote" : "Vote for this drawing");
    button.addEventListener("click", function () {
        toggleVote(drawing, button);
    });
    return button;
}

function openButton(drawing, label, className) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = className || "btn btn-small";
    button.textContent = label;
    button.addEventListener("click", function () {
        openDetail(drawing.id);
    });
    return button;
}

function createPost(drawing, rank) {
    const post = document.createElement("article");
    post.className = "post";
    post.id = "drawing-" + drawing.id;

    if (rank <= 10) {
        const sticker = document.createElement("span");
        sticker.className = "rank-sticker" + (rank <= 3 ? " rank-" + rank : "");
        sticker.textContent = "#" + rank;
        sticker.setAttribute("aria-label", "Rank " + rank);
        post.appendChild(sticker);
    }

    const head = document.createElement("div");
    head.className = "post-head";

    const heading = document.createElement("div");
    heading.className = "post-heading";

    const title = document.createElement("h2");
    title.className = "post-title";
    const titleLink = document.createElement("a");
    titleLink.href = "/?drawing=" + drawing.id;
    titleLink.textContent = drawingTitle(drawing);
    titleLink.addEventListener("click", function (event) {
        event.preventDefault();
        openDetail(drawing.id);
    });
    title.appendChild(titleLink);

    const created = new Date(drawing.createdAt);
    const isNew = Date.now() - created.getTime() < 2 * 24 * 60 * 60 * 1000;

    const meta = document.createElement("p");
    meta.className = "post-meta";
    meta.append("Posted by ", ownerLink(drawing.owner), " on " + formatDate(drawing.createdAt));
    if (isNew) {
        const blinkie = document.createElement("span");
        blinkie.className = "new-blinkie";
        blinkie.textContent = "NEW!";
        meta.append(" ", blinkie);
    }

    heading.append(title, meta);
    head.append(dateBadge(drawing), heading);

    const frame = document.createElement("div");
    frame.className = "post-image-frame";
    const image = document.createElement("img");
    image.className = "post-image";
    image.src = drawing.imageUrl;
    image.alt = "Drawing by " + drawing.owner.displayName;
    image.loading = "lazy";
    image.addEventListener("click", function () {
        openDetail(drawing.id);
    });
    frame.appendChild(image);

    post.append(head, frame);

    const description = (drawing.description || "").trim();
    const firstLine = description.split(/\r?\n/)[0].trim();
    const remainder = description.slice(firstLine.length).trim();
    if (remainder) {
        const body = document.createElement("p");
        body.className = "post-description";
        body.textContent = remainder;
        post.appendChild(body);
    }

    const footer = document.createElement("div");
    footer.className = "post-footer";

    const comments = openButton(drawing, "\u2665 Comments / Guestbook", "btn btn-small btn-ghost");
    comments.setAttribute("aria-label", "Open comments for this drawing");

    const permalink = document.createElement("a");
    permalink.className = "post-link";
    permalink.href = "/?drawing=" + drawing.id;
    permalink.textContent = "\u2693 Permalink";
    permalink.addEventListener("click", function (event) {
        event.preventDefault();
        openDetail(drawing.id);
    });

    const category = document.createElement("span");
    category.className = "post-category";
    category.textContent = "Drawing No. " + drawing.id;

    footer.append(voteButton(drawing), comments, permalink, category);
    post.appendChild(footer);

    return post;
}

function renderGallery(drawings, page, totalPagesParam) {
    gallery.replaceChildren();
    drawings.forEach(function (drawing, index) {
        gallery.appendChild(createPost(drawing, index + 1 + page * PAGE_SIZE));
    });
    totalPages = totalPagesParam;
    renderPagination();
    renderTopTen(drawings);
}

function renderTopTen(drawings) {
    if (!topTenBody) {
        return;
    }

    topTenBody.replaceChildren();
    if (drawings.length === 0) {
        const empty = document.createElement("p");
        empty.textContent = "No drawings in this view.";
        topTenBody.appendChild(empty);
        return;
    }

    const list = document.createElement("ol");
    list.className = "top-ten-list";
    drawings.slice(0, 10).forEach(function (drawing, index) {
        const item = document.createElement("li");

        const rank = document.createElement("span");
        rank.className = "top-ten-rank";
        rank.textContent = String(index + 1).padStart(2, "0") + ".";

        const link = document.createElement("a");
        link.href = "/?drawing=" + drawing.id;
        link.textContent = drawingTitle(drawing);
        link.title = drawingTitle(drawing);
        link.addEventListener("click", function (event) {
            event.preventDefault();
            openDetail(drawing.id);
        });

        const votes = document.createElement("span");
        votes.className = "top-ten-votes";
        votes.textContent = drawing.voteCount + "\u2665";

        item.append(rank, link, votes);
        list.appendChild(item);
    });
    topTenBody.appendChild(list);

    const note = document.createElement("p");
    note.className = "tiny";
    note.textContent = "Ranked by votes, refreshed on every visit.";
    topTenBody.appendChild(note);
}

function showEmpty(message) {
    const paragraph = galleryEmpty.querySelector("p");
    if (paragraph) {
        paragraph.textContent = message;
    }
    galleryEmpty.classList.remove("hidden");
    pagination.classList.add("hidden");
}

function hideEmpty() {
    galleryEmpty.classList.add("hidden");
    pagination.classList.remove("hidden");
}

function renderPagination() {
    if (totalPages <= 1) {
        pagination.classList.add("hidden");
        return;
    }

    pagination.classList.remove("hidden");
    pagination.replaceChildren();

    const maxVisible = 7;
    let startPage = Math.max(0, currentPage - Math.floor(maxVisible / 2));
    let endPage = Math.min(totalPages - 1, startPage + maxVisible - 1);

    if (endPage - startPage + 1 < maxVisible) {
        startPage = Math.max(0, endPage - maxVisible + 1);
    }

    if (startPage > 0) {
        pagination.appendChild(createPageButton(0, "« First"));
        if (startPage > 1) {
            const ellipsis = document.createElement("span");
            ellipsis.className = "pagination-ellipsis";
            ellipsis.textContent = "...";
            pagination.appendChild(ellipsis);
        }
    }

    for (let i = startPage; i <= endPage; i++) {
        pagination.appendChild(createPageButton(i, String(i + 1), i === currentPage));
    }

    if (endPage < totalPages - 1) {
        if (endPage < totalPages - 2) {
            const ellipsis = document.createElement("span");
            ellipsis.className = "pagination-ellipsis";
            ellipsis.textContent = "...";
            pagination.appendChild(ellipsis);
        }
        pagination.appendChild(createPageButton(totalPages - 1, "Last »"));
    }
}

function createPageButton(page, label, isActive) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "btn btn-small" + (isActive ? " active" : "");
    button.textContent = label;
    if (isActive) {
        button.disabled = true;
        button.setAttribute("aria-current", "page");
    } else {
        button.addEventListener("click", function () {
            goToPage(page);
        });
    }
    return button;
}

function goToPage(page) {
    if (page < 0 || page >= totalPages || page === currentPage) {
        return;
    }
    currentPage = page;
    loadGallery();
    window.scrollTo(0, 0);
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

    params.set("page", String(currentPage));
    params.set("size", String(PAGE_SIZE));

    setStatus(galleryStatus, "Loading drawings...", false);

    try {
        const query = params.toString();
        const response = await api("/api/drawings" + (query ? "?" + query : ""));

        if (response.status === 404) {
            const problem = await response.json().catch(function () {
                return {};
            });
            renderGallery([], 0, 1);
            showEmpty(problem.detail || "No drawings were found for that filter.");
            setStatus(galleryStatus, "", false);
            return;
        }

        if (!response.ok) {
            throw new Error(await readProblem(response));
        }

        const data = await response.json();

        if (data.content !== undefined) {
            renderGallery(data.content, data.number, data.totalPages);
            if (data.content.length === 0) {
                showEmpty("No drawings were found.");
            } else {
                hideEmpty();
            }
        } else {
            renderGallery(data, 0, 1);
            if (data.length === 0) {
                showEmpty("No drawings were found.");
            } else {
                hideEmpty();
            }
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
            currentPage = 0;
            loadGallery();
        });
    });

    document.getElementById("apply-filter").addEventListener("click", function () {
        currentPage = 0;
        loadGallery();
    });

    document.getElementById("reset-filter").addEventListener("click", function () {
        const now = todayISO();
        filterDate.value = now;
        filterFrom.value = now;
        filterTo.value = now;
        setFilterMode("all");
        currentPage = 0;
        loadGallery();
    });
}

function setupTopTenWidget() {
    const widget = addSidebarWidget("\u2605 Top 10 Drawings \u2605", "<p class=\"tiny\">Loading the charts...</p>", "widget-top10");
    if (widget) {
        topTenBody = widget.querySelector(".widget-body");
    }
}

function buildMeta(drawing) {
    detailMeta.replaceChildren();

    const votes = document.createElement("span");
    votes.textContent = drawing.voteCount + (drawing.voteCount === 1 ? " vote" : " votes");

    const owner = ownerLink(drawing.owner, "detail-owner");

    const created = document.createElement("span");
    created.textContent = formatDate(drawing.createdAt);

    detailMeta.append(votes, owner, created, voteButton(drawing));
}

function renderDetail(drawing) {
    detailTitle.textContent = drawingTitle(drawing);
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
        if (comments.length === 0) {
            const empty = document.createElement("p");
            empty.className = "tiny";
            empty.textContent = "Nobody has signed the guestbook yet. Be the first!";
            commentList.appendChild(empty);
        }
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

    const author = ownerLink(comment.author, "comment-author");
    author.setAttribute("aria-label", "View " + comment.author.displayName + "'s profile");

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
    setupTopTenWidget();
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