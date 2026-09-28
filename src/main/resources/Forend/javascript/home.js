const API_BASE = "http://localhost:8080/api";

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

const shopList = document.getElementById('shopList');
const shopsMsg = document.getElementById('shopsMsg');
const locText = document.getElementById('locText');
const locationPill = document.getElementById('locationPill');
const searchInput = document.getElementById('searchInput');

let allShops = [];

function setMsg(el, text, type){
  el.textContent = text;
  el.className = 'inline-msg show ' + type;
}
function clearMsg(el){ el.className = 'inline-msg'; }

async function loadShops(lat, lng){
  clearMsg(shopsMsg);
  try{
    let res = await fetch(`${API_BASE}/shops/nearby?lat=${lat}&lng=${lng}&radiusKm=20`);
    let body = await res.json();
    if(!res.ok || !body.success) throw new Error(body.message || 'Could not fetch shops');
    allShops = body.data || [];
    if(allShops.length === 0){
      return loadAllShops();
    }
    renderShops(allShops);
  }catch(err){
    // If location is unavailable or there are no nearby results, still show available shops.
    try{
      const fallback = await fetch(`${API_BASE}/shops/all`);
      const fallbackBody = await fallback.json();
      if(fallback.ok && fallbackBody.success){
        allShops = fallbackBody.data;
        renderShops(allShops);
        return;
      }
    }catch(_){ }
    setMsg(shopsMsg, err.message, 'error');
  }
}

async function loadAllShops(){
  clearMsg(shopsMsg);
  try{
    const res = await fetch(`${API_BASE}/shops/all`);
    const body = await res.json();
    if(!res.ok || !body.success) throw new Error(body.message || 'Could not fetch shops');
    allShops = body.data;
    renderShops(allShops);
  }catch(err){ setMsg(shopsMsg, err.message, 'error'); }
}

function renderShops(shops){
  shopList.innerHTML = '';
  const count = document.getElementById('shopCount');
  if(count) count.textContent = `${shops.length} shop${shops.length === 1 ? '' : 's'} found`;
  if(shops.length === 0){
    shopList.innerHTML = '<div class="empty-state">No rental shops found nearby yet.</div>';
    return;
  }
  shops.forEach(shop=>{
    const card = document.createElement('div');
    card.className = 'shop-card';
    card.innerHTML = `
      <div class="shop-thumb">
        <svg viewBox="0 0 24 24" fill="none"><path d="M3 10.5L12 4l9 6.5M5 10v9a1 1 0 001 1h4v-5h4v5h4a1 1 0 001-1v-9" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>
      </div>
      <div class="shop-info">
        <h3>${shop.shopName}</h3>
        <p>${shop.address}</p>
        <div class="tags">
          <span class="tag">${shop.contactPhone || 'No contact listed'}</span>
        </div>
      </div>
    `;
    card.addEventListener('click', ()=>{
      sessionStorage.setItem('rentwheels_selected_shop', JSON.stringify(shop));
      window.location.href = `shop.html?shopId=${shop.id}`;
    });
    shopList.appendChild(card);
  });
}

searchInput.addEventListener('input', ()=>{
  const q = searchInput.value.trim().toLowerCase();
  const filtered = allShops.filter(s =>
    s.shopName.toLowerCase().includes(q) || s.address.toLowerCase().includes(q)
  );
  renderShops(filtered);
});

async function initLocation(){
  const saved = getSavedLocation();
  if(saved){
    locText.textContent = `Near ${saved.lat.toFixed(3)}, ${saved.lng.toFixed(3)}`;
    await loadShops(saved.lat, saved.lng);
    return;
  }
  try{
    const loc = await detectLocation();
    locText.textContent = `Near ${loc.lat.toFixed(3)}, ${loc.lng.toFixed(3)}`;
    await loadShops(loc.lat, loc.lng);
  }catch(err){
    locText.textContent = 'Showing all shops';
    await loadAllShops();
  }
}

locationPill.addEventListener('click', async ()=>{
  locText.textContent = 'Detecting your location…';
  try{
    const loc = await detectLocation();
    locText.textContent = `Near ${loc.lat.toFixed(3)}, ${loc.lng.toFixed(3)}`;
    await loadShops(loc.lat, loc.lng);
  }catch(err){
    locText.textContent = 'Showing all shops';
    await loadAllShops();
  }
});

initLocation();
