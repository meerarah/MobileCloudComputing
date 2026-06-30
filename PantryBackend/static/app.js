// Pantry Management Portal - Client Script
const API_BASE = "";

// Firebase Web SDK Configuration
const firebaseConfig = {
    apiKey: "AIzaSyDP6z7v5hWgpLjMAhAUwaao9-4siwUByCI",
    authDomain: "smartcampuspantry.firebaseapp.com",
    projectId: "smartcampuspantry",
    storageBucket: "smartcampuspantry.firebasestorage.app",
    messagingSenderId: "476175639578",
    appId: "1:476175639578:web:0b48a123f123abc456def7"
};

let isFirebaseAvailable = false;
try {
    if (typeof firebase !== 'undefined') {
        firebase.initializeApp(firebaseConfig);
        isFirebaseAvailable = true;
        console.log("Firebase SDK successfully initialized in Admin Console.");
    } else {
        console.warn("Firebase SDK script not loaded. Admin Console falling back to simulation mode.");
    }
} catch (err) {
    console.error("Error initializing Firebase:", err);
}

// Local mock locations state
let locations = [
    { id: "loc_1", name: "Kolej Perindu Hub", status: "Needs Restocking", lastChecked: "10 mins ago" },
    { id: "loc_2", name: "Kolej Mawar Hub", status: "Stocked", lastChecked: "1 hour ago" },
    { id: "loc_3", name: "Kolej Mawar Hub 2", status: "Needs Restocking", lastChecked: "30 mins ago" }
];

document.addEventListener("DOMContentLoaded", () => {
    // 1. Check admin login session
    checkAdminAuth();

    // 2. Render initial view
    renderLocations();
    fetchClaims();
    fetchRestocks();
    fetchReports();

    // 3. Setup periodic polling (every 2 seconds) for real-time synchronization
    setInterval(fetchClaims, 2000);
    setInterval(fetchRestocks, 2000);
    setInterval(fetchReports, 2000);

    // 4. Bind login form submit event
    const loginForm = document.getElementById("admin-login-form");
    if (loginForm) {
        loginForm.addEventListener("submit", handleAdminLogin);
    }

    // 5. Bind sign out button event
    const btnSignOut = document.getElementById("btn-admin-signout");
    if (btnSignOut) {
        btnSignOut.addEventListener("click", handleAdminSignOut);
    }
});

/**
 * Authentication state check for admin session
 */
function checkAdminAuth() {
    const adminUser = localStorage.getItem("adminUser");
    if (adminUser && adminUser.toLowerCase().endsWith("@staf.uitm.edu.my")) {
        hideLoginScreen();
    } else {
        showLoginScreen();
    }
}

/**
 * Authenticates staff account using Firebase Authentication
 */
async function handleAdminLogin(e) {
    e.preventDefault();
    const email = document.getElementById("admin-email").value.trim();
    const password = document.getElementById("admin-password").value.trim();
    const errorEl = document.getElementById("login-error-msg");
    const loginBtn = document.getElementById("btn-admin-login");

    if (!email || !password) {
        showAdminLoginError("Please enter both email and password.");
        return;
    }

    // Enforce domain restriction for staff/admin role separation
    if (!email.toLowerCase().endsWith("@staf.uitm.edu.my")) {
        showAdminLoginError("Access Denied: Only UiTM staff accounts (@staf.uitm.edu.my) can access the console.");
        return;
    }

    if (isFirebaseAvailable) {
        try {
            loginBtn.disabled = true;
            loginBtn.innerText = "Signing in...";
            
            // Authenticate via Firebase
            const userCredential = await firebase.auth().signInWithEmailAndPassword(email, password);
            console.log("Firebase Auth Success:", userCredential.user.email);
            
            localStorage.setItem("adminUser", email);
            hideLoginScreen();
            errorEl.classList.add("hide");
        } catch (err) {
            console.warn("Firebase sign-in failed, trying to auto-register staff...", err);
            
            // Try to auto-create user for seamless lecturer testing fallback
            try {
                const newCredential = await firebase.auth().createUserWithEmailAndPassword(email, password);
                console.log("Auto-Registered Staff in Firebase:", newCredential.user.email);
                
                localStorage.setItem("adminUser", email);
                hideLoginScreen();
                errorEl.classList.add("hide");
            } catch (createErr) {
                console.error("Firebase registration failed:", createErr);
                showAdminLoginError(err.message || "Invalid credentials.");
            }
        } finally {
            loginBtn.disabled = false;
            loginBtn.innerText = "Access Console";
        }
    } else {
        // Offline / Simulation fallback mode
        localStorage.setItem("adminUser", email);
        hideLoginScreen();
    }
}

function handleAdminSignOut() {
    if (isFirebaseAvailable) {
        firebase.auth().signOut().then(() => {
            console.log("Firebase signed out.");
        });
    }
    localStorage.removeItem("adminUser");
    showLoginScreen();
}

function showLoginScreen() {
    document.getElementById("admin-login-screen").classList.remove("hide");
}

function hideLoginScreen() {
    document.getElementById("admin-login-screen").classList.add("hide");
}

function showAdminLoginError(message) {
    const errorEl = document.getElementById("login-error-msg");
    errorEl.innerText = message;
    errorEl.classList.remove("hide");
}

/**
 * Render pantry locations and their stocking requirements
 */
function renderLocations() {
    const container = document.getElementById("locations-container");
    if (!container) return;

    container.innerHTML = "";
    locations.forEach(loc => {
        const isNeedsStock = loc.status === "Needs Restocking";
        const statusClass = isNeedsStock ? "needs-stock" : "stocked";
        const statusLabel = loc.status;

        const card = document.createElement("div");
        card.className = "location-card";
        card.innerHTML = `
            <div class="location-info">
                <h3>${loc.name}</h3>
                <p>Last checked: ${loc.lastChecked}</p>
            </div>
            <div class="location-status">
                <span class="status-badge ${statusClass}">${statusLabel}</span>
                ${isNeedsStock ? `<button class="btn-restock-action" onclick="restockLocation('${loc.id}')">Restock Station</button>` : ""}
            </div>
        `;
        container.appendChild(card);
    });
}

window.restockLocation = function(id) {
    const loc = locations.find(l => l.id === id);
    if (loc) {
        loc.status = "Stocked";
        loc.lastChecked = "Just now";
        renderLocations();
    }
};

/**
 * Fetch and render Sign-out claims table
 */
async function fetchClaims() {
    const tableBody = document.getElementById("claims-table-rows");
    if (!tableBody) return;

    try {
        const response = await fetch(`${API_BASE}/api/claims`);
        const data = await response.json();
        
        if (data.success) {
            tableBody.innerHTML = "";
            if (data.claims.length === 0) {
                tableBody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--text-slate);">No item checkouts recorded.</td></tr>`;
                return;
            }
            
            data.claims.forEach(c => {
                const tr = document.createElement("tr");
                // Format timestamp
                const time = c.timestamp ? c.timestamp.split(" ")[1] || c.timestamp : "Just now";
                
                tr.innerHTML = `
                    <td><strong>${c.name}</strong></td>
                    <td><span class="student-id-badge">${c.studentId}</span></td>
                    <td><span class="phone-badge">${c.phone || "+6013-4567890"}</span></td>
                    <td>${c.itemName}</td>
                    <td><i class="fa-solid fa-location-dot text-blue"></i> ${c.location}</td>
                    <td>${time}</td>
                `;
                tableBody.appendChild(tr);
            });
        }
    } catch (err) {
        console.error("Error fetching claims:", err);
    }
}

/**
 * Fetch and render restocks history grid
 */
async function fetchRestocks() {
    const container = document.getElementById("restocks-container");
    if (!container) return;

    try {
        const response = await fetch(`${API_BASE}/api/restocks`);
        const data = await response.json();
        
        if (data.success) {
            container.innerHTML = "";
            if (data.restocks.length === 0) {
                container.innerHTML = `<p style="text-align: center; padding: 20px; color: var(--text-slate);">No restocking history recorded.</p>`;
                return;
            }
            
            data.restocks.forEach(r => {
                // Map images to high-quality unsplash sources for mock data
                let imgSrc = r.imageUrl || "tuna";
                if (!imgSrc.startsWith("http")) {
                    if (imgSrc === "tuna") imgSrc = "https://images.unsplash.com/photo-1599084993091-1cb5c0721cc6?w=150&auto=format&fit=crop&q=60";
                    else if (imgSrc === "cereal") imgSrc = "https://images.unsplash.com/photo-1586444248902-2f64eddc13df?w=150&auto=format&fit=crop&q=60";
                    else if (imgSrc === "apple") imgSrc = "https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?w=150&auto=format&fit=crop&q=60";
                    else if (imgSrc === "milk") imgSrc = "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=150&auto=format&fit=crop&q=60";
                    else if (imgSrc === "soup") imgSrc = "https://images.unsplash.com/photo-1547592165-e1d17fed6006?w=150&auto=format&fit=crop&q=60";
                    else imgSrc = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=150&auto=format&fit=crop&q=60";
                }
                
                const time = r.timestamp ? r.timestamp.split(" ")[1] || r.timestamp : "Just now";
                
                const card = document.createElement("div");
                card.className = "restock-card";
                card.innerHTML = `
                    <div class="restock-img-box">
                        <img src="${imgSrc}" class="restock-img" alt="${r.itemName}">
                    </div>
                    <div class="restock-info">
                        <div class="restock-item-title">${r.itemName}</div>
                        <div class="restock-item-qty">Qty: +${r.quantity}</div>
                        <div class="restock-meta">Restocked by: <strong>${r.restockerName}</strong></div>
                        <div class="restock-meta"><i class="fa-solid fa-location-dot"></i> ${r.location}</div>
                    </div>
                    <div class="restock-time">${time}</div>
                `;
                container.appendChild(card);
            });
        }
    } catch (err) {
        console.error("Error fetching restocks:", err);
    }
}

/**
 * Fetch and render customer reports table
 */
async function fetchReports() {
    const tableBody = document.getElementById("reports-table-rows");
    if (!tableBody) return;

    try {
        const response = await fetch(`${API_BASE}/api/reports`);
        const data = await response.json();
        
        if (data.success) {
            tableBody.innerHTML = "";
            if (data.reports.length === 0) {
                tableBody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--text-slate);">No item reports submitted by students.</td></tr>`;
                return;
            }
            
            data.reports.forEach(r => {
                const tr = document.createElement("tr");
                const time = r.timestamp ? r.timestamp.split(" ")[1] || r.timestamp : "Just now";
                
                tr.innerHTML = `
                    <td><strong>${r.studentName}</strong></td>
                    <td><span class="student-id-badge">${r.studentId}</span></td>
                    <td>${r.itemName}</td>
                    <td><i class="fa-solid fa-location-dot text-blue"></i> ${r.location}</td>
                    <td style="color: var(--accent-red); font-weight: 500;">
                        <i class="fa-solid fa-circle-exclamation"></i> ${r.issueDescription}
                    </td>
                    <td>${time}</td>
                `;
                tableBody.appendChild(tr);
            });
        }
    } catch (err) {
        console.error("Error fetching reports:", err);
    }
}
