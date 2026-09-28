const API_BASE = "http://localhost:8080/api";
const FILE_BASE = "http://localhost:8080"; // uploaded images are served from here
function imageSrc(url){
  if(!url) return "";
  try {
    const raw=String(url).trim();
    if(raw.startsWith("http://") || raw.startsWith("https://")) return raw;
    const path=raw.replace(/^.*?(\/uploads\/)/,"/uploads/").replace("/uploads/licenses/vehicles/","/uploads/vehicles/");
    return FILE_BASE + (path.startsWith("/") ? path : "/"+path);
  } catch(e) { return url; }
}

function requireOwner(){
  const token = localStorage.getItem('rentwheels_token');
  const userRaw = localStorage.getItem('rentwheels_user');
  if(!token || !userRaw){ window.location.href = 'login.html'; return null; }
  const user = JSON.parse(userRaw);
  if(user.role !== 'OWNER'){ window.location.href = 'login.html'; return null; }
  return { token, user };
}
function authHeaders(token, extra = {}){ return { 'Authorization': `Bearer ${token}`, ...extra }; }
function logout(){
  localStorage.removeItem('rentwheels_token');
  localStorage.removeItem('rentwheels_user');
  window.location.href = 'login.html';
}

const auth = requireOwner();
document.getElementById('logoutBtn').addEventListener('click', logout);

function setMsg(el, text, type){ el.textContent = text; el.className = 'inline-msg show ' + type; }
function clearMsg(el){ el.className = 'inline-msg'; }

let myShop = null;
let editingVehicleId = null;

const vehicleTable = document.getElementById('vehicleTable');
const vehiclesMsg = document.getElementById('vehiclesMsg');
const vehicleModal = document.getElementById('vehicleModal');

// ================= INIT: get owner's shop =================
async function init(){
  try{
    const res = await fetch(`${API_BASE}/owner/shops`, { headers: authHeaders(auth.token) });
    const body = await res.json();
    if(!res.ok || !body.success) throw new Error(body.message || 'Could not fetch shop');

    if(body.data.length === 0){
      vehicleTable.innerHTML = '<div class="empty-state">You need to create a shop first. <a href="owner-dashboard.html" style="color:var(--teal-deep); font-weight:600;">Go to Dashboard</a></div>';
      document.getElementById('addVehicleBtn').disabled = true;
      return;
    }
    myShop = body.data[0];
    loadVehicles();
  }catch(err){
    setMsg(vehiclesMsg, err.message, 'error');
  }
}

// ================= LOAD VEHICLES =================
async function loadVehicles(){
  clearMsg(vehiclesMsg);
  try{
    const res = await fetch(`${API_BASE}/vehicles/shop/${myShop.id}`);
    const body = await res.json();
    if(!res.ok || !body.success) throw new Error(body.message || 'Could not fetch vehicles');

    vehicleTable.innerHTML = '';
    if(body.data.length === 0){
      vehicleTable.innerHTML = '<div class="empty-state">No vehicles yet. Click "+ Add vehicle" to list your first one.</div>';
      return;
    }
    body.data.forEach(v=>{
      const row = document.createElement('div');
      row.className = 'vehicle-row';
      row.innerHTML = `
        <div class="row-thumb">
          ${v.imageUrl ? `<img src="${imageSrc(v.imageUrl)}" alt="${v.name}">` : `<div class="no-image">No photo</div>`}
        </div>
        <div class="info">
          <strong>${v.name}</strong>
          <span>${v.type} · ${v.model || ''} · ${v.registrationNumber}</span>
        </div>
        <div class="price">₹${v.pricePerDay}/day</div>
        <div class="actions">
          <select class="status-select" data-id="${v.id}">
            <option value="AVAILABLE" ${v.status==='AVAILABLE'?'selected':''}>Available</option>
            <option value="RENTED" ${v.status==='RENTED'?'selected':''}>Rented</option>
            <option value="MAINTENANCE" ${v.status==='MAINTENANCE'?'selected':''}>Maintenance</option>
            <option value="INACTIVE" ${v.status==='INACTIVE'?'selected':''}>Inactive</option>
          </select>
          <button class="icon-btn edit" title="Edit" data-vehicle='${JSON.stringify(v)}'>
            <svg viewBox="0 0 24 24" fill="none"><path d="M12 20h9M16.5 3.5a2.1 2.1 0 013 3L7 19l-4 1 1-4L16.5 3.5z" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </button>
          <button class="icon-btn delete" title="Delete" data-id="${v.id}">
            <svg viewBox="0 0 24 24" fill="none"><path d="M3 6h18M8 6V4a2 2 0 012-2h4a2 2 0 012 2v2m3 0l-1 14a2 2 0 01-2 2H7a2 2 0 01-2-2L4 6h16z" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </button>
        </div>
      `;
      vehicleTable.appendChild(row);
    });

    // status change
    vehicleTable.querySelectorAll('.status-select').forEach(sel=>{
      sel.addEventListener('change', async ()=>{
        try{
          const res = await fetch(`${API_BASE}/owner/vehicles/${sel.dataset.id}/status?status=${sel.value}`, {
            method:'PATCH', headers: authHeaders(auth.token)
          });
          const body = await res.json();
          if(!res.ok || !body.success) throw new Error(body.message || 'Status update failed');
          setMsg(vehiclesMsg, 'Status updated.', 'success');
        }catch(err){
          setMsg(vehiclesMsg, err.message, 'error');
          loadVehicles();
        }
      });
    });

    // edit
    vehicleTable.querySelectorAll('.icon-btn.edit').forEach(btn=>{
      btn.addEventListener('click', ()=> openVehicleModal(JSON.parse(btn.dataset.vehicle)));
    });

    // delete
    vehicleTable.querySelectorAll('.icon-btn.delete').forEach(btn=>{
      btn.addEventListener('click', async ()=>{
        if(!confirm('Delete this vehicle? This cannot be undone.')) return;
        try{
          const res = await fetch(`${API_BASE}/owner/vehicles/${btn.dataset.id}`, {
            method:'DELETE', headers: authHeaders(auth.token)
          });
          const body = await res.json();
          if(!res.ok || !body.success) throw new Error(body.message || 'Delete failed');
          setMsg(vehiclesMsg, 'Vehicle deleted.', 'success');
          loadVehicles();
        }catch(err){
          setMsg(vehiclesMsg, err.message, 'error');
        }
      });
    });
  }catch(err){
    setMsg(vehiclesMsg, err.message, 'error');
  }
}

// ================= ADD / EDIT MODAL =================
function openVehicleModal(vehicle){
  editingVehicleId = vehicle ? vehicle.id : null;
  document.getElementById('vehicleModalTitle').textContent = vehicle ? 'Edit vehicle' : 'Add vehicle';
  document.getElementById('vehicleSubmitBtn').querySelector('span').textContent = vehicle ? 'Save changes' : 'Add vehicle';
  clearMsg(document.getElementById('vehicleFormMsg'));

  document.getElementById('vName').value = vehicle ? vehicle.name : '';
  document.getElementById('vType').value = vehicle ? vehicle.type : 'CAR';
  document.getElementById('vModel').value = vehicle ? (vehicle.model || '') : '';
  document.getElementById('vReg').value = vehicle ? vehicle.registrationNumber : '';
  document.getElementById('vPrice').value = vehicle ? vehicle.pricePerDay : '';
  document.getElementById('vReg').disabled = !!vehicle; // registration number fixed once created
  document.getElementById('vImage').value = '';

  const previewWrap = document.getElementById('imagePreviewWrap');
  const preview = document.getElementById('imagePreview');
  if(vehicle && vehicle.imageUrl){
    preview.src = imageSrc(vehicle.imageUrl);
    previewWrap.style.display = 'block';
  }else{
    previewWrap.style.display = 'none';
  }

  vehicleModal.classList.add('show');
}
document.getElementById('addVehicleBtn').addEventListener('click', ()=> openVehicleModal(null));
document.getElementById('closeVehicleModal').addEventListener('click', ()=> vehicleModal.classList.remove('show'));

document.getElementById('vImage').addEventListener('change', (e)=>{
  const file = e.target.files[0];
  const previewWrap = document.getElementById('imagePreviewWrap');
  const preview = document.getElementById('imagePreview');
  if(file){
    preview.src = URL.createObjectURL(file);
    previewWrap.style.display = 'block';
  }
});

document.getElementById('vehicleForm').addEventListener('submit', async (e)=>{
  e.preventDefault();
  const msgEl = document.getElementById('vehicleFormMsg');
  clearMsg(msgEl);
  const btn = document.getElementById('vehicleSubmitBtn');
  btn.disabled = true;

  const payload = {
    name: document.getElementById('vName').value.trim(),
    type: document.getElementById('vType').value,
    model: document.getElementById('vModel').value.trim(),
    registrationNumber: document.getElementById('vReg').value.trim(),
    pricePerDay: parseFloat(document.getElementById('vPrice').value)
  };

  try{
    let res, body;
    if(editingVehicleId){
      res = await fetch(`${API_BASE}/owner/vehicles/${editingVehicleId}`, {
        method:'PUT',
        headers: authHeaders(auth.token, { 'Content-Type':'application/json' }),
        body: JSON.stringify(payload)
      });
    }else{
      res = await fetch(`${API_BASE}/owner/shops/${myShop.id}/vehicles`, {
        method:'POST',
        headers: authHeaders(auth.token, { 'Content-Type':'application/json' }),
        body: JSON.stringify(payload)
      });
    }
    body = await res.json();
    if(!res.ok || !body.success) throw new Error(body.message || 'Save failed');

    const savedVehicleId = body.data.id;

    // If a photo was chosen, upload it right after the vehicle is saved
    const imageFile = document.getElementById('vImage').files[0];
    if(imageFile){
      const formData = new FormData();
      formData.append('file', imageFile);
      const imgRes = await fetch(`${API_BASE}/owner/vehicles/${savedVehicleId}/image`, {
        method:'POST',
        headers: authHeaders(auth.token), // don't set Content-Type manually for FormData
        body: formData
      });
      const imgBody = await imgRes.json();
      if(!imgRes.ok || !imgBody.success) throw new Error(imgBody.message || 'Vehicle saved, but photo upload failed');
    }

    setMsg(msgEl, editingVehicleId ? 'Vehicle updated!' : 'Vehicle added!', 'success');
    loadVehicles();
    setTimeout(()=> vehicleModal.classList.remove('show'), 700);
  }catch(err){
    setMsg(msgEl, err.message, 'error');
  }finally{
    btn.disabled = false;
  }
});

init();
