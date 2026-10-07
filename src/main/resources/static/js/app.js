/* ==========================================================================
   RentEase - Application Logic & API Integration
   ========================================================================== */

const API_BASE = '/api';

// Application State
let vehicles = [];
let customers = [];
let rentals = [];
let availableOnlyFilter = false;

// DOM Elements & Initialization
document.addEventListener('DOMContentLoaded', () => {
  fetchAppData();
});

// Fetch all resources from REST APIs
async function fetchAppData() {
  try {
    const [vehRes, custRes, rentRes] = await Promise.all([
      fetch(`${API_BASE}/vehicles`),
      fetch(`${API_BASE}/customers`),
      fetch(`${API_BASE}/rentals`)
    ]);

    if (!vehRes.ok || !custRes.ok || !rentRes.ok) {
      throw new Error('Failed to connect to backend service');
    }

    vehicles = await vehRes.json();
    customers = await custRes.json();
    rentals = await rentRes.json();

    updateServerStatus(true);
    updateStats();
    renderVehicles();
    renderCustomers();
    renderRentals();
  } catch (err) {
    console.error('Error fetching application data:', err);
    updateServerStatus(false);
    showToast('Backend connection failed. Ensure Spring Boot is running.', 'error');
  }
}

// Update Header Server Status Indicator
function updateServerStatus(isOnline) {
  const dot = document.getElementById('status-dot');
  const text = document.getElementById('status-text');
  
  if (isOnline) {
    dot.className = 'status-dot';
    text.textContent = 'Server Online';
  } else {
    dot.className = 'status-dot offline';
    text.textContent = 'Server Offline';
  }
}

// Update Top Statistics Cards
function updateStats() {
  const availableVehicles = vehicles.filter(v => v.available).length;
  const activeRentals = rentals.filter(r => r.status === 'ACTIVE').length;

  document.getElementById('stat-total-vehicles').textContent = vehicles.length;
  document.getElementById('stat-available-vehicles').textContent = availableVehicles;
  document.getElementById('stat-active-rentals').textContent = activeRentals;
  document.getElementById('stat-total-customers').textContent = customers.length;
}

// Navigation Tab Switching
function switchTab(tabName) {
  const tabs = ['vehicles', 'customers', 'rentals'];
  
  tabs.forEach(t => {
    document.getElementById(`tab-${t}`).classList.remove('active');
    document.getElementById(`section-${t}`).style.display = 'none';
  });

  document.getElementById(`tab-${tabName}`).classList.add('active');
  document.getElementById(`section-${tabName}`).style.display = 'block';
}

// Toggle Available Filter Button
function toggleAvailableFilter() {
  availableOnlyFilter = !availableOnlyFilter;
  const btn = document.getElementById('btn-filter-available');
  if (availableOnlyFilter) {
    btn.classList.remove('btn-secondary');
    btn.classList.add('btn-primary');
  } else {
    btn.classList.remove('btn-primary');
    btn.classList.add('btn-secondary');
  }
  renderVehicles();
}

// Render Vehicles Cards Grid
function renderVehicles() {
  const container = document.getElementById('vehicles-container');
  const typeFilter = document.getElementById('filter-vehicle-type').value;

  let filtered = vehicles;

  if (typeFilter !== 'ALL') {
    filtered = filtered.filter(v => v.type.toUpperCase() === typeFilter);
  }

  if (availableOnlyFilter) {
    filtered = filtered.filter(v => v.available);
  }

  if (filtered.length === 0) {
    container.innerHTML = `
      <div class="empty-state" style="grid-column: 1 / -1;">
        <div class="empty-icon">🚗</div>
        <div class="empty-title">No vehicles found</div>
        <p>No vehicles match your selected filters.</p>
      </div>
    `;
    return;
  }

  container.innerHTML = filtered.map(v => {
    const isAvailable = v.available;
    const badgeClass = isAvailable ? 'badge-available' : 'badge-rented';
    const badgeText = isAvailable ? 'Available' : 'Rented Out';

    return `
      <div class="vehicle-card">
        <div>
          <div class="vehicle-header">
            <span class="vehicle-type-tag">${v.type}</span>
            <span class="badge ${badgeClass}">${badgeText}</span>
          </div>
          <h3 class="vehicle-title">${escapeHtml(v.model)}</h3>
          <div class="vehicle-number">${escapeHtml(v.vehicleNumber)}</div>
        </div>

        <div class="vehicle-footer">
          <div class="daily-rate">
            <span class="rate-label">Daily Rate</span>
            <span class="rate-value">₹${v.dailyRate.toLocaleString()}</span>
          </div>

          ${isAvailable ? `
            <button class="btn btn-primary btn-sm" onclick="openCreateRentalModal(${v.id})">
              <span>🔑</span> Rent Now
            </button>
          ` : `
            <button class="btn btn-secondary btn-sm" disabled style="opacity: 0.5; cursor: not-allowed;">
              Rented
            </button>
          `}
        </div>
      </div>
    `;
  }).join('');
}

// Render Customers Data Table
function renderCustomers() {
  const tbody = document.getElementById('customers-table-body');

  if (customers.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="5" class="empty-state">
          <div class="empty-icon">👥</div>
          <div class="empty-title">No customers registered</div>
          <p>Click "Register Customer" to add a new customer.</p>
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = customers.map(c => `
    <tr>
      <td><strong>#${c.id}</strong></td>
      <td><strong>${escapeHtml(c.name)}</strong></td>
      <td>${escapeHtml(c.email)}</td>
      <td>${escapeHtml(c.phone)}</td>
      <td>
        <button class="btn btn-secondary btn-sm" onclick="openCreateRentalModalForCustomer(${c.id})">
          <span>🔑</span> Rent Vehicle
        </button>
      </td>
    </tr>
  `).join('');
}

// Render Rentals Data Table
function renderRentals() {
  const tbody = document.getElementById('rentals-table-body');

  if (rentals.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="7" class="empty-state">
          <div class="empty-icon">🔑</div>
          <div class="empty-title">No rental transactions</div>
          <p>Click "Rent a Vehicle" to create your first rental.</p>
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = rentals.map(r => {
    const isActive = r.status === 'ACTIVE';
    const statusBadge = isActive 
      ? '<span class="badge badge-rented">ACTIVE</span>'
      : '<span class="badge badge-returned">RETURNED</span>';

    return `
      <tr>
        <td><strong>#${r.id}</strong></td>
        <td>
          <div><strong>${escapeHtml(r.customer ? r.customer.name : 'Unknown')}</strong></div>
          <div style="font-size: 12px; color: var(--text-secondary);">${escapeHtml(r.customer ? r.customer.email : '')}</div>
        </td>
        <td>
          <div><strong>${escapeHtml(r.vehicle ? r.vehicle.model : 'Unknown')}</strong></div>
          <span class="vehicle-number">${escapeHtml(r.vehicle ? r.vehicle.vehicleNumber : '')}</span>
        </td>
        <td>${r.rentalDays} Days</td>
        <td><strong style="color: #10b981;">₹${(r.totalAmount || 0).toLocaleString()}</strong></td>
        <td>${statusBadge}</td>
        <td>
          <div style="display: flex; gap: 8px;">
            ${isActive ? `
              <button class="btn btn-success btn-sm" onclick="returnVehicle(${r.id})">
                <span>🔄</span> Return
              </button>
            ` : ''}
            <button class="btn btn-danger btn-sm" onclick="deleteRental(${r.id})">
              <span>🗑️</span> Cancel
            </button>
          </div>
        </td>
      </tr>
    `;
  }).join('');
}

// Modal Helpers
function openModal(id) {
  document.getElementById(id).classList.add('active');
}

function closeModal(id) {
  document.getElementById(id).classList.remove('active');
}

// Open Vehicle Modal
function openAddVehicleModal() {
  document.getElementById('form-create-vehicle').reset();
  openModal('modal-vehicle');
}

// Open Customer Modal
function openAddCustomerModal() {
  document.getElementById('form-create-customer').reset();
  openModal('modal-customer');
}

// Open Create Rental Modal
function openCreateRentalModal(preSelectedVehicleId = null) {
  const custSelect = document.getElementById('rental-customer');
  const vehSelect = document.getElementById('rental-vehicle');

  if (customers.length === 0) {
    showToast('Please register at least one customer first!', 'error');
    openAddCustomerModal();
    return;
  }

  const availableVehicles = vehicles.filter(v => v.available);
  if (availableVehicles.length === 0) {
    showToast('No vehicles are currently available for rent.', 'error');
    return;
  }

  // Populate Customers dropdown
  custSelect.innerHTML = customers.map(c => `
    <option value="${c.id}">${escapeHtml(c.name)} (${escapeHtml(c.email)})</option>
  `).join('');

  // Populate Available Vehicles dropdown
  vehSelect.innerHTML = availableVehicles.map(v => `
    <option value="${v.id}" data-rate="${v.dailyRate}" ${preSelectedVehicleId === v.id ? 'selected' : ''}>
      ${escapeHtml(v.model)} - ${escapeHtml(v.vehicleNumber)} (₹${v.dailyRate}/day)
    </option>
  `).join('');

  document.getElementById('rental-days').value = 1;
  calculateEstimatedCost();
  openModal('modal-rental');
}

function openCreateRentalModalForCustomer(customerId) {
  openCreateRentalModal();
  document.getElementById('rental-customer').value = customerId;
}

// Dynamic Estimated Cost Preview Calculation
function calculateEstimatedCost() {
  const vehSelect = document.getElementById('rental-vehicle');
  const daysInput = document.getElementById('rental-days');
  const preview = document.getElementById('cost-preview-val');

  const selectedOpt = vehSelect.options[vehSelect.selectedIndex];
  if (!selectedOpt) {
    preview.textContent = '₹0.00';
    return;
  }

  const dailyRate = parseFloat(selectedOpt.getAttribute('data-rate') || 0);
  const days = parseInt(daysInput.value) || 0;
  const total = dailyRate * days;

  preview.textContent = `₹${total.toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;
}

// Form Submission Handlers
async function handleCreateVehicle(e) {
  e.preventDefault();

  const body = {
    vehicleNumber: document.getElementById('veh-number').value.trim(),
    model: document.getElementById('veh-model').value.trim(),
    type: document.getElementById('veh-type').value,
    dailyRate: parseFloat(document.getElementById('veh-rate').value),
    available: true
  };

  try {
    const res = await fetch(`${API_BASE}/vehicles`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });

    const data = await res.json();

    if (!res.ok) {
      throw new Error(data.message || 'Failed to create vehicle');
    }

    showToast(`Vehicle ${data.vehicleNumber} added successfully!`, 'success');
    closeModal('modal-vehicle');
    fetchAppData();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function handleCreateCustomer(e) {
  e.preventDefault();

  const body = {
    name: document.getElementById('cust-name').value.trim(),
    email: document.getElementById('cust-email').value.trim(),
    phone: document.getElementById('cust-phone').value.trim()
  };

  try {
    const res = await fetch(`${API_BASE}/customers`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });

    const data = await res.json();

    if (!res.ok) {
      throw new Error(data.message || 'Failed to register customer');
    }

    showToast(`Customer ${data.name} registered successfully!`, 'success');
    closeModal('modal-customer');
    fetchAppData();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function handleCreateRental(e) {
  e.preventDefault();

  const body = {
    customerId: parseInt(document.getElementById('rental-customer').value),
    vehicleId: parseInt(document.getElementById('rental-vehicle').value),
    rentalDays: parseInt(document.getElementById('rental-days').value)
  };

  try {
    const res = await fetch(`${API_BASE}/rentals`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });

    const data = await res.json();

    if (!res.ok) {
      throw new Error(data.message || 'Failed to process rental');
    }

    showToast(`Rental created! Total Amount: ₹${data.totalAmount.toLocaleString()}`, 'success');
    closeModal('modal-rental');
    fetchAppData();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function returnVehicle(rentalId) {
  try {
    const res = await fetch(`${API_BASE}/rentals/${rentalId}/return`, {
      method: 'POST'
    });

    const data = await res.json();

    if (!res.ok) {
      throw new Error(data.message || 'Failed to return vehicle');
    }

    showToast(`Vehicle returned successfully! Status updated to RETURNED.`, 'success');
    fetchAppData();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function deleteRental(rentalId) {
  if (!confirm(`Are you sure you want to cancel/delete Rental #${rentalId}?`)) return;

  try {
    const res = await fetch(`${API_BASE}/rentals/${rentalId}`, {
      method: 'DELETE'
    });

    if (!res.ok) {
      throw new Error('Failed to delete rental');
    }

    showToast(`Rental #${rentalId} canceled successfully.`, 'success');
    fetchAppData();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

// Toast Notification Utility
function showToast(message, type = 'info') {
  const container = document.getElementById('toast-container');
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `
    <span>${type === 'success' ? '✅' : '⚠️'}</span>
    <span>${escapeHtml(message)}</span>
  `;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(100%)';
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

// Utility: HTML Sanitizer
function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
