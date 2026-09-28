const API='/api';
const FILE_BASE='http://localhost:8080';
const imageSrc=url=>url ? `${FILE_BASE}${url.replace('/uploads/licenses/vehicles/','/uploads/vehicles/')}` : '';
const $=id=>document.getElementById(id);
const icons={CAR:'🚘',BIKE:'🏍',SCOOTER:'🛵',VAN:'🚐',TRUCK:'🚚'};
function token(){return localStorage.getItem('rentwheels_token')||localStorage.getItem('rh_token')}
function auth(){try{return JSON.parse(localStorage.getItem('rentwheels_user')||localStorage.getItem('rh_user')||'null')}catch{return null}}
function headers(extra={}){return {'Authorization':`Bearer ${token()}`,...extra}}
const me=auth();if(!token()||!me||me.role!=='OWNER')location.href='/Forend/Html/login.html';
function toast(msg,good=true){const e=document.createElement('div');e.className='toast '+(good?'good':'bad');e.textContent=msg;$('toastRoot').appendChild(e);setTimeout(()=>e.remove(),2800)}
function openModal(){ $('vehicleModal').classList.add('show') } function closeModal(){ $('vehicleModal').classList.remove('show') }
document.querySelector('[data-close]').onclick=closeModal;$('vehicleModal').onclick=e=>{if(e.target===$('vehicleModal'))closeModal()};$('logoutBtn').onclick=()=>{localStorage.removeItem('rentwheels_token');localStorage.removeItem('rentwheels_user');location.href='/Forend/Html/login.html'};
let shop=null,vehicles=[],bookings=[];
async function json(url,opt={}){const r=await fetch(url,{...opt,headers:{...headers(),...(opt.headers||{})}});let b={};try{b=await r.json()}catch{}if(!r.ok||b.success===false)throw Error(b.message||`Request failed (${r.status})`);return b.data}
async function init(){try{const shops=await json(`${API}/owner/shops`);if(!shops.length){$('createShopPanel').style.display='grid';$('dashboardContent').style.display='none';return}shop=shops[0];showDashboard();await refreshAll()}catch(e){toast(e.message,false)}}
function showDashboard(){ $('createShopPanel').style.display='none';$('dashboardContent').style.display='block';$('shopNameDisplay').textContent=shop.shopName;$('shopAddressDisplay').textContent=shop.address||'Location not provided' }
$('useLocationBtn').onclick=()=>navigator.geolocation?.getCurrentPosition(p=>{$('shopLat').value=p.coords.latitude.toFixed(6);$('shopLng').value=p.coords.longitude.toFixed(6)},()=>toast('Location permission denied',false),{timeout:10000});
$('createShopForm').onsubmit=async e=>{e.preventDefault();try{shop=await json(`${API}/owner/shops`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({shopName:$('shopName').value.trim(),address:$('shopAddress').value.trim(),latitude:+$('shopLat').value,longitude:+$('shopLng').value,contactPhone:$('shopPhone').value.trim()||null})});toast('Shop created successfully');showDashboard();await refreshAll()}catch(x){$('createShopMsg').textContent=x.message}}
$('editShopBtn').onclick=()=>{ $('shopName').value=shop.shopName;$('shopAddress').value=shop.address||'';$('shopLat').value=shop.latitude;$('shopLng').value=shop.longitude;$('shopPhone').value=shop.contactPhone||'';$('createShopPanel').style.display='grid';$('dashboardContent').style.display='none';const old=$('createShopForm');old.onsubmit=async e=>{e.preventDefault();try{shop=await json(`${API}/owner/shops/${shop.id}`,{method:'PUT',headers:{'Content-Type':'application/json'},body:JSON.stringify({shopName:$('shopName').value.trim(),address:$('shopAddress').value.trim(),latitude:+$('shopLat').value,longitude:+$('shopLng').value,contactPhone:$('shopPhone').value.trim()||null})});toast('Shop updated');old.onsubmit=createShopHandler;showDashboard();await refreshAll()}catch(x){$('createShopMsg').textContent=x.message}}}
async function createShopHandler(e){e.preventDefault();try{shop=await json(`${API}/owner/shops`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({shopName:$('shopName').value.trim(),address:$('shopAddress').value.trim(),latitude:+$('shopLat').value,longitude:+$('shopLng').value,contactPhone:$('shopPhone').value.trim()||null})});toast('Shop created successfully');showDashboard();await refreshAll()}catch(x){$('createShopMsg').textContent=x.message}}
$('addVehicleBtn').onclick=()=>openModal();
$('vImage').onchange=()=>{const file=$('vImage').files[0];const box=$('vImagePreview');box.innerHTML=file?`<img src="${URL.createObjectURL(file)}" alt="Preview">`:''};
$('vehicleForm').onsubmit=async e=>{
  e.preventDefault();
  try{
    const created=await json(`${API}/owner/shops/${shop.id}/vehicles`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({name:$('vName').value.trim(),type:$('vType').value,model:$('vModel').value.trim()||null,registrationNumber:$('vReg').value.trim(),pricePerDay:+$('vPrice').value})});
    const file=$('vImage').files[0];
    if(file){
      const fd=new FormData();fd.append('file',file);
      const upload=await fetch(`${API}/owner/vehicles/${created.id}/image`,{method:'POST',headers:headers(),body:fd});
      const uploadBody=await upload.json();
      if(!upload.ok||!uploadBody.success) throw Error(uploadBody.message||'Vehicle added, but image upload failed');
    }
    toast('Vehicle added to your fleet');e.target.reset();$('vImagePreview').innerHTML='';closeModal();await refreshAll()
  }catch(x){toast(x.message,false)}
};
async function refreshAll(){vehicles=await json(`${API}/vehicles/shop/${shop.id}`);renderVehicles();try{bookings=await json(`${API}/owner/shops/${shop.id}/bookings`)}catch{bookings=[]}renderBookings();try{const d=await json(`${API}/owner/dashboard/shops/${shop.id}/yearly`);renderStats(d);renderRevenue(d.stats||[])}catch(x){toast('Dashboard stats could not be loaded',false)}}
function renderVehicles(){const e=$('vehicleList');e.innerHTML=vehicles.length?'':'<div class="empty"><p class="muted">No vehicles yet. Add your first ride to start accepting bookings.</p></div>';vehicles.slice(0,6).forEach(v=>{const x=document.createElement('div');x.className='fleet-item';const media=v.imageUrl?`<img class="fleet-photo" src="${imageSrc(v.imageUrl)}" alt="${v.name}">`:`<div class="fleet-icon">${icons[v.type]||'🚗'}</div>`;x.innerHTML=`<div class="fleet-main">${media}<div><b>${v.name}</b><span>${v.model||v.type} · ${v.registrationNumber}</span></div></div><span class="status">${v.status}</span>`;e.appendChild(x)})}
function renderBookings(){const e=$('bookingList');e.innerHTML=bookings.length?'':'<p class="muted">No bookings yet. They will appear here when customers reserve your vehicles.</p>';bookings.slice(0,5).forEach(b=>{const x=document.createElement('div');x.className='booking-item';x.innerHTML=`<div><b>${b.vehicle?.name||'Vehicle'} · #${b.id}</b><span>${b.startDate} → ${b.endDate}</span></div><span class="status">${b.status}</span>`;e.appendChild(x)})}
function renderStats(d){$('statTotal').textContent=d.totalVehicles??vehicles.length;$('statAvailable').textContent=d.availableVehicles??vehicles.filter(v=>v.status==='AVAILABLE').length;$('statRented').textContent=d.rentedVehicles??vehicles.filter(v=>v.status==='RENTED').length;const rev=(d.stats||[]).reduce((s,x)=>s+Number(x.totalRevenue||0),0);$('statRevenue').textContent='₹'+rev.toLocaleString('en-IN')}
function renderRevenue(stats){const box=$('revenueRows');box.innerHTML='';if(!stats.length){box.innerHTML='<p class="muted">Revenue will appear after completed bookings.</p>';return}const max=Math.max(...stats.map(x=>Number(x.totalRevenue||0)),1);stats.forEach(x=>{const row=document.createElement('div');row.className='revenue-row';row.innerHTML=`<span>${x.label}</span><div class="bar"><i style="width:${Math.round(Number(x.totalRevenue||0)/max*100)}%"></i></div><b>₹${Number(x.totalRevenue||0).toLocaleString('en-IN')}</b>`;box.appendChild(row)})}
$('yearSelect').innerHTML=[new Date().getFullYear(),new Date().getFullYear()-1,new Date().getFullYear()-2].map(y=>`<option>${y}</option>`).join('');
$('yearSelect').onchange=async()=>{try{const d=await json(`${API}/owner/dashboard/shops/${shop.id}/monthly?year=${$('yearSelect').value}`);renderStats(d);renderRevenue(d.stats||[])}catch(x){toast(x.message,false)}};
init();
