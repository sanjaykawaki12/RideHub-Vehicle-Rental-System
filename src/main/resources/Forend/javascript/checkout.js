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
document.getElementById('backLink').addEventListener('click', ()=> history.back());

const vehicle = JSON.parse(sessionStorage.getItem('rentwheels_selected_vehicle') || 'null');
const shop = JSON.parse(sessionStorage.getItem('rentwheels_selected_shop') || 'null');

if(!vehicle){
  window.location.href = 'home.html';
}

document.getElementById('vehicleThumb').innerHTML = vehicle.imageUrl
  ? `<img src="${imageSrc(vehicle.imageUrl)}" alt="${vehicle.name}" style="width:100%; height:100%; object-fit:cover; border-radius:12px;">`
  : vehicleIcon(vehicle.type);
document.getElementById('vehicleName').textContent = vehicle.name;
document.getElementById('vehicleMeta').textContent = `${vehicle.type} · ${vehicle.model || ''} · ${shop ? shop.shopName : ''}`;
document.getElementById('billRate').textContent = `₹${vehicle.pricePerDay} / day`;

const checkoutMsg = document.getElementById('checkoutMsg');
function setMsg(el, text, type){ el.textContent = text; el.className = 'inline-msg show ' + type; }
function clearMsg(el){ el.className = 'inline-msg'; }

let myLicenses = [];

async function loadLicenses(){
  try{
    const res = await fetch(`${API_BASE}/user/licenses`, { headers: authHeaders(auth.token) });
    const body = await res.json();
    if(!res.ok || !body.success) throw new Error(body.message || 'Could not fetch licenses');
    myLicenses = body.data;

    const select = document.getElementById('licenseSelect');
    const hint = document.getElementById('licenseHint');
    if(myLicenses.length === 0){
      select.innerHTML = '<option value="">No license on file</option>';
      hint.innerHTML = `You need a license to book. <a id="addLicenseLink">Add one now</a>`;
      document.getElementById('addLicenseLink').addEventListener('click', ()=>{
        document.getElementById('licenseModal').classList.add('show');
      });
    }else{
      select.innerHTML = myLicenses.map(l => `<option value="${l.id}">${l.licenseNumber}</option>`).join('');
      hint.textContent = '';
    }
  }catch(err){
    setMsg(checkoutMsg, err.message, 'error');
  }
}

function recalcBill(){
  const start = document.getElementById('startDate').value;
  const end = document.getElementById('endDate').value;
  if(start && end){
    const days = Math.round((new Date(end) - new Date(start)) / 86400000);
    if(days > 0){
      document.getElementById('billDays').textContent = `${days} day${days>1?'s':''}`;
      document.getElementById('billTotal').textContent = `₹${(days * vehicle.pricePerDay).toFixed(2)}`;
      return days;
    }
  }
  document.getElementById('billDays').textContent = '0 days';
  document.getElementById('billTotal').textContent = '₹0';
  return 0;
}
document.getElementById('startDate').addEventListener('change', recalcBill);
document.getElementById('endDate').addEventListener('change', recalcBill);

// ---------- add license (inline modal) ----------
document.getElementById('closeLicenseModal').addEventListener('click', ()=>{
  document.getElementById('licenseModal').classList.remove('show');
});
document.getElementById('licenseForm').addEventListener('submit', async (e)=>{
  e.preventDefault();
  const msgEl = document.getElementById('licenseFormMsg');
  clearMsg(msgEl);
  const btn = document.getElementById('uploadLicenseBtn');
  btn.disabled = true;
  btn.textContent = 'Uploading…';

  const formData = new FormData();
  formData.append('licenseNumber', document.getElementById('licenseNumberInput').value.trim());
  formData.append('file', document.getElementById('licenseFileInput').files[0]);

  try{
    const res = await fetch(`${API_BASE}/user/licenses`, {
      method:'POST', headers: authHeaders(auth.token), body: formData
    });
    const body = await res.json();
    if(!res.ok || !body.success) throw new Error(body.message || 'Upload failed');

    setMsg(msgEl, 'License uploaded!', 'success');
    await loadLicenses();
    setTimeout(()=> document.getElementById('licenseModal').classList.remove('show'), 700);
  }catch(err){
    setMsg(msgEl, err.message, 'error');
  }finally{
    btn.disabled = false;
    btn.textContent = 'Upload';
  }
});

// Payment-method UI (backend remains mock/real-provider compatible)
const paymentFields = document.getElementById('paymentFields');
document.querySelectorAll('input[name="paymentMethod"]').forEach(r => r.addEventListener('change', () => {
  if (r.checked && r.value === 'UPI') paymentFields.innerHTML = '<input id="upiId" placeholder="Enter UPI ID (demo)">';
  else if (r.checked && r.value === 'CARD') paymentFields.innerHTML = '<input id="cardNumber" inputmode="numeric" maxlength="19" placeholder="Card number (demo)">';
  else if (r.checked && r.value === 'CASH') paymentFields.innerHTML = '<div class="hint">You can choose pay-at-pickup. In demo mode the booking will still be confirmed after checkout.</div>';
}));

// ---------- book & pay (single flow) ----------
document.getElementById('payBtn').addEventListener('click', async ()=>{
  clearMsg(checkoutMsg);

  const paymentMethod = document.querySelector('input[name=\"paymentMethod\"]:checked')?.value || 'UPI';
  if(paymentMethod === 'UPI' && !document.getElementById('upiId')?.value.trim()){
    setMsg(checkoutMsg, 'Please enter your UPI ID.', 'error');
    return;
  }
  if(paymentMethod === 'CARD' && (document.getElementById('cardNumber')?.value.replace(/\s/g,'').length || 0) < 12){
    setMsg(checkoutMsg, 'Please enter a valid demo card number.', 'error');
    return;
  }

  const startDate = document.getElementById('startDate').value;
  const endDate = document.getElementById('endDate').value;
  const licenseId = document.getElementById('licenseSelect').value;
  const days = recalcBill();

  if(!startDate || !endDate || days <= 0){
    setMsg(checkoutMsg, 'Please choose a valid date range.', 'error');
    return;
  }
  if(!licenseId){
    setMsg(checkoutMsg, 'Please add a driving license before booking.', 'error');
    document.getElementById('licenseModal').classList.add('show');
    return;
  }

  const btn = document.getElementById('payBtn');
  btn.disabled = true;
  btn.textContent = 'Booking…';

  try{
    // 1. create booking
    const bookingRes = await fetch(`${API_BASE}/user/bookings`, {
      method:'POST',
      headers: authHeaders(auth.token, { 'Content-Type':'application/json' }),
      body: JSON.stringify({ vehicleId: vehicle.id, licenseId: Number(licenseId), startDate, endDate })
    });
    const bookingBody = await bookingRes.json();
    if(!bookingRes.ok || !bookingBody.success) throw new Error(bookingBody.message || 'Booking failed');

    const booking = bookingBody.data;
    btn.textContent = 'Processing payment…';

    // 2. create order (mock or real depending on backend config)
    const orderRes = await fetch(`${API_BASE}/user/payments/create-order`, {
      method:'POST',
      headers: authHeaders(auth.token, { 'Content-Type':'application/json' }),
      body: JSON.stringify({ bookingId: booking.id })
    });
    const orderBody = await orderRes.json();
    if(!orderRes.ok || !orderBody.success) throw new Error(orderBody.message || 'Could not start payment');

    // 3. verify (mock mode auto-succeeds; real Razorpay would call this from the Checkout widget callback)
    const verifyRes = await fetch(`${API_BASE}/user/payments/verify`, {
      method:'POST',
      headers: authHeaders(auth.token, { 'Content-Type':'application/json' }),
      body: JSON.stringify({
        razorpayOrderId: orderBody.data.orderId,
        razorpayPaymentId: 'test_pay_' + Date.now(),
        razorpaySignature: 'test_sig'
      })
    });
    const verifyBody = await verifyRes.json();
    if(!verifyRes.ok || !verifyBody.success) throw new Error(verifyBody.message || 'Payment failed');

    setMsg(checkoutMsg, 'Payment successful! Redirecting to your bookings…', 'success');
    setTimeout(()=> window.location.href = 'bookings.html', 1200);
  }catch(err){
    setMsg(checkoutMsg, err.message, 'error');
    btn.disabled = false;
    btn.textContent = 'Book & Pay Now';
  }
});

loadLicenses();

