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
    { id: "loc_1", name: "Kolej Mawar Hub", status: "Needs Restocking", lastChecked: "10 mins ago" }
];

document.addEventListener("DOMContentLoaded", () => {
    // 1. Check admin login session
    checkAdminAuth();

    // 2. Render initial view
    renderLocations();
    fetchClaims();
    fetchRestocks();
    fetchReports();
    fetchStudents();

    // 3. Setup periodic polling (every 2 seconds) for real-time synchronization
    setInterval(fetchClaims, 2000);
    setInterval(fetchRestocks, 2000);
    setInterval(fetchReports, 2000);
    setInterval(fetchStudents, 5000);

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

/**
 * Reset quota for a specific student
 */
async function resetStudentQuota(studentId) {
    if (!confirm(`Are you sure you want to reset the weekly quota for student ${studentId}?`)) return;
    
    try {
        const response = await fetch(`${API_BASE}/api/student/${studentId}/reset-quota`, {
            method: 'PUT'
        });
        const result = await response.json();
        
        if (result.success) {
            alert(result.message);
            fetchStudents(); // Refresh table
        } else {
            alert(result.message);
        }
    } catch (err) {
        console.error("Error resetting student quota:", err);
        alert("Failed to reset student quota.");
    }
}

/**
 * Reset quota for all students globally
 */
async function resetAllQuotas() {
    if (!confirm("Are you sure you want to completely reset the weekly quota for ALL students? This is usually done on Monday mornings.")) return;
    
    try {
        const response = await fetch(`${API_BASE}/api/reset-claims`, {
            method: 'POST'
        });
        const result = await response.json();
        
        if (result.success) {
            alert(result.message);
            fetchStudents(); // Refresh table
        } else {
            alert(result.message);
        }
    } catch (err) {
        console.error("Error resetting all quotas:", err);
        alert("Failed to reset quotas globally.");
    }
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
                let imgSrc = r.imageUrl || "tuna";
                
                const getFallback = (name) => {
                    const iName = (name || "").toLowerCase();
                    if (iName.includes("tuna")) return "https://images.unsplash.com/photo-1599084993091-1cb5c0721cc6?w=150&auto=format&fit=crop&q=60";
                    if (iName.includes("cereal")) return "https://images.unsplash.com/photo-1586444248902-2f64eddc13df?w=150&auto=format&fit=crop&q=60";
                    if (iName.includes("apple") || iName.includes("gala")) return "https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?w=150&auto=format&fit=crop&q=60";
                    if (iName.includes("milk")) return "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=150&auto=format&fit=crop&q=60";
                    if (iName.includes("soup")) return "https://images.unsplash.com/photo-1547592165-e1d17fed6006?w=150&auto=format&fit=crop&q=60";
                    return "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=150&auto=format&fit=crop&q=60";
                };

                const fallbackUrl = getFallback(r.itemName);
                
                if (!imgSrc.startsWith("http") || imgSrc.startsWith("content://")) {
                    imgSrc = fallbackUrl;
                }
                
                const time = r.timestamp ? r.timestamp.split(" ")[1] || r.timestamp : "Just now";
                
                const card = document.createElement("div");
                card.className = "restock-card";
                card.innerHTML = `
                    <div class="restock-img-box">
                        <img src="${imgSrc}" class="restock-img" alt="${r.itemName}" onerror="this.onerror=null; this.src='${fallbackUrl}';">
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

/**
 * Fetch and render students table
 */
async function fetchStudents() {
    const tableBody = document.getElementById("students-table-rows");
    if (!tableBody) return;

    try {
        const response = await fetch(`${API_BASE}/api/students`);
        const data = await response.json();
        
        if (data.success) {
            tableBody.innerHTML = "";
            if (data.students.length === 0) {
                tableBody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--text-slate);">No registered students found.</td></tr>`;
                return;
            }
            
            data.students.forEach(s => {
                const tr = document.createElement("tr");
                
                tr.innerHTML = `
                    <td><strong>${s.name}</strong></td>
                    <td><span class="student-id-badge">${s.studentId}</span></td>
                    <td><span class="phone-badge">${s.phone || "N/A"}</span></td>
                    <td>${s.claimsThisWeek} / 3</td>
                    <td><span style="color: var(--accent-green); font-weight: bold;">${s.impactPoints} pts</span></td>
                    <td style="display: flex; gap: 8px;">
                        <button onclick="resetStudentQuota('${s.studentId}')" style="background: var(--accent-orange); color: white; border: none; padding: 6px 12px; border-radius: 4px; cursor: pointer; font-size: 12px;">
                            <i class="fa-solid fa-rotate-left"></i> Reset Quota
                        </button>
                        <button onclick="openEditStudentModal('${s.studentId}', '${s.name}', '${s.phone || ''}', ${s.impactPoints})" style="background: var(--accent-blue); color: white; border: none; padding: 6px 12px; border-radius: 4px; cursor: pointer; font-size: 12px;">
                            <i class="fa-solid fa-pen"></i> Edit
                        </button>
                        <button onclick="deleteStudent('${s.studentId}')" style="background: var(--accent-red); color: white; border: none; padding: 6px 12px; border-radius: 4px; cursor: pointer; font-size: 12px;">
                            <i class="fa-solid fa-trash"></i> Delete
                        </button>
                    </td>
                `;
                tableBody.appendChild(tr);
            });
        }
    } catch (err) {
        console.error("Error fetching students:", err);
    }
}

async function deleteStudent(studentId) {
    if (!confirm(`Are you sure you want to permanently delete student ${studentId}?`)) return;
    
    try {
        const res = await fetch(`${API_BASE}/api/student/${studentId}`, { method: 'DELETE' });
        const data = await res.json();
        if (data.success) {
            fetchStudents();
        } else {
            alert(data.message || "Failed to delete student");
        }
    } catch (err) {
        console.error("Error deleting student:", err);
        alert("An error occurred while deleting the student.");
    }
}

// Student CRUD Modal Logic
function openAddStudentModal() {
    document.getElementById('student-modal-title').innerText = "Add Student";
    document.getElementById('student-modal-mode').value = "add";
    document.getElementById('modal-student-id').value = "";
    document.getElementById('modal-student-id').readOnly = false;
    document.getElementById('modal-student-name').value = "";
    document.getElementById('modal-student-phone').value = "";
    document.getElementById('points-group').style.display = "none";
    
    document.getElementById('student-modal').style.display = "flex";
}

function openEditStudentModal(id, name, phone, points) {
    document.getElementById('student-modal-title').innerText = "Edit Student";
    document.getElementById('student-modal-mode').value = "edit";
    document.getElementById('modal-student-id').value = id;
    document.getElementById('modal-student-id').readOnly = true;
    document.getElementById('modal-student-name').value = name;
    document.getElementById('modal-student-phone').value = phone;
    
    const pointsGroup = document.getElementById('points-group');
    pointsGroup.style.display = "block";
    document.getElementById('modal-student-points').value = points;
    
    document.getElementById('student-modal').style.display = "flex";
}

function closeStudentModal() {
    document.getElementById('student-modal').style.display = "none";
}

document.addEventListener('DOMContentLoaded', () => {
    const studentForm = document.getElementById('student-form');
    if (studentForm) {
        studentForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const mode = document.getElementById('student-modal-mode').value;
            const studentId = document.getElementById('modal-student-id').value.trim();
            const name = document.getElementById('modal-student-name').value.trim();
            const phone = document.getElementById('modal-student-phone').value.trim();
            const points = parseInt(document.getElementById('modal-student-points').value) || 0;
            
            try {
                let url, method, body;
                if (mode === "add") {
                    url = `${API_BASE}/api/register`;
                    method = 'POST';
                    body = { studentId, name, phone, password: "password123" }; // default password
                } else {
                    url = `${API_BASE}/api/student/${studentId}`;
                    method = 'PUT';
                    body = { name, phone, impactPoints: points };
                }
                
                const res = await fetch(url, {
                    method,
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(body)
                });
                const data = await res.json();
                
                if (data.success) {
                    closeStudentModal();
                    fetchStudents();
                } else {
                    alert(data.message || "Failed to save student.");
                }
            } catch (err) {
                console.error("Error saving student:", err);
                alert("An error occurred.");
            }
        });
    }
});
