const API_BASE='http://localhost:8080/api';
const roleSwitch=document.getElementById('roleSwitch');
const regNameEl=document.getElementById('regName');
const regEmailEl=document.getElementById('regEmail');
const regPhoneEl=document.getElementById('regPhone');
const regPasswordEl=document.getElementById('regPassword');
const msgBox=document.getElementById('msgBox');
let role='USER';
roleSwitch.querySelectorAll('button').forEach(btn=>btn.addEventListener('click',()=>{role=btn.dataset.role;roleSwitch.querySelectorAll('button').forEach(b=>b.classList.toggle('active',b===btn));}));
function msg(text,type){msgBox.textContent=text;msgBox.className='form-msg show '+type}
function saveSession(data){localStorage.setItem('rentwheels_token',data.token);localStorage.setItem('rentwheels_user',JSON.stringify({id:data.id,name:data.name,email:data.email,role:data.role}));}
document.getElementById('registerForm').addEventListener('submit',async e=>{e.preventDefault();const btn=document.getElementById('registerBtn');btn.disabled=true;btn.querySelector('span').textContent='Creating…';msgBox.className='form-msg';try{const res=await fetch(`${API_BASE}/auth/register`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({name:regNameEl.value.trim(),email:regEmailEl.value.trim(),password:regPasswordEl.value,phone:regPhoneEl.value.trim(),role})});const body=await res.json();if(!res.ok||!body.success)throw new Error(body.message||'Registration failed');saveSession(body.data);msg('Account created successfully.','success');setTimeout(()=>{window.location.href=body.data.role==='OWNER'?'/Forend/Html/owner-dashboard.html':'/Forend/Html/home.html'},400)}catch(err){msg(err.message||'Unable to create account.','error')}finally{btn.disabled=false;btn.querySelector('span').textContent='Create account'}});
