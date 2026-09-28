const API='http://localhost:8080/api';
const token=localStorage.getItem('rentwheels_token');
const user=JSON.parse(localStorage.getItem('rentwheels_user')||'null');
if(!token||!user||user.role!=='USER') location.href='login.html';
const headers={'Authorization':`Bearer ${token}`,'Content-Type':'application/json'};
document.getElementById('logout').onclick=()=>{localStorage.clear();location.href='login.html'};

let currentTicket=null;

async function loadTickets(){
  const r=await fetch(`${API}/user/support/tickets`,{headers});
  const b=await r.json();
  const box=document.getElementById('tickets');
  if(!r.ok){box.innerHTML='<p class="meta">Could not load support tickets.</p>';return;}
  box.innerHTML=(b.data||[]).map(t=>`
    <article class="ticket" data-ticket="${t.id}">
      <div class="ticket-top"><strong>#${t.id} · ${escapeHtml(t.subject)}</strong><span class="status ${String(t.status).toLowerCase()}">${t.status}</span></div>
      <div class="meta">${escapeHtml(t.category)} · ${escapeHtml(t.priority)} · ${new Date(t.createdAt).toLocaleString()}</div>
      <p>${escapeHtml(t.message)}</p>
      <div class="thread" id="thread-${t.id}"><span class="meta">Loading conversation…</span></div>
      <div class="reply-row"><input id="reply-${t.id}" placeholder="Reply to RideHub Support…"><button onclick="sendReply(${t.id})">Send</button></div>
    </article>`).join('')||'<p class="meta">No tickets yet.</p>';
  for(const t of (b.data||[])) loadThread(t.id);
}

async function loadThread(id){
  const r=await fetch(`${API}/user/support/tickets/${id}/messages`,{headers});
  const b=await r.json();
  const box=document.getElementById(`thread-${id}`); if(!box)return;
  box.innerHTML=(b.data||[]).map(m=>`<div class="message ${m.senderType==='CUSTOMER'?'mine':'support'}"><b>${escapeHtml(m.senderName)}</b><span>${escapeHtml(m.message)}</span><small>${new Date(m.createdAt).toLocaleString()}</small></div>`).join('')||'<span class="meta">No messages yet.</span>';
}

async function sendReply(id){
  const input=document.getElementById(`reply-${id}`); const message=input.value.trim();
  if(!message)return;
  const r=await fetch(`${API}/user/support/tickets/${id}/messages`,{method:'POST',headers,body:JSON.stringify({message})});
  const b=await r.json();
  if(!r.ok){alert(b.message||'Could not send message');return;}
  input.value=''; await loadThread(id); await loadTickets();
}

document.getElementById('ticketForm').onsubmit=async e=>{
  e.preventDefault();
  const msg=document.getElementById('msg');
  const r=await fetch(`${API}/user/support/tickets`,{method:'POST',headers,body:JSON.stringify({category:category.value,priority:priority.value,subject:subject.value,message:message.value})});
  const b=await r.json(); msg.textContent=b.message||'Ticket created';
  msg.className=r.ok?'success-msg':'error-msg';
  if(r.ok){e.target.reset();loadTickets();}
};
function escapeHtml(v){return String(v??'').replace(/[&<>'"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[c]));}
loadTickets();

setInterval(loadTickets,5000);
