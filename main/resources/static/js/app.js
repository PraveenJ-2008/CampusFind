/* CampusFind - Frontend Application Logic */

const API_BASE = "";
let authToken = localStorage.getItem("campusfind_token") || null;
let currentUser = null;
let currentView = "view-browse-lost";
let allCategories = [];

// API Fetch Wrapper
async function apiFetch(endpoint, options = {}) {
  const headers = options.headers || {};
  if (!headers["Content-Type"] && !(options.body instanceof FormData)) {
    headers["Content-Type"] = "application/json";
  }
  if (authToken) {
    headers["Authorization"] = `Bearer ${authToken}`;
  }

  try {
    const res = await fetch(`${API_BASE}${endpoint}`, { ...options, headers });
    const data = await res.json().catch(() => null);

    if (res.status === 401) {
      if (authToken) {
        showToast("Session expired. Please log in again.", "error");
        logout();
      }
      throw new Error(data && data.message ? data.message : "Unauthorized");
    }

    if (!res.ok) {
      const errorMsg = (data && data.message) || (data && data.errors && data.errors.join(", ")) || "Request failed";
      throw new Error(errorMsg);
    }

    return data;
  } catch (err) {
    throw err;
  }
}
// Date restriction: prevent future dates

// Initialization on DOM Load
document.addEventListener("DOMContentLoaded", async () => {
  await restoreSession();
  await loadGlobalCategories();
  navigateTo("view-browse-lost");
});

async function restoreSession() {
  if (!authToken) {
    updateNavigationUI();
    return;
  }

  try {
    const res = await apiFetch("/api/auth/me");
    if (res && res.success && res.data) {
      currentUser = res.data;
    } else {
      logout();
    }
  } catch (e) {
    logout();
  }
  updateNavigationUI();
}

function updateNavigationUI() {
  const snippet = document.getElementById("sidebar-user-snippet");
  const userNameEl = document.getElementById("sidebar-user-name");
  const userRoleEl = document.getElementById("sidebar-user-role");
  const authToggleBtn = document.getElementById("btn-auth-toggle");
  const headerGreeting = document.getElementById("header-user-greeting");

  const secStudentCat = document.getElementById("sec-student-cat");
  const secStudentDash = document.getElementById("sec-student-dash");
  const secReportLost = document.getElementById("sec-report-lost");

  const secStaffCat = document.getElementById("sec-staff-cat");
  const secReportFound = document.getElementById("sec-report-found");

  const secAdminCat = document.getElementById("sec-admin-cat");
  const secAdminDash = document.getElementById("sec-admin-dash");
  const secAdminCatMgmt = document.getElementById("sec-admin-cat-mgmt");

  const secProfile = document.getElementById("sec-profile");
  const secLogin = document.getElementById("sec-login");
  const secRegister = document.getElementById("sec-register");

  if (currentUser) {
    userNameEl.textContent = currentUser.fullName;
    userRoleEl.textContent = currentUser.role;
    userRoleEl.className = `user-role-badge badge-${currentUser.role.toLowerCase()}`;
    authToggleBtn.textContent = "Sign Out";
    headerGreeting.textContent = `Hello, ${currentUser.fullName} (${currentUser.role})`;

    secProfile.style.display = "block";
    secLogin.style.display = "none";
    secRegister.style.display = "none";

    // Role-based visibility
    if (currentUser.role === "STUDENT") {
      secStudentCat.style.display = "block";
      secStudentDash.style.display = "block";
      secReportLost.style.display = "block";

      secStaffCat.style.display = "none";
      secReportFound.style.display = "none";
      secAdminCat.style.display = "none";
      secAdminDash.style.display = "none";
      secAdminCatMgmt.style.display = "none";
    } else if (currentUser.role === "STAFF") {
      secStudentCat.style.display = "block";
      secStudentDash.style.display = "block";
      secReportLost.style.display = "block";

      secStaffCat.style.display = "block";
      secReportFound.style.display = "block";

      secAdminCat.style.display = "none";
      secAdminDash.style.display = "none";
      secAdminCatMgmt.style.display = "none";
    } else if (currentUser.role === "ADMIN") {
      secStudentCat.style.display = "block";
      secStudentDash.style.display = "block";
      secReportLost.style.display = "block";

      secStaffCat.style.display = "block";
      secReportFound.style.display = "block";

      secAdminCat.style.display = "block";
      secAdminDash.style.display = "block";
      secAdminCatMgmt.style.display = "block";
    }
  } else {
    userNameEl.textContent = "Guest User";
    userRoleEl.textContent = "NOT LOGGED IN";
    userRoleEl.className = "user-role-badge";
    authToggleBtn.textContent = "Sign In";
    headerGreeting.textContent = "Welcome to CampusFind";

    secProfile.style.display = "none";
    secLogin.style.display = "block";
    secRegister.style.display = "block";

    secStudentCat.style.display = "none";
    secStudentDash.style.display = "none";
    secReportLost.style.display = "none";

    secStaffCat.style.display = "none";
    secReportFound.style.display = "none";

    secAdminCat.style.display = "none";
    secAdminDash.style.display = "none";
    secAdminCatMgmt.style.display = "none";
  }
}

function handleAuthToggle() {
  if (currentUser) {
    logout();
  } else {
    navigateTo("view-login");
  }
}

function logout() {
  authToken = null;
  currentUser = null;
  localStorage.removeItem("campusfind_token");
  updateNavigationUI();
  showToast("You have been signed out.", "info");
  navigateTo("view-browse-lost");
}

// Navigation View Switcher
function navigateTo(viewId) {
  // Guard screens requiring authentication
  if (!currentUser && (viewId === "view-student-dashboard" || viewId === "view-report-lost" || viewId === "view-report-found" || viewId === "view-admin-dashboard" || viewId === "view-categories" || viewId === "view-profile")) {
    showToast("Please sign in to access this page.", "info");
    viewId = "view-login";
  }

  // Guard admin screens
  if ((viewId === "view-admin-dashboard" || viewId === "view-categories") && (!currentUser || currentUser.role !== "ADMIN")) {
    showToast("Access restricted to administrators.", "error");
    viewId = "view-browse-lost";
  }

  // Guard staff screens
  if (viewId === "view-report-found" && (!currentUser || (currentUser.role !== "STAFF" && currentUser.role !== "ADMIN"))) {
    showToast("Only staff and admins can register found items.", "error");
    viewId = "view-browse-found";
  }

  document.querySelectorAll(".view-section").forEach(sec => sec.classList.remove("active"));
  const target = document.getElementById(viewId);
  if (target) {
    target.classList.add("active");
    currentView = viewId;
  }

  // Update sidebar links
  document.querySelectorAll(".sidebar-link").forEach(link => {
    link.classList.remove("active");
    if (link.getAttribute("onclick") && link.getAttribute("onclick").includes(viewId)) {
      link.classList.add("active");
    }
  });

  // Update header title
  const titleMap = {
    "view-login": "Sign In",
    "view-register": "Create Account",
    "view-student-dashboard": "My Student Dashboard",
    "view-report-lost": "Report Lost Item",
    "view-browse-lost": "Browse Lost Items",
    "view-report-found": "Register Found Item",
    "view-browse-found": "Browse Found Items",
    "view-matches": "Smart Match Engine",
    "view-admin-dashboard": "Campus Administration Dashboard",
    "view-categories": "Category Management",
    "view-profile": "User Profile"
  };
  document.getElementById("page-title").textContent = titleMap[viewId] || "CampusFind";

  // Trigger screen load handlers
  if (viewId === "view-browse-lost") loadLostItems();
  if (viewId === "view-browse-found") loadFoundItems();
  if (viewId === "view-student-dashboard") loadStudentDashboard();
  if (viewId === "view-matches") initMatchesScreen();
  if (viewId === "view-admin-dashboard") loadAdminDashboard();
  if (viewId === "view-categories") loadAdminCategories();
  if (viewId === "view-profile") loadUserProfile();
  if (viewId === "view-report-lost") {
    document.getElementById("lost-date").value = new Date().toISOString().split("T")[0];
    populateCategoryDropdown("lost-category");
  }
  if (viewId === "view-report-found") {
    document.getElementById("found-date").value = new Date().toISOString().split("T")[0];
    populateCategoryDropdown("found-category");
  }
}

// Category Loader
async function loadGlobalCategories() {
  try {
    const res = await apiFetch("/api/categories");
    if (res && res.success && res.data) {
      allCategories = res.data;
      populateCategoryDropdown("browse-lost-cat", true);
      populateCategoryDropdown("browse-found-cat", true);
    }
  } catch (e) {
    console.error("Failed to load categories:", e);
  }
}

function populateCategoryDropdown(elementId, hasAllOption = false) {
  const el = document.getElementById(elementId);
  if (!el) return;

  el.innerHTML = hasAllOption ? '<option value="">All Categories</option>' : '<option value="">Select Category</option>';
  allCategories.forEach(cat => {
    const opt = document.createElement("option");
    opt.value = cat.id;
    opt.textContent = cat.name;
    el.appendChild(opt);
  });
}

// SCREEN 1: LOGIN
function fillLogin(email, password) {
  document.getElementById("login-email").value = email;
  document.getElementById("login-password").value = password;
}

async function handleLoginSubmit(e) {
  e.preventDefault();
  const email = document.getElementById("login-email").value;
  const password = document.getElementById("login-password").value;

  try {
    const res = await apiFetch("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password })
    });

    if (res && res.success && res.data) {
      authToken = res.data.token;
      currentUser = res.data.user;
      localStorage.setItem("campusfind_token", authToken);
      showToast("Signed in successfully!", "success");
      updateNavigationUI();

      if (currentUser.role === "ADMIN") {
        navigateTo("view-admin-dashboard");
      } else if (currentUser.role === "STAFF") {
        navigateTo("view-browse-found");
      } else {
        navigateTo("view-student-dashboard");
      }
    }
  } catch (err) {
    showToast(err.message, "error");
  }
}

// SCREEN 2: REGISTRATION
async function handleRegisterSubmit(e) {
  e.preventDefault();
  const fullName = document.getElementById("reg-fullname").value;
  const email = document.getElementById("reg-email").value;
  const password = document.getElementById("reg-password").value;
  const role = document.getElementById("reg-role").value;

  try {
    const res = await apiFetch("/api/auth/register", {
      method: "POST",
      body: JSON.stringify({ fullName, email, password, role })
    });

    if (res && res.success) {
      showToast("Registration successful! Please sign in.", "success");
      fillLogin(email, password);
      navigateTo("view-login");
    }
  } catch (err) {
    showToast(err.message, "error");
  }
}

// SCREEN 3: STUDENT DASHBOARD
async function loadStudentDashboard() {
  const tableBody = document.getElementById("student-reports-table-body");
  tableBody.innerHTML = `<tr><td colspan="6" style="text-align:center;">Loading your reports...</td></tr>`;

  try {
    const res = await apiFetch("/api/lost/mine");
    const reports = (res && res.data) || [];

    document.getElementById("student-my-reports-count").textContent = reports.length;
    document.getElementById("student-open-reports-count").textContent = reports.filter(r => r.status === "OPEN").length;
    document.getElementById("student-matched-reports-count").textContent = reports.filter(r => r.status === "MATCHED").length;

    if (reports.length === 0) {
      tableBody.innerHTML = `
        <tr>
          <td colspan="6" style="text-align:center; padding:32px;">
            <p style="color:var(--text-muted); margin-bottom:12px;">You haven't reported any lost items yet.</p>
            <button class="btn btn-primary btn-sm" onclick="navigateTo('view-report-lost')">Report Lost Item</button>
          </td>
        </tr>
      `;
      return;
    }

    tableBody.innerHTML = reports.map(r => `
      <tr>
        <td><strong>${escapeHtml(r.title)}</strong></td>
        <td>${escapeHtml(r.category ? r.category.name : "N/A")}</td>
        <td>${escapeHtml(r.location)}</td>
        <td>${formatDate(r.lostDate)}</td>
        <td><span class="badge badge-${r.status.toLowerCase()}">${r.status}</span></td>
        <td>
          <button class="btn btn-sm btn-outline" onclick="openMatchesForReport(${r.id})">⚡ Matches</button>
          ${r.status === "OPEN" ? `<button class="btn btn-sm btn-secondary" onclick="closeLostReportPrompt(${r.id})">Close</button>` : ""}
          <button class="btn btn-sm btn-danger" onclick="deleteLostReportPrompt(${r.id})">Delete</button>
        </td>
      </tr>
    `).join("");
  } catch (err) {
    tableBody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:var(--accent-red);">${err.message}</td></tr>`;
  }
}

async function closeLostReportPrompt(reportId) {
  if (!confirm("Are you sure you want to mark this lost report as CLOSED?")) return;
  try {
    await apiFetch(`/api/lost/${reportId}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status: "CLOSED" })
    });
    showToast("Report marked as CLOSED", "success");
    loadStudentDashboard();
  } catch (err) {
    showToast(err.message, "error");
  }
}

async function deleteLostReportPrompt(reportId) {
  if (!confirm("Are you sure you want to delete this lost report permanently?")) return;
  try {
    await apiFetch(`/api/lost/${reportId}`, {
      method: "DELETE"
    });
    showToast("Lost report deleted successfully", "success");
    loadStudentDashboard();
  } catch (err) {
    showToast(err.message, "error");
  }
}

// SCREEN 4: REPORT LOST ITEM
async function handleReportLostSubmit(e) {
  e.preventDefault();
  const payload = {
    title: document.getElementById("lost-title").value,
    categoryId: parseInt(document.getElementById("lost-category").value, 10),
    location: document.getElementById("lost-location").value,
    lostDate: document.getElementById("lost-date").value,
    description: document.getElementById("lost-desc").value
  };

  try {
    const res = await apiFetch("/api/lost", {
      method: "POST",
      body: JSON.stringify(payload)
    });
    if (res && res.success) {
      showToast("Lost item report submitted successfully!", "success");
      document.getElementById("form-report-lost").reset();
      navigateTo("view-student-dashboard");
    }
  } catch (err) {
    showToast(err.message, "error");
  }
}

// SCREEN 5: BROWSE LOST ITEMS
async function loadLostItems() {
  const container = document.getElementById("lost-items-container");
  container.innerHTML = `<div style="grid-column:1/-1; text-align:center; padding:40px;">Searching lost items...</div>`;

  const query = document.getElementById("browse-lost-query").value;
  const categoryId = document.getElementById("browse-lost-cat").value;
  const status = document.getElementById("browse-lost-status").value;

  const params = new URLSearchParams();
  if (query) params.append("query", query);
  if (categoryId) params.append("categoryId", categoryId);
  if (status) params.append("status", status);

  try {
    const res = await apiFetch(`/api/lost?${params.toString()}`);
    const items = (res && res.data) || [];

    if (items.length === 0) {
      container.innerHTML = `
        <div class="empty-state" style="grid-column:1/-1;">
          <div class="empty-icon">🔍</div>
          <div class="empty-title">No lost items found</div>
          <p>Try modifying your search keywords or filter criteria.</p>
        </div>
      `;
      return;
    }

    container.innerHTML = items.map(item => `
      <div class="item-card">
        <div class="item-header">
          <span class="item-title">${escapeHtml(item.title)}</span>
          <span class="badge badge-${item.status.toLowerCase()}">${item.status}</span>
        </div>
        <div class="item-meta">
          <span>🏷️ ${escapeHtml(item.category ? item.category.name : "N/A")}</span>
          <span>📍 ${escapeHtml(item.location)}</span>
          <span>📅 ${formatDate(item.lostDate)}</span>
        </div>
        <div class="item-desc">${escapeHtml(item.description)}</div>
        <div class="item-footer">
          <span>Reported by: ${escapeHtml(item.reporter ? item.reporter.fullName : "Anonymous")}</span>
          <button class="btn btn-sm btn-outline" onclick="openMatchesForReport(${item.id})">⚡ View Matches</button>
        </div>
      </div>
    `).join("");
  } catch (err) {
    container.innerHTML = `<div style="grid-column:1/-1; color:var(--accent-red);">${err.message}</div>`;
  }
}

// SCREEN 6: REPORT FOUND ITEM
async function handleReportFoundSubmit(e) {
  e.preventDefault();
  const payload = {
    title: document.getElementById("found-title").value,
    categoryId: parseInt(document.getElementById("found-category").value, 10),
    location: document.getElementById("found-location").value,
    foundDate: document.getElementById("found-date").value,
    description: document.getElementById("found-desc").value
  };

  try {
    const res = await apiFetch("/api/found", {
      method: "POST",
      body: JSON.stringify(payload)
    });
    if (res && res.success) {
      showToast("Found item registered successfully!", "success");
      document.getElementById("form-report-found").reset();
      navigateTo("view-browse-found");
    }
  } catch (err) {
    showToast(err.message, "error");
  }
}

// SCREEN 7: BROWSE FOUND ITEMS
async function loadFoundItems() {
  const container = document.getElementById("found-items-container");
  container.innerHTML = `<div style="grid-column:1/-1; text-align:center; padding:40px;">Loading found items...</div>`;

  const query = document.getElementById("browse-found-query").value;
  const categoryId = document.getElementById("browse-found-cat").value;
  const status = document.getElementById("browse-found-status").value;

  const params = new URLSearchParams();
  if (query) params.append("query", query);
  if (categoryId) params.append("categoryId", categoryId);
  if (status) params.append("status", status);

  try {
    const res = await apiFetch(`/api/found?${params.toString()}`);
    const items = (res && res.data) || [];

    if (items.length === 0) {
      container.innerHTML = `
        <div class="empty-state" style="grid-column:1/-1;">
          <div class="empty-icon">📦</div>
          <div class="empty-title">No found items found</div>
          <p>No matching found items match your filter criteria.</p>
        </div>
      `;
      return;
    }

    const canChangeStatus = currentUser && (currentUser.role === "STAFF" || currentUser.role === "ADMIN");

    container.innerHTML = items.map(item => `
      <div class="item-card">
        <div class="item-header">
          <span class="item-title">${escapeHtml(item.title)}</span>
          <span class="badge badge-${item.status.toLowerCase()}">${item.status}</span>
        </div>
        <div class="item-meta">
          <span>🏷️ ${escapeHtml(item.category ? item.category.name : "N/A")}</span>
          <span>📍 ${escapeHtml(item.location)}</span>
          <span>📅 ${formatDate(item.foundDate)}</span>
        </div>
        <div class="item-desc">${escapeHtml(item.description)}</div>
        <div class="item-footer">
          <span>Desk: ${escapeHtml(item.reporter ? item.reporter.fullName : "Campus Desk")}</span>
          ${canChangeStatus ? `<button class="btn btn-sm btn-secondary" onclick="openFoundStatusModal(${item.id}, '${item.status}')">Update Status</button>` : ""}
        </div>
      </div>
    `).join("");
  } catch (err) {
    container.innerHTML = `<div style="grid-column:1/-1; color:var(--accent-red);">${err.message}</div>`;
  }
}

function openFoundStatusModal(id, currentStatus) {
  let options = "";
  if (currentStatus === "FOUND" || currentStatus === "AVAILABLE") {
    options = `<option value="CLAIMED">CLAIMED (Mark as claimed by student)</option>`;
  } else if (currentStatus === "CLAIMED") {
    options = `
      <option value="RETURNED">RETURNED (Handed over to rightful owner)</option>
      <option value="FOUND">FOUND (Claim cancelled / unverified)</option>
    `;
  } else if (currentStatus === "RETURNED" && currentUser.role === "ADMIN") {
    options = `
      <option value="FOUND">FOUND (Admin reopen)</option>
      <option value="CLAIMED">CLAIMED (Admin re-verify)</option>
    `;
  } else {
    alert("Returned items can only be reopened by administrators.");
    return;
  }

  const content = `
    <p style="margin-bottom:16px;">Current Status: <span class="badge badge-${currentStatus.toLowerCase()}">${currentStatus}</span></p>
    <div class="form-group">
      <label class="form-label">Select New Transition Status:</label>
      <select id="modal-found-status" class="form-control">
        ${options}
      </select>
    </div>
    <div style="display:flex; justify-content:flex-end; gap:8px; margin-top:20px;">
      <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
      <button class="btn btn-primary" onclick="submitFoundStatusUpdate(${id})">Apply Transition</button>
    </div>
  `;
  openModal("Update Found Item Status", content);
}

async function submitFoundStatusUpdate(id) {
  const newStatus = document.getElementById("modal-found-status").value;
  try {
    await apiFetch(`/api/found/${id}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status: newStatus })
    });
    showToast(`Status updated to ${newStatus}`, "success");
    closeModal();
    loadFoundItems();
    if (currentView === "view-admin-dashboard") {
      loadAdminDashboard();
    }
  } catch (err) {
    showToast(err.message, "error");
  }
}

// SCREEN 8: SMART MATCH ENGINE
async function initMatchesScreen() {
  const selector = document.getElementById("match-lost-selector");
  selector.innerHTML = `<option value="">-- Choose an Open Lost Report --</option>`;

  try {
    const res = await apiFetch("/api/lost?status=OPEN");
    const reports = (res && res.data) || [];

    reports.forEach(r => {
      const opt = document.createElement("option");
      opt.value = r.id;
      opt.textContent = `#${r.id} - ${r.title} (${r.category ? r.category.name : "N/A"})`;
      selector.appendChild(opt);
    });
  } catch (e) {
    console.error("Failed to load reports for matching:", e);
  }
}

function openMatchesForReport(lostId) {
  navigateTo("view-matches");
  setTimeout(() => {
    const selector = document.getElementById("match-lost-selector");
    selector.value = lostId;
    runMatching();
  }, 100);
}

function handleMatchSelectorChange() {
  runMatching();
}

async function runMatching() {
  const lostId = document.getElementById("match-lost-selector").value;
  const container = document.getElementById("matches-results-container");

  if (!lostId) {
    container.innerHTML = `
      <div class="empty-state">
        <div class="empty-icon">⚡</div>
        <div class="empty-title">Select a lost report to see matched suggestions</div>
        <p>The system compares categories, normalized keywords, and location tokens.</p>
      </div>
    `;
    return;
  }

  container.innerHTML = `<div style="text-align:center; padding:40px;">Calculating similarity scores...</div>`;

  try {
    const res = await apiFetch(`/api/matches/${lostId}`);
    const matches = (res && res.data) || [];

    if (matches.length === 0) {
      container.innerHTML = `
        <div class="empty-state">
          <div class="empty-icon">🔍</div>
          <div class="empty-title">No candidate matches found</div>
          <p>No available found items matched the category or keywords for this report. New items will appear here when registered.</p>
        </div>
      `;
      return;
    }

    container.innerHTML = matches.map(m => `
      <div class="match-card">
        <div style="flex:1;">
          <div style="display:flex; align-items:center; gap:8px; margin-bottom:6px;">
            <strong style="font-size:1.1rem; color:var(--text-main);">#${m.foundItemId}: ${escapeHtml(m.foundItemTitle)}</strong>
            <span class="badge ${m.categoryMatched ? 'badge-available' : 'badge-closed'}">
              ${m.categoryMatched ? '✓ Category Matched' : 'Category Mismatch'}
            </span>
          </div>
          <p style="font-size:0.875rem; color:var(--text-muted); margin-bottom:8px;">
            📍 ${escapeHtml(m.foundItemLocation)} | 📅 Found: ${formatDate(m.foundDate)}
          </p>
          <p style="font-size:0.875rem; margin-bottom:10px;">${escapeHtml(m.foundItemDescription)}</p>
          <div>
            <span style="font-size:0.8rem; font-weight:600; color:var(--text-muted);">Matching Keywords:</span>
            ${m.matchingKeywords && m.matchingKeywords.length > 0
              ? m.matchingKeywords.map(k => `<span class="keyword-tag">${escapeHtml(k)}</span>`).join("")
              : '<span style="font-size:0.8rem; color:var(--text-muted);">None</span>'
            }
          </div>
        </div>
        <div style="display:flex; flex-direction:column; align-items:center; gap:8px;">
          <div class="match-score-badge">
            ${m.score}%
            <div style="font-size:0.65rem; color:var(--text-muted); text-transform:uppercase;">Similarity</div>
          </div>
          <button class="btn btn-success btn-sm" onclick="confirmMatchAction(${m.lostId}, ${m.foundItemId})">
            ✓ Confirm Match
          </button>
        </div>
      </div>
    `).join("");
  } catch (err) {
    container.innerHTML = `<div style="color:var(--accent-red); padding:20px;">${err.message}</div>`;
  }
}

async function confirmMatchAction(lostId, foundId) {
  if (!currentUser) {
    showToast("Please log in to confirm matches.", "info");
    navigateTo("view-login");
    return;
  }

  if (!confirm("Confirming this match will mark the Lost Report as MATCHED and the Found Item as CLAIMED. Proceed?")) {
    return;
  }

  try {
    const res = await apiFetch(`/api/matches/${lostId}/confirm?foundId=${foundId}`, {
      method: "POST"
    });
    showToast(res.message || "Match confirmed successfully!", "success");
    runMatching();
  } catch (err) {
    showToast(err.message, "error");
  }
}

// SCREEN 9: ADMIN DASHBOARD
async function loadAdminDashboard() {
  try {
    const res = await apiFetch("/api/admin/dashboard");
    const d = (res && res.data) || {};

    document.getElementById("adm-total-lost").textContent = d.totalLostReports || 0;
    document.getElementById("adm-total-found").textContent = d.totalFoundItems || 0;
    document.getElementById("adm-pending-lost").textContent = d.pendingLostReports || 0;
    document.getElementById("adm-matched-reports").textContent = d.matchedReports || 0;
    document.getElementById("adm-available-found").textContent = d.availableFoundItems || 0;
    document.getElementById("adm-claimed-found").textContent = d.claimedFoundItems || 0;
    document.getElementById("adm-returned-found").textContent = d.returnedFoundItems || 0;
    document.getElementById("adm-total-users").textContent = d.totalUsers || 0;

    // Recent Lost Table
    const lostTbody = document.getElementById("adm-recent-lost-table");
    const recentLost = d.recentLostReports || [];
    lostTbody.innerHTML = recentLost.length === 0
      ? `<tr><td colspan="5" style="text-align:center;">No lost reports yet.</td></tr>`
      : recentLost.map(r => `
        <tr>
          <td><strong>${escapeHtml(r.title)}</strong></td>
          <td>${escapeHtml(r.category ? r.category.name : "N/A")}</td>
          <td>${formatDate(r.lostDate)}</td>
          <td><span class="badge badge-${r.status.toLowerCase()}">${r.status}</span></td>
          <td>
            <button class="btn btn-sm btn-outline" onclick="openMatchesForReport(${r.id})">Matches</button>
          </td>
        </tr>
      `).join("");

    // Recent Found Table
    const foundTbody = document.getElementById("adm-recent-found-table");
    const recentFound = d.recentFoundItems || [];
    foundTbody.innerHTML = recentFound.length === 0
      ? `<tr><td colspan="5" style="text-align:center;">No found items yet.</td></tr>`
      : recentFound.map(f => `
        <tr>
          <td><strong>${escapeHtml(f.title)}</strong></td>
          <td>${escapeHtml(f.category ? f.category.name : "N/A")}</td>
          <td>${formatDate(f.foundDate)}</td>
          <td><span class="badge badge-${f.status.toLowerCase()}">${f.status}</span></td>
          <td>
            <button class="btn btn-sm btn-secondary" onclick="openFoundStatusModal(${f.id}, '${f.status}')">Update</button>
          </td>
        </tr>
      `).join("");
  } catch (err) {
    showToast(err.message, "error");
  }
}

// SCREEN 10: CATEGORY MANAGEMENT
async function loadAdminCategories() {
  const tbody = document.getElementById("admin-category-table-body");
  tbody.innerHTML = `<tr><td colspan="5" style="text-align:center;">Loading categories...</td></tr>`;

  try {
    const res = await apiFetch("/api/categories?all=true");
    const list = (res && res.data) || [];

    tbody.innerHTML = list.map(c => `
      <tr>
        <td>#${c.id}</td>
        <td><strong>${escapeHtml(c.name)}</strong></td>
        <td>${escapeHtml(c.description || "—")}</td>
        <td>
          <span class="badge ${c.active ? 'badge-available' : 'badge-closed'}">
            ${c.active ? 'ACTIVE' : 'INACTIVE'}
          </span>
        </td>
        <td>
          <button class="btn btn-sm btn-outline" onclick="editCategoryModal(${c.id}, '${escapeHtml(c.name)}', '${escapeHtml(c.description || '')}', ${c.active})">Edit</button>
          <button class="btn btn-sm btn-secondary" onclick="toggleCategoryActive(${c.id}, ${c.active})">${c.active ? 'Deactivate' : 'Activate'}</button>
          <button class="btn btn-sm btn-danger" onclick="deleteCategoryPrompt(${c.id}, '${escapeHtml(c.name)}')">Delete</button>
        </td>
      </tr>
    `).join("");
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="5" style="color:var(--accent-red); text-align:center;">${err.message}</td></tr>`;
  }
}

function openCategoryModal() {
  const content = `
    <div class="form-group">
      <label class="form-label">Category Name *</label>
      <input class="form-control" type="text" id="cat-modal-name" placeholder="e.g. Laboratory Equipment">
    </div>
    <div class="form-group">
      <label class="form-label">Description</label>
      <input class="form-control" type="text" id="cat-modal-desc" placeholder="Brief description of items in this category">
    </div>
    <div style="display:flex; justify-content:flex-end; gap:8px; margin-top:20px;">
      <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
      <button class="btn btn-primary" onclick="submitCreateCategory()">Create Category</button>
    </div>
  `;
  openModal("Add New Category", content);
}

function editCategoryModal(id, name, desc, active) {
  const content = `
    <div class="form-group">
      <label class="form-label">Category Name *</label>
      <input class="form-control" type="text" id="cat-modal-edit-name" value="${escapeHtml(name)}">
    </div>
    <div class="form-group">
      <label class="form-label">Description</label>
      <input class="form-control" type="text" id="cat-modal-edit-desc" value="${escapeHtml(desc)}">
    </div>
    <div style="display:flex; justify-content:flex-end; gap:8px; margin-top:20px;">
      <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
      <button class="btn btn-primary" onclick="submitEditCategory(${id}, ${active})">Save Changes</button>
    </div>
  `;
  openModal("Edit Category", content);
}

async function submitCreateCategory() {
  const name = document.getElementById("cat-modal-name").value;
  const description = document.getElementById("cat-modal-desc").value;
  if (!name.trim()) {
    showToast("Category name is required", "error");
    return;
  }

  try {
    await apiFetch("/api/categories", {
      method: "POST",
      body: JSON.stringify({ name: name.trim(), description: description.trim(), active: true })
    });
    showToast("Category created successfully", "success");
    closeModal();
    loadAdminCategories();
    loadGlobalCategories();
  } catch (err) {
    showToast(err.message, "error");
  }
}

async function submitEditCategory(id, active) {
  const name = document.getElementById("cat-modal-edit-name").value;
  const description = document.getElementById("cat-modal-edit-desc").value;
  if (!name.trim()) {
    showToast("Category name is required", "error");
    return;
  }

  try {
    await apiFetch(`/api/categories/${id}`, {
      method: "PUT",
      body: JSON.stringify({ name: name.trim(), description: description.trim(), active })
    });
    showToast("Category updated successfully", "success");
    closeModal();
    loadAdminCategories();
    loadGlobalCategories();
  } catch (err) {
    showToast(err.message, "error");
  }
}

async function toggleCategoryActive(id, currentActive) {
  try {
    await apiFetch(`/api/categories/${id}/active`, {
      method: "PATCH",
      body: JSON.stringify({ active: !currentActive })
    });
    showToast(`Category marked as ${!currentActive ? 'ACTIVE' : 'INACTIVE'}`, "success");
    loadAdminCategories();
    loadGlobalCategories();
  } catch (err) {
    showToast(err.message, "error");
  }
}

async function deleteCategoryPrompt(id, name) {
  if (!confirm(`Are you sure you want to delete category "${name}"? If any items reference it, deletion will be rejected.`)) {
    return;
  }

  try {
    await apiFetch(`/api/categories/${id}`, {
      method: "DELETE"
    });
    showToast("Category deleted successfully", "success");
    loadAdminCategories();
    loadGlobalCategories();
  } catch (err) {
    showToast(err.message, "error");
  }
}

// SCREEN 11: USER PROFILE
function loadUserProfile() {
  const box = document.getElementById("profile-details-box");
  if (!currentUser) {
    box.innerHTML = `<p>Not logged in.</p>`;
    return;
  }

  box.innerHTML = `
    <div style="display:flex; flex-direction:column; gap:14px;">
      <div>
        <span style="font-size:0.8rem; color:var(--text-muted); text-transform:uppercase;">Full Name</span>
        <div style="font-size:1.15rem; font-weight:700;">${escapeHtml(currentUser.fullName)}</div>
      </div>
      <div>
        <span style="font-size:0.8rem; color:var(--text-muted); text-transform:uppercase;">Campus Email</span>
        <div style="font-size:1rem; font-weight:500;">${escapeHtml(currentUser.email)}</div>
      </div>
      <div>
        <span style="font-size:0.8rem; color:var(--text-muted); text-transform:uppercase;">Account Role</span>
        <div><span class="badge badge-${currentUser.role.toLowerCase()}">${currentUser.role}</span></div>
      </div>
      <div>
        <span style="font-size:0.8rem; color:var(--text-muted); text-transform:uppercase;">Account Status</span>
        <div><span class="badge badge-open">ENABLED & ACTIVE</span></div>
      </div>
      <div>
        <span style="font-size:0.8rem; color:var(--text-muted); text-transform:uppercase;">Member Since</span>
        <div style="font-size:0.9rem;">${formatDate(currentUser.createdAt)}</div>
      </div>
    </div>
  `;
}

// Modal Helpers
function openModal(title, htmlContent) {
  document.getElementById("modal-title").textContent = title;
  document.getElementById("modal-body").innerHTML = htmlContent;
  document.getElementById("generic-modal").classList.add("active");
}

function closeModal() {
  document.getElementById("generic-modal").classList.remove("active");
}

function closeModalOnOverlay(e) {
  if (e.target.id === "generic-modal") {
    closeModal();
  }
}

// Toast Helpers
function showToast(message, type = "info") {
  const container = document.getElementById("toast-container");
  const toast = document.createElement("div");
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `<span>${type === 'success' ? '✓' : type === 'error' ? '⚠️' : 'ℹ️'}</span> <span>${escapeHtml(message)}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = "0";
    toast.style.transition = "opacity 0.3s";
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

// Utility Helpers
function escapeHtml(text) {
  if (!text) return "";
  const div = document.createElement("div");
  div.textContent = text;
  return div.innerHTML;
}

function formatDate(dateStr) {
  if (!dateStr) return "N/A";
  try {
    const d = new Date(dateStr);
    return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
  } catch (e) {
    return dateStr;
  }
}

function debounce(func, wait) {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
}

function toggleSidebar() {
  document.getElementById("sidebar").classList.toggle("open");
}
