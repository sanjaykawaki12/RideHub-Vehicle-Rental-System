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

function setMsg(el, text, type){ el.textContent = text; el.className = 'inline-msg show ' + type; }
function clearMsg(el){ el.className = 'inline-msg'; }

// ---------- bookings ----------
const bookingList = document.getElementById('bookingList');
const bookingsMsg = document.getElementById('bookingsMsg');

async function loadBookings(){
  clearMsg(bookingsMsg);
  try{
    const res = await fetch(`${API_BASE}/user/bookings`, { headers: authHeaders(auth.token) });
    const body = await res.json();
    if(!res.ok || !body.success) throw new Error(body.message || 'Could not fetch bookings');

    bookingList.innerHTML = '';
    if(body.data.length === 0){
      bookingList.innerHTML = '<div class="empty-state">No bookings yet. <a href="home.html" style="color:var(--amber-deep); font-weight:600;">Find a shop</a> to get started.</div>';
      return;
    }

    body.data
      .sort((a,b)=> new Date(b.createdAt) - new Date(a.createdAt))
      .forEach(b=>{
        const row = document.createElement('div');
        row.className = 'list-row';
        row.innerHTML = `
          <div class="info">
            <strong>${b.vehicle.name} · ${b.shop.shopName}</strong>
            <span>${b.startDate} → ${b.endDate} · ₹${b.totalAmount}</span>
          </div>
          <div style="display:flex; align-items:center; gap:10px;">
            <span class="badge ${b.status}">${b.status.replace('_',' ')}</span>
            ${b.status === 'PENDING_PAYMENT' ? `<button class="book-now-btn resume-pay" data-booking='${JSON.stringify(b)}'>Pay now</button>` : ''}
          </div>
        `;
        bookingList.appendChild(row);
      });

    bookingList.querySelectorAll('.resume-pay').forEach(btn=>{
      btn.addEventListener('click', ()=>{
        const booking = JSON.parse(btn.dataset.booking);
        sessionStorage.setItem('rentwheels_selected_vehicle', JSON.stringify(booking.vehicle));
        sessionStorage.setItem('rentwheels_selected_shop', JSON.stringify(booking.shop));
        sessionStorage.setItem('rentwheels_resume_booking_id', booking.id);
        // Simplest resume path: send them to pay directly via the checkout page's payment step.
        // (For a booking already created, we just re-run create-order/verify with its id.)
        resumePayment(booking);
      });
    });
  }catch(err){
    setMsg(bookingsMsg, err.message, 'error');
  }
}

async function resumePayment(booking){
  try{
    const orderRes = await fetch(`${API_BASE}/user/payments/create-order`, {
      method:'POST',
      headers: authHeaders(auth.token, { 'Content-Type':'application/json' }),
      body: JSON.stringify({ bookingId: booking.id })
    });
    const orderBody = await orderRes.json();
    if(!orderRes.ok || !orderBody.success) throw new Error(orderBody.message || 'Could not start payment');

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

    setMsg(bookingsMsg, 'Payment successful!', 'success');
    await loadBookings();
  }catch(err){
    setMsg(bookingsMsg, err.message, 'error');
  }
}

// ---------- licenses ----------
const licenseList = document.getElementById('licenseList');
const licenseMsg = document.getElementById('licenseMsg');

async function loadLicenses(){
  clearMsg(licenseMsg);
  try{
    const res = await fetch(`${API_BASE}/user/licenses`, { headers: authHeaders(auth.token) });
    const body = await res.json();
    if(!res.ok || !body.success) throw new Error(body.message || 'Could not fetch licenses');

    licenseList.innerHTML = '';
    if(body.data.length === 0){
      licenseList.innerHTML = '<div class="empty-state">No licenses uploaded yet.</div>';
      return;
    }
    body.data.forEach(lic=>{
      const row = document.createElement('div');
      row.className = 'list-row';
      row.innerHTML = `
        <div class="info">
          <strong>${lic.licenseNumber}</strong>
          <span>Uploaded ${new Date(lic.uploadedAt).toLocaleDateString()}</span>
        </div>
        <span class="badge ${lic.verified ? 'COMPLETED' : 'PENDING_PAYMENT'}">${lic.verified ? 'Verified' : 'Pending review'}</span>
      `;
      licenseList.appendChild(row);
    });
  }catch(err){
    setMsg(licenseMsg, err.message, 'error');
  }
}

document.getElementById('addLicenseBtn').addEventListener('click', ()=>{
  document.getElementById('licenseModal').classList.add('show');
});
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
    document.getElementById('licenseForm').reset();
    await loadLicenses();
    setTimeout(()=> document.getElementById('licenseModal').classList.remove('show'), 700);
  }catch(err){
    setMsg(msgEl, err.message, 'error');
  }finally{
    btn.disabled = false;
    btn.textContent = 'Upload';
  }
});

loadBookings();
loadLicenses();

if(window.location.hash === '#licenses'){
  document.getElementById('licenses').scrollIntoView();
}

async function cancelBooking(id){if(!confirm('Cancel this booking?'))return;const r=await fetch(`${API}/user/bookings/${id}/cancel`,{method:'PATCH',headers:headers});const b=await r.json();if(!r.ok){alert(b.message||'Cancellation failed');return;}load();}
