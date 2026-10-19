import { api, readProblem, setStatus } from "./api.js";
import { addSidebarWidget, initShell } from "./shell.js";

const CANVAS_WIDTH = 1000;
const CANVAS_HEIGHT = 700;
const HISTORY_LIMIT = 12;

const canvas = document.getElementById("drawing-canvas");
const context = canvas.getContext("2d", { willReadFrequently: true });
const brushColor = document.getElementById("brush-color");
const brushSize = document.getElementById("brush-size");
const brushSizeValue = document.getElementById("brush-size-value");
const descriptionInput = document.getElementById("drawing-description");
const publishButton = document.getElementById("publish-button");
const studioStatus = document.getElementById("studio-status");

const DRAWING_PALETTE = [
    ["#111111", "Black"],
    ["#7f7f7f", "Grey"],
    ["#ffffff", "White"],
    ["#ff0000", "Red"],
    ["#ff7f00", "Orange"],
    ["#ffe600", "Yellow"],
    ["#00a000", "Green"],
    ["#00c2ff", "Cyan"],
    ["#1d4ed8", "Blue"],
    ["#8000ff", "Purple"],
    ["#ff2d95", "Pink"],
    ["#8b4513", "Brown"]
];

let history = [];
let isDrawing = false;
let lastPoint = null;

function whiteOut() {
    context.save();
    context.fillStyle = "#ffffff";
    context.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
    context.restore();
}

function pushHistory() {
    history.push(context.getImageData(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT));
    if (history.length > HISTORY_LIMIT) {
        history.shift();
    }
}

function pointFrom(event) {
    const rect = canvas.getBoundingClientRect();
    return {
        x: (event.clientX - rect.left) * (CANVAS_WIDTH / rect.width),
        y: (event.clientY - rect.top) * (CANVAS_HEIGHT / rect.height)
    };
}

function configureBrush() {
    context.strokeStyle = brushColor.value;
    context.fillStyle = brushColor.value;
    context.lineWidth = Number(brushSize.value);
    context.lineCap = "round";
    context.lineJoin = "round";
}

function setupCanvas() {
    whiteOut();

    brushSize.addEventListener("input", function () {
        brushSizeValue.textContent = brushSize.value + " px";
    });

    canvas.addEventListener("pointerdown", function (event) {
        event.preventDefault();
        canvas.setPointerCapture(event.pointerId);
        pushHistory();
        configureBrush();
        isDrawing = true;
        lastPoint = pointFrom(event);

        context.beginPath();
        context.arc(lastPoint.x, lastPoint.y, Number(brushSize.value) / 2, 0, Math.PI * 2);
        context.fill();
    });

    canvas.addEventListener("pointermove", function (event) {
        if (!isDrawing || !lastPoint) {
            return;
        }
        event.preventDefault();
        const point = pointFrom(event);
        context.beginPath();
        context.moveTo(lastPoint.x, lastPoint.y);
        context.lineTo(point.x, point.y);
        context.stroke();
        lastPoint = point;
    });

    function endStroke() {
        isDrawing = false;
        lastPoint = null;
    }

    canvas.addEventListener("pointerup", endStroke);
    canvas.addEventListener("pointerleave", endStroke);
    canvas.addEventListener("pointercancel", endStroke);

    document.getElementById("undo-button").addEventListener("click", function () {
        const previous = history.pop();
        if (previous) {
            context.putImageData(previous, 0, 0);
        }
    });

    document.getElementById("clear-button").addEventListener("click", function () {
        pushHistory();
        whiteOut();
    });

    publishButton.addEventListener("click", publishDrawing);
}

async function publishDrawing() {
    publishButton.disabled = true;
    setStatus(studioStatus, "Publishing your drawing...", false);

    try {
        const blob = await new Promise(function (resolve) {
            canvas.toBlob(resolve, "image/png");
        });
        if (!blob) {
            throw new Error("Your drawing could not be captured. Please try again.");
        }

        const formData = new FormData();
        formData.append("image", blob, "drawing.png");
        const description = descriptionInput.value.trim();
        if (description) {
            formData.append("description", description);
        }

        const response = await api("/api/drawings", { method: "POST", body: formData });
        if (!response.ok) {
            throw new Error(await readProblem(response));
        }

        setStatus(studioStatus, "Drawing published! Opening your drawings...", false);
        window.location.href = "/my-drawings?created=1";
    } catch (error) {
        setStatus(studioStatus, error.message, true);
        publishButton.disabled = false;
    }
}

function setupSidebar() {
    const palette = document.createElement("div");
    palette.className = "palette-grid";

    DRAWING_PALETTE.forEach(function (entry) {
        const swatch = document.createElement("button");
        swatch.type = "button";
        swatch.className = "palette-swatch";
        swatch.style.backgroundColor = entry[0];
        swatch.title = entry[1];
        swatch.setAttribute("aria-label", "Use " + entry[1]);
        swatch.addEventListener("click", function () {
            brushColor.value = entry[0];
            palette.querySelectorAll(".palette-swatch").forEach(function (other) {
                other.classList.toggle("active", other === swatch);
            });
        });
        palette.appendChild(swatch);
    });

    addSidebarWidget("\u2605 Quick Palette \u2605", palette, "widget-palette");

    addSidebarWidget("\u2605 Studio Tips \u2605", `
        <ul class="widget-list">
            <li>Draw with your mouse, stylus, or finger.</li>
            <li>Use <strong>Undo</strong> to step back up to 12 times.</li>
            <li><strong>Clear</strong> wipes the canvas to white.</li>
            <li>Add a description so voters know what they are looking at.</li>
        </ul>
    `, "widget-tips");

    addSidebarWidget("\u2605 Today's Prompt \u2605", `
        <p><strong>Draw your favourite snack as a friendly monster.</strong></p>
        <p class="tiny">No pressure. Stick figures are art too.</p>
    `, "widget-prompt");
}

async function init() {
    await initShell("new");
    setupSidebar();
    setupCanvas();
}

init().catch(function (error) {
    setStatus(studioStatus, error.message, true);
});
