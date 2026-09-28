const API_BASE = "http://localhost:8080/api";
const FILE_BASE = "http://localhost:8080"; // uploaded vehicle photos are served from here
function imageSrc(url){
  if(!url) return "";
  try {
    const raw=String(url).trim();
    if(raw.startsWith("http://") || raw.startsWith("https://")) return raw;
    const path=raw.replace(/^.*?(\/uploads\/)/,"/uploads/").replace("/uploads/licenses/vehicles/","/uploads/vehicles/");
    return FILE_BASE + (path.startsWith("/") ? path : "/"+path);
  } catch(e) { return url; }
}

// ---------- auth guard (customer pages) ----------
function requireUser(){
  const token = localStorage.getItem('rentwheels_token');
  const userRaw = localStorage.getItem('rentwheels_user');
  if(!token || !userRaw){
    window.location.href = 'login.html';
    return null;
  }
  const user = JSON.parse(userRaw);
  if(user.role !== 'USER'){
    window.location.href = 'login.html';
    return null;
  }
  return { token, user };
}

function authHeaders(token, extra = {}){
  return { 'Authorization': `Bearer ${token}`, ...extra };
}

function logout(){
  localStorage.removeItem('rentwheels_token');
  localStorage.removeItem('rentwheels_user');
  window.location.href = 'login.html';
}

// ---------- location (stored so it persists across pages) ----------
function getSavedLocation(){
  const raw = sessionStorage.getItem('rentwheels_location');
  return raw ? JSON.parse(raw) : null;
}
function saveLocation(lat, lng){
  sessionStorage.setItem('rentwheels_location', JSON.stringify({ lat, lng }));
}
function detectLocation(){
  return new Promise((resolve, reject)=>{
    if(!navigator.geolocation){
      reject(new Error('Geolocation is not supported by this browser.'));
      return;
    }
    navigator.geolocation.getCurrentPosition(
      pos => {
        const { latitude, longitude } = pos.coords;
        saveLocation(latitude, longitude);
        resolve({ lat: latitude, lng: longitude });
      },
      err => reject(new Error(err.message)),
      { enableHighAccuracy:false, timeout:10000 }
    );
  });
}

// ---------- vehicle type -> icon (used as a stand-in "image" since shops haven't uploaded photos) ----------
const VEHICLE_ICONS = {
  CAR: `<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M3 13l1.5-4.5A2 2 0 016.4 7h11.2a2 2 0 011.9 1.5L21 13" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/><rect x="2.5" y="13" width="19" height="6" rx="2" stroke="currentColor" stroke-width="1.6"/><circle cx="7" cy="19.5" r="1.6" fill="currentColor"/><circle cx="17" cy="19.5" r="1.6" fill="currentColor"/></svg>`,
  BIKE: `<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><circle cx="5.5" cy="17.5" r="3.5" stroke="currentColor" stroke-width="1.6"/><circle cx="18.5" cy="17.5" r="3.5" stroke="currentColor" stroke-width="1.6"/><path d="M5.5 17.5L9 9h5l3.5 8.5M9 9L7 6h3M12 9l2 4h4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>`,
  SCOOTER: `<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><circle cx="5.5" cy="18" r="2.5" stroke="currentColor" stroke-width="1.6"/><circle cx="17.5" cy="18" r="2.5" stroke="currentColor" stroke-width="1.6"/><path d="M5.5 18h6l2-7h3.5M17.5 18l-2-5M8 6h3l1 3" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>`,
  VAN: `<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M2.5 16V9.5A1.5 1.5 0 014 8h9l4 4h3.5a1.5 1.5 0 011.5 1.5V16" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><rect x="2" y="16" width="20" height="2.5" fill="currentColor" opacity="0.15"/><circle cx="7" cy="18.5" r="1.8" fill="currentColor"/><circle cx="17" cy="18.5" r="1.8" fill="currentColor"/></svg>`,
  TRUCK: `<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><rect x="1.5" y="8" width="12" height="8" rx="1" stroke="currentColor" stroke-width="1.6"/><path d="M13.5 11h4l3 3v2h-7v-5z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><circle cx="6" cy="18.5" r="1.8" fill="currentColor"/><circle cx="17.5" cy="18.5" r="1.8" fill="currentColor"/></svg>`
};
function vehicleIcon(type){
  return VEHICLE_ICONS[type] || VEHICLE_ICONS.CAR;
}

const CATEGORY_ICONS = {
  ALL: `<svg viewBox="0 0 24 24" fill="none"><rect x="3" y="3" width="7" height="7" rx="1.5" stroke="currentColor" stroke-width="1.6"/><rect x="14" y="3" width="7" height="7" rx="1.5" stroke="currentColor" stroke-width="1.6"/><rect x="3" y="14" width="7" height="7" rx="1.5" stroke="currentColor" stroke-width="1.6"/><rect x="14" y="14" width="7" height="7" rx="1.5" stroke="currentColor" stroke-width="1.6"/></svg>`,
  CAR: VEHICLE_ICONS.CAR,
  BIKE: VEHICLE_ICONS.BIKE,
  SCOOTER: VEHICLE_ICONS.SCOOTER,
  VAN: VEHICLE_ICONS.VAN,
  TRUCK: VEHICLE_ICONS.TRUCK
};
const auth = requireUser();
document.getElementById('logoutBtn').addEventListener('click', logout);

const params = new URLSearchParams(window.location.search);
const shopId = params.get('shopId');

let shop = JSON.parse(sessionStorage.getItem('rentwheels_selected_shop') || 'null');
async function loadShopDetails(){
  if(shop){
    document.getElementById('shopName').textContent = shop.shopName;
    document.getElementById('shopAddress').textContent = shop.address;
    return;
  }
  if(!shopId) return;
  try{
    const res = await fetch(`${API_BASE}/shops/${shopId}`);
    const body = await res.json();
    if(res.ok && body.success){
      shop = body.data;
      sessionStorage.setItem('rentwheels_selected_shop', JSON.stringify(shop));
      document.getElementById('shopName').textContent = shop.shopName;
      document.getElementById('shopAddress').textContent = shop.address;
    }
  }catch(_){ }
}

document.getElementById('backLink').addEventListener('click', ()=>{
  window.location.href = 'home.html';
});

const vehicleGrid = document.getElementById('vehicleGrid');
const vehiclesMsg = document.getElementById('vehiclesMsg');
const chipRow = document.getElementById('chipRow');
const vehicleSearch = document.getElementById('vehicleSearch');

let allVehicles = [];
let activeType = 'ALL';

function setMsg(el, text, type){ el.textContent = text; el.className = 'inline-msg show ' + type; }
function clearMsg(el){ el.className = 'inline-msg'; }

const CATEGORY_LABELS = { ALL:'All', CAR:'Cars', BIKE:'Bikes', SCOOTER:'Scooters', VAN:'Vans', TRUCK:'Trucks' };

function buildChips(types){
  const uniqueTypes = ['ALL', ...new Set(types)];
  chipRow.innerHTML = uniqueTypes.map(t => `
    <div class="chip ${t === activeType ? 'active' : ''}" data-type="${t}">
      ${CATEGORY_ICONS[t] || ''}
      <span>${CATEGORY_LABELS[t] || t}</span>
    </div>
  `).join('');

  chipRow.querySelectorAll('.chip').forEach(chip=>{
    chip.addEventListener('click', ()=>{
      activeType = chip.dataset.type;
      chipRow.querySelectorAll('.chip').forEach(c => c.classList.toggle('active', c.dataset.type === activeType));
      applyFilters();
    });
  });
}

function applyFilters(){
  const q = vehicleSearch.value.trim().toLowerCase();
  const filtered = allVehicles.filter(v=>{
    const matchesType = activeType === 'ALL' || v.type === activeType;
    const matchesSearch = v.name.toLowerCase().includes(q) || (v.model || '').toLowerCase().includes(q);
    return matchesType && matchesSearch;
  });
  renderVehicles(filtered);
}
vehicleSearch.addEventListener('input', applyFilters);

function renderVehicles(vehicles){
  vehicleGrid.innerHTML = '';
  if(vehicles.length === 0){
    vehicleGrid.innerHTML = '<div class="empty-state">No vehicles match your filters.</div>';
    return;
  }
  vehicles.forEach(v=>{
    const canBook = v.status === 'AVAILABLE';
    const card = document.createElement('div');
    card.className = 'vehicle-card';
    card.innerHTML = `
      <div class="vehicle-image">
        ${v.imageUrl ? `<img src="${imageSrc(v.imageUrl)}" alt="${v.name}" loading="lazy" style="width:100%;height:100%;object-fit:cover;display:block;" onerror="this.style.display='none';this.parentElement.classList.add('image-missing')">` : vehicleIcon(v.type)}
        <span class="status-tag ${v.status}">${v.status}</span>
      </div>
      <div class="vehicle-body">
        <h3>${v.name}</h3>
        <div class="meta">${v.type} · ${v.model || ''} · ${v.registrationNumber}</div>
        <div class="price-row">
          <div class="price">₹${v.pricePerDay}<span> /day</span></div>
          <button class="book-now-btn" ${canBook ? '' : 'disabled'}>${canBook ? 'Book Now' : 'Unavailable'}</button>
        </div>
      </div>
    `;
    card.querySelector('.book-now-btn').addEventListener('click', ()=>{
      if(!canBook) return;
      sessionStorage.setItem('rentwheels_selected_vehicle', JSON.stringify(v));
      sessionStorage.setItem('rentwheels_selected_shop', JSON.stringify(shop));
      window.location.href = `checkout.html?vehicleId=${v.id}`;
    });
    vehicleGrid.appendChild(card);
  });
}

async function loadVehicles(){
  clearMsg(vehiclesMsg);
  try{
    const res = await fetch(`${API_BASE}/vehicles/shop/${shopId}`);
    const body = await res.json();
    if(!res.ok || !body.success) throw new Error(body.message || 'Could not fetch vehicles');

    allVehicles = body.data;
    buildChips(allVehicles.map(v => v.type));
    applyFilters();
  }catch(err){
    setMsg(vehiclesMsg, err.message, 'error');
  }
}

loadShopDetails().then(loadVehicles);
