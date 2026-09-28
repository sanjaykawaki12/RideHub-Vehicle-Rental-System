const API_BASE='http://localhost:8080/api';
const roleSwitch=document.getElementById('roleSwitch');
const loginEmailEl=document.getElementById('loginEmail');
const loginPasswordEl=document.getElementById('loginPassword');
const msgBox=document.getElementById('msgBox');
let role='USER';
roleSwitch.querySelectorAll('button').forEach(btn=>btn.addEventListener('click',()=>{role=btn.dataset.role;roleSwitch.querySelectorAll('button').forEach(b=>b.classList.toggle('active',b===btn));}));
function msg(text,type){msgBox.textContent=text;msgBox.className='form-msg show '+type}
function saveSession(data){localStorage.setItem('rentwheels_token',data.token);localStorage.setItem('rentwheels_user',JSON.stringify({id:data.id,name:data.name,email:data.email,role:data.role}));}
document.getElementById('loginForm').addEventListener('submit',async e=>{e.preventDefault();const btn=document.getElementById('loginBtn');btn.disabled=true;btn.querySelector('span').textContent='Signing in…';msgBox.className='form-msg';try{const res=await fetch(`${API_BASE}/auth/login`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email:loginEmailEl.value.trim(),password:loginPasswordEl.value,role})});const body=await res.json();if(!res.ok||!body.success)throw new Error(body.message||'Login failed');saveSession(body.data);msg('Login successful. Opening your dashboard…','success');setTimeout(()=>{window.location.href=body.data.role==='OWNER'?'/Forend/Html/owner-dashboard.html':'/Forend/Html/home.html'},350)}catch(err){msg(err.message||'Unable to sign in.','error')}finally{btn.disabled=false;btn.querySelector('span').textContent='Sign in'}});
