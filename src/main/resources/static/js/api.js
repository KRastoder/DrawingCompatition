let csrf = null;

// Simple in-memory cache for GET requests
const requestCache = new Map();
const CACHE_TTL = 30000; // 30 seconds

// Network status
let isOnline = navigator.onLine;
window.addEventListener("online", () => { isOnline = true; });
window.addEventListener("offline", () => { isOnline = false; });

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

/**
 * Sleep utility for retry delays
 */
function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

/**
 * Generate cache key from path and options
 */
function getCacheKey(path, options) {
    const method = (options?.method || "GET").toUpperCase();
    if (method !== "GET") return null;
    return path + JSON.stringify(options);
}

/**
 * Log request/response for debugging
 */
function logRequest(method, path, status, duration) {
    if (typeof console !== "undefined" && console.debug) {
        console.debug(`[API] ${method} ${path} -> ${status} (${duration}ms)`);
    }
}

/**
 * Retry with exponential backoff
 */
async function fetchWithRetry(path, settings, retries = 3, baseDelay = 500) {
    let lastError;
    
    for (let attempt = 0; attempt <= retries; attempt++) {
        const startTime = performance.now();
        
        try {
            const response = await fetch(path, settings);
            const duration = Math.round(performance.now() - startTime);
            logRequest(settings.method || "GET", path, response.status, duration);
            
            // Don't retry on client errors (4xx), only on server errors (5xx) or network errors
            if (response.status >= 500 || response.status === 0) {
                throw new Error(`Server error: ${response.status}`);
            }
            
            return response;
        } catch (error) {
            lastError = error;
            const duration = Math.round(performance.now() - startTime);
            logRequest(settings.method || "GET", path, "ERROR", duration);
            
            if (attempt < retries) {
                const delay = baseDelay * Math.pow(2, attempt) + Math.random() * 100;
                console.warn(`[API] Request failed (attempt ${attempt + 1}/${retries + 1}), retrying in ${Math.round(delay)}ms:`, error.message);
                await sleep(delay);
            }
        }
    }
    
    throw lastError;
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

    // Check cache for GET requests
    const cacheKey = getCacheKey(path, settings);
    if (cacheKey && requestCache.has(cacheKey)) {
        const cached = requestCache.get(cacheKey);
        if (Date.now() - cached.timestamp < CACHE_TTL) {
            console.debug(`[API] Cache hit for ${path}`);
            return new Response(JSON.stringify(cached.data), {
                status: 200,
                headers: { "Content-Type": "application/json", "X-Cache": "HIT" }
            });
        } else {
            requestCache.delete(cacheKey);
        }
    }

    // Check network status
    if (!isOnline && method !== "GET") {
        throw new Error("You appear to be offline. Please check your connection and try again.");
    }

    let response;
    try {
        response = await fetchWithRetry(path, settings);
    } catch (error) {
        // Log error to server for monitoring
        logErrorToServer(path, method, error);
        throw error;
    }

    // Handle 401 - session expired
    if (response.status === 401) {
        // Force CSRF refresh on next request
        csrf = null;
        window.location.href = "/login";
        throw new Error("Your session expired. Please sign in again.");
    }

    // Cache successful GET responses
    if (cacheKey && response.ok && method === "GET") {
        try {
            const clonedResponse = response.clone();
            const data = await clonedResponse.json();
            requestCache.set(cacheKey, { data, timestamp: Date.now() });
        } catch (e) {
            // Ignore caching errors
        }
    }

    return response;
}

/**
 * Clear cache for a specific path or all
 */
export function clearCache(path) {
    if (path) {
        const keysToDelete = [];
        for (const key of requestCache.keys()) {
            if (key.startsWith(path)) {
                keysToDelete.push(key);
            }
        }
        keysToDelete.forEach(key => requestCache.delete(key));
    } else {
        requestCache.clear();
    }
}

/**
 * Log error to server for monitoring
 */
async function logErrorToServer(path, method, error) {
    try {
        await fetch("/api/errors", {
            method: "POST",
            credentials: "same-origin",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                path,
                method,
                message: error.message,
                stack: error.stack,
                timestamp: new Date().toISOString(),
                userAgent: navigator.userAgent
            })
        });
    } catch (e) {
        // Ignore logging errors
    }
}

/**
 * Report unhandled errors to server
 */
export function initErrorReporting() {
    window.addEventListener("error", (event) => {
        logErrorToServer(window.location.pathname, "UNHANDLED", event.error || new Error(event.message));
    });

    window.addEventListener("unhandledrejection", (event) => {
        logErrorToServer(window.location.pathname, "UNHANDLED_REJECTION", event.reason);
    });
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

// Network status helpers
export function isNetworkOnline() {
    return isOnline;
}

export function onNetworkChange(callback) {
    window.addEventListener("online", () => callback(true));
    window.addEventListener("offline", () => callback(false));
}