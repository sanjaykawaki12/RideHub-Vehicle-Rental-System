const API='http://localhost:8080/api',t=localStorage.getItem('rentwheels_token'),u=JSON.parse(localStorage.getItem('rentwheels_user')||'null');
if(!t||!u||u.role!=='ADMIN')location.href='login.html';
const h={Authorization:`Bearer ${t}`};
async function load(){
  let r=await fetch(API+'/admin/summary',{headers:h}),b=await r.json();
  document.getElementById('stats').innerHTML=Object.entries(b.data||{}).map(([k,v])=>`<div class="card"><b>${v}</b><span>${k}</span></div>`).join('');
  r=await fetch(API+'/admin/support/tickets',{headers:h}); b=await r.json();
  document.getElementById('tickets').innerHTML=(b.data||[]).map(x=>`<article class="ticket">
    <div class="ticket-head"><div><b>#${x.id} · ${safe(x.subject)}</b><div class="customer">👤 ${safe(x.customerName)} · ${safe(x.customerEmail)}</div></div><span class="status">${safe(x.status)}</span></div>
    <div class="meta">${safe(x.category)} · ${safe(x.priority)} · ${new Date(x.createdAt).toLocaleString()}</div>
    <p>${safe(x.message)}</p>
    <div class="thread" id="thread-${x.id}">Loading messages…</div>
    <div class="reply"><input id="reply-${x.id}" placeholder="Reply to customer…"><button onclick="reply(${x.id})">Send Reply</button><button onclick="updateT(${x.id},'IN_PROGRESS')">In progress</button><button onclick="updateT(${x.id},'RESOLVED')">Resolve</button></div>
  </article>`).join('')||'<p>No customer messages yet.</p>';
  for(const x of (b.data||[])) loadThread(x.id);
  await loadLicenses(); await loadReports();
}
async function loadThread(id){const r=await fetch(API+'/admin/support/tickets/'+id+'/messages',{headers:h}),b=await r.json();const e=document.getElementById('thread-'+id);if(!e)return;e.innerHTML=(b.data||[]).map(m=>`<div class="msg ${m.senderType==='CUSTOMER'?'customer':'support'}"><b>${safe(m.senderName)}</b><span>${safe(m.message)}</span><small>${new Date(m.createdAt).toLocaleString()}</small></div>`).join('');}
async function reply(id){const e=document.getElementById('reply-'+id),message=e.value.trim();if(!message)return;const r=await fetch(API+'/admin/support/tickets/'+id+'/messages',{method:'POST',headers:{...h,'Content-Type':'application/json'},body:JSON.stringify({message})});if(!r.ok){const b=await r.json();alert(b.message||'Reply failed');return;}e.value='';await load();}
async function updateT(id,status){await fetch(API+'/admin/support/tickets/'+id,{method:'PATCH',headers:{...h,'Content-Type':'application/json'},body:JSON.stringify({status})});load();}
function safe(v){return String(v??'').replace(/[&<>'"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[c]));}
load();

async function loadLicenses(){const r=await fetch(API+'/admin/licenses',{headers:h}),b=await r.json();const e=document.getElementById('licenses');if(!e)return;e.innerHTML=(b.data||[]).map(x=>`<div class="ticket"><b>#${x.id} · ${safe(x.licenseNumber)}</b><div class="customer">User #${x.user?.id||''}</div><div class="meta">${x.verified?'VERIFIED':'PENDING'}</div><a href="http://localhost:8080${x.fileUrl}" target="_blank" style="color:#d6b36a">Open document</a> ${!x.verified?`<button onclick="verifyLicense(${x.id},true)">Verify</button>`:`<button onclick="verifyLicense(${x.id},false)">Reject</button>`}</div>`).join('')||'<p>No licenses submitted.</p>';}
async function verifyLicense(id,verified){const r=await fetch(API+'/admin/licenses/'+id+'/verify?verified='+verified,{method:'PATCH',headers:h});const b=await r.json();if(!r.ok)alert(b.message||'Update failed');await loadLicenses();}
async function loadReports(){const r=await fetch(API+'/admin/reports',{headers:h}),b=await r.json();const e=document.getElementById('reports');if(!e)return;e.innerHTML=Object.entries(b.data||{}).map(([k,v])=>`<div style="display:inline-block;margin:6px;padding:10px 14px;border:1px solid #27302f;border-radius:10px"><small>${safe(k)}</small><b style="display:block;color:#d6b36a">${safe(v)}</b></div>`).join('');}

setInterval(load,5000);
